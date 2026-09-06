export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const API_BASE = String(process.env.SPRING_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const API_KEY = process.env.CLINIC_API_KEY || '';

const FORWARDED_HEADER_DENYLIST = new Set([
  'host',
  'content-length',
  'connection',
  'transfer-encoding',
  'keep-alive',
  'upgrade',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailers',
  'origin',
  'referer'
]);

const CLINIC_LOCAL_DATE_TIME_FIELDS = new Set([
  'scheduledAt',
  'endAt'
]);

let clinicTimezoneCache = {
  value: '',
  expiresAt: 0
};

function apiKeyHeaders() {
  const headers = new Headers();
  if (API_KEY) headers.set('X-API-KEY', API_KEY);
  return headers;
}

async function getClinicTimezone() {
  const now = Date.now();

  if (clinicTimezoneCache.value && clinicTimezoneCache.expiresAt > now) {
    return clinicTimezoneCache.value;
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/public/clinic/timezone`,
      {
        method: 'GET',
        headers: apiKeyHeaders(),
        cache: 'no-store'
      }
    );

    if (!response.ok) {
      throw new Error(`Clinic timezone request failed with HTTP ${response.status}`);
    }

    const data = await response.json();
    const timezone = String(data?.timezone || '').trim();

    if (!timezone) {
      throw new Error('Clinic timezone is empty');
    }

    // Проверяем, что значение из БД является валидной IANA timezone.
    new Intl.DateTimeFormat('en-US', { timeZone: timezone }).format(new Date());

    clinicTimezoneCache = {
      value: timezone,
      expiresAt: now + 60_000
    };

    return timezone;
  } catch (error) {
    console.error('Clinic timezone lookup failed', error);

    // Не ломаем весь frontend, если timezone временно не удалось получить.
    // UTC здесь безопаснее, чем timezone устройства пользователя.
    return 'UTC';
  }
}

function isOffsetDateTime(value) {
  return /(?:Z|[+-]\d{2}:\d{2})$/i.test(value);
}

function parseBackendInstant(value) {
  if (typeof value !== 'string' || !value.trim()) {
    return null;
  }

  const normalized = value.trim();
  const date = new Date(
    isOffsetDateTime(normalized)
      ? normalized
      : `${normalized}Z`
  );

  return Number.isNaN(date.getTime()) ? null : date;
}

function formatInstantAsClinicLocal(value, timezone) {
  const instant = parseBackendInstant(value);

  if (!instant) {
    return value;
  }

  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23'
  }).formatToParts(instant);

  const values = Object.fromEntries(
    parts
      .filter(part => part.type !== 'literal')
      .map(part => [part.type, part.value])
  );

  return `${values.year}-${values.month}-${values.day}T${values.hour}:${values.minute}:${values.second}`;
}

function shouldConvertDateTimeField(key, value) {
  if (
    typeof value !== 'string'
    || CLINIC_LOCAL_DATE_TIME_FIELDS.has(key)
  ) {
    return false;
  }

  // Backend сейчас использует LocalDateTime и для системных timestamp-полей,
  // поэтому они приходят без Z/offset. Все *At, кроме clinic-local полей выше,
  // трактуем как UTC instant и переводим в timezone клиники.
  return key.endsWith('At')
    && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(value);
}

function convertResponseDateTimes(value, timezone) {
  if (Array.isArray(value)) {
    return value.map(item => convertResponseDateTimes(item, timezone));
  }

  if (!value || typeof value !== 'object') {
    return value;
  }

  return Object.fromEntries(
    Object.entries(value).map(([key, item]) => {
      if (shouldConvertDateTimeField(key, item)) {
        return [key, formatInstantAsClinicLocal(item, timezone)];
      }

      return [key, convertResponseDateTimes(item, timezone)];
    })
  );
}

function isJsonResponse(response) {
  return String(response.headers.get('content-type') || '')
    .toLowerCase()
    .includes('application/json');
}

async function buildResponse(upstream, path) {
  const responseHeaders = new Headers();

  upstream.headers.forEach((value, name) => {
    if (!FORWARDED_HEADER_DENYLIST.has(name.toLowerCase())) {
      responseHeaders.set(name, value);
    }
  });

  responseHeaders.set('Cache-Control', 'no-store');

  if (
    !isJsonResponse(upstream)
    || upstream.status === 204
    || upstream.status === 205
    || path.join('/') === 'public/clinic/timezone'
  ) {
    return new Response(upstream.body, {
      status: upstream.status,
      headers: responseHeaders
    });
  }

  const payload = await upstream.json();
  const timezone = await getClinicTimezone();
  const converted = convertResponseDateTimes(payload, timezone);

  return Response.json(
    converted,
    {
      status: upstream.status,
      headers: responseHeaders
    }
  );
}

async function proxy(request, context) {
  const { path = [] } = await context.params;
  const incoming = new URL(request.url);
  const target = new URL(`${API_BASE}/api/${path.join('/')}`);
  target.search = incoming.search;

  const headers = new Headers();
  request.headers.forEach((value, name) => {
    if (!FORWARDED_HEADER_DENYLIST.has(name.toLowerCase())) {
      headers.set(name, value);
    }
  });

  if (API_KEY) headers.set('X-API-KEY', API_KEY);
  headers.set('X-Forwarded-Proto', incoming.protocol.replace(':',''));
  headers.set('X-Forwarded-Host', incoming.host);

  const init = {
    method: request.method,
    headers,
    redirect: 'manual',
    cache: 'no-store'
  };

  if (!['GET','HEAD'].includes(request.method)) {
    init.body = await request.arrayBuffer();
  }

  try {
    const upstream = await fetch(target, init);
    return await buildResponse(upstream, path);
  } catch (error) {
    console.error('BublAPI proxy error', {
      method: request.method,
      target: target.toString(),
      error
    });

    return Response.json(
      { message: 'Spring Boot API is unavailable' },
      { status: 502 }
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const OPTIONS = proxy;
export const HEAD = proxy;

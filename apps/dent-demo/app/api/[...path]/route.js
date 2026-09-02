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
    const responseHeaders = new Headers();

    upstream.headers.forEach((value, name) => {
      if (!FORWARDED_HEADER_DENYLIST.has(name.toLowerCase())) {
        responseHeaders.set(name, value);
      }
    });

    responseHeaders.set('Cache-Control', 'no-store');

    return new Response(upstream.body, {
      status: upstream.status,
      headers: responseHeaders
    });
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

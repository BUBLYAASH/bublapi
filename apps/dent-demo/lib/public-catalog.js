const API_BASE = String(
  process.env.SPRING_API_URL || 'http://localhost:8080'
).replace(/\/$/, '');
const API_KEY = process.env.CLINIC_API_KEY || '';

const WORKING_DAY_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
];

const SHORT_DAY_NAMES = {
  MONDAY: 'пн',
  TUESDAY: 'вт',
  WEDNESDAY: 'ср',
  THURSDAY: 'чт',
  FRIDAY: 'пт',
  SATURDAY: 'сб',
  SUNDAY: 'вс'
};

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function formatTime24(value) {
  return value ? String(value).slice(0, 5) : '—';
}

function groupWorkingHours(hours = []) {
  return hours.reduce((groups, interval) => {
    const day = interval?.dayOfWeek;
    if (!day) return groups;
    (groups[day] ||= []).push(interval);
    return groups;
  }, {});
}

function renderCompactWorkingHours(hours = []) {
  if (!Array.isArray(hours) || !hours.length) {
    return '<span class="muted">Расписание пока не указано</span>';
  }

  const grouped = groupWorkingHours(hours);
  return WORKING_DAY_ORDER
    .filter(day => grouped[day]?.length)
    .map(day => {
      const intervals = grouped[day]
        .slice()
        .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
        .map(interval => `${escapeHtml(formatTime24(interval.startTime))}–${escapeHtml(formatTime24(interval.endTime))}`)
        .join(', ');

      return `<span class="doctor-card-schedule-day"><strong>${SHORT_DAY_NAMES[day]}</strong> ${intervals}</span>`;
    })
    .join('');
}

function doctorAvatarHtml(doctor = {}) {
  const avatar = doctor.avatarUrl ?? doctor.avatar_url;
  const source = escapeHtml(
    avatar && String(avatar).trim() ? String(avatar).trim() : '/default.png'
  );

  return `<img class="doctor-avatar" src="${source}" alt="Аватар врача" onerror="this.onerror=null;this.src='/default.png'">`;
}

async function fetchPublicJson(path) {
  const headers = new Headers({ Accept: 'application/json' });
  if (API_KEY) headers.set('X-API-KEY', API_KEY);

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'GET',
    headers,
    cache: 'no-store'
  });

  if (!response.ok) {
    throw new Error(`Public API ${path} returned HTTP ${response.status}`);
  }

  return response.json();
}

async function loadDoctors() {
  const doctors = await fetchPublicJson('/api/public/doctors');
  if (!Array.isArray(doctors)) {
    throw new Error('Public doctors response is not an array');
  }

  return Promise.all(doctors.map(async doctor => {
    try {
      const hours = await fetchPublicJson(
        `/api/public/doctors/${encodeURIComponent(doctor.id)}/working-hours`
      );
      return { ...doctor, workingHours: Array.isArray(hours) ? hours : [] };
    } catch {
      return { ...doctor, workingHours: [] };
    }
  }));
}

async function loadServices() {
  const services = await fetchPublicJson('/api/public/services');
  if (!Array.isArray(services)) {
    throw new Error('Public services response is not an array');
  }
  return services;
}

export async function loadInitialPublicCatalog(view) {
  try {
    if (view === 'public-doctors') {
      return { doctors: { loaded: true, items: await loadDoctors() } };
    }

    if (view === 'public-services') {
      return { services: { loaded: true, items: await loadServices() } };
    }
  } catch (error) {
    console.error('Public catalog SSR failed', { view, error });
    return {
      [view === 'public-doctors' ? 'doctors' : 'services']: {
        loaded: false,
        items: []
      }
    };
  }

  return {};
}

export function renderDoctorsHtml(doctors = []) {
  if (!doctors.length) {
    return '<div class="card empty">Данных пока нет</div>';
  }

  return doctors.map((doctor, index) => {
    const ref = `publicDoctor_ssr_${index}`;
    const fullName = [doctor.lastName, doctor.firstName, doctor.middleName]
      .filter(Boolean)
      .join(' ');

    return `
      <article class="card clickable-card public-doctor-card" data-ref="${ref}">
        <div class="doctor-card-layout">
          ${doctorAvatarHtml(doctor)}
          <div class="doctor-card-content">
            <span class="badge ${doctor.active ? 'success' : 'danger'}">
              ${doctor.active ? 'Активен' : 'Недоступен'}
            </span>
            <h3>
              <button class="link-button public-doctor-link" data-ref="${ref}">
                ${escapeHtml(fullName)}
              </button>
            </h3>
            <p><strong>${escapeHtml(doctor.specialty || 'Специальность не указана')}</strong></p>
            <p class="muted">${escapeHtml(doctor.description || 'Описание не указано')}</p>
            <div class="doctor-card-schedule">
              <span class="doctor-card-schedule-icon" aria-hidden="true">◷</span>
              <div class="doctor-card-schedule-content"><strong>Рабочие часы:</strong>${renderCompactWorkingHours(doctor.workingHours)}</div>
            </div>
          </div>
          <span class="doctor-card-arrow" aria-hidden="true">›</span>
        </div>
      </article>
    `;
  }).join('');
}

function renderServiceCardsHtml(services, status) {
  return services.map((service, index) => {
    const ref = `publicService_ssr_${status}_${index}`;
    return `
      <article class="card clickable-card public-service-card" data-ref="${ref}">
        <span class="badge ${service.active === false ? 'danger' : 'success'}">
          ${service.active === false ? 'Отключена' : 'Активна'}
        </span>
        <span class="badge">${escapeHtml(service.durationMinutes)} мин</span>
        <h3>
          <button class="link-button public-service-link" data-ref="${ref}">
            ${escapeHtml(service.title || 'Стоматологическая услуга')}
          </button>
        </h3>
        <div class="metric">${escapeHtml(service.price)} ₽</div>
        <p class="muted">${escapeHtml(service.description || 'Подробности можно уточнить при записи.')}</p>
      </article>
    `;
  }).join('');
}

export function renderServicesHtml(services = []) {
  const groups = [
    {
      status: 'active',
      title: 'Активированные услуги',
      items: services.filter(service => service.active !== false),
      open: true
    },
    {
      status: 'inactive',
      title: 'Отключённые услуги',
      items: services.filter(service => service.active === false),
      open: false
    }
  ].filter(group => group.items.length);

  if (!groups.length) {
    return '<div class="card empty">Услуг пока нет</div>';
  }

  return groups.map(group => `
    <details class="appointment-group service-status-group" data-status="${group.status}"${group.open ? ' open' : ''}>
      <summary>
        <span>${group.title}</span>
        <span class="badge ${group.status === 'active' ? 'success' : 'danger'}">${group.items.length}</span>
      </summary>
      <div class="appointment-group-content grid grid-3">${renderServiceCardsHtml(group.items, group.status)}</div>
    </details>
  `).join('');
}

export function serializeCatalogSeed(catalog) {
  return JSON.stringify(catalog)
    .replaceAll('<', '\\u003c')
    .replaceAll('>', '\\u003e')
    .replaceAll('&', '\\u0026')
    .replaceAll('\u2028', '\\u2028')
    .replaceAll('\u2029', '\\u2029');
}

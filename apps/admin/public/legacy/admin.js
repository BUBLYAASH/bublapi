import {api, detailItem, escapeHtml, formatDate, formToObject, setupRequiredFieldMarkers, toast} from './api.js';

import './phone.js';

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];

// Админка обновляется только вручную. Периодическое обновление сбрасывало
// выбранные роли и мешало редактированию форм.
const AUTO_REFRESH_INTERVAL_MS = 0;
const AUTO_REFRESH_VIEWS = new Set();

let autoRefreshTimer = null;
let refreshInProgress = false;

const caches = {
    clinics: new Map(),
    apiKeys: new Map(),
    dentalServices: new Map(),
    notifications: new Map(),
    users: new Map()
};

const ROLE_IDS = {
    OWNER: '11111111-1111-1111-1111-111111111111',
    ADMIN: '22222222-2222-2222-2222-222222222222',
    DOCTOR: '33333333-3333-3333-3333-333333333333',
    RECEPTIONIST: '44444444-4444-4444-4444-444444444444',
    PATIENT: '55555555-5555-5555-5555-555555555555'
};

const ROLE_NAMES = Object.keys(ROLE_IDS);

let loadedAdminUsers = [];
const selectedAdminRoles = new Map();
let modalReturnFocus = null;
let activeModalResolver = null;

$('#view-admin-api-keys').innerHTML = `
  <div class="section-head">
    <div>
      <h1>API-ключи клиник</h1>
      <p class="muted">Создание и управление ключами доступа клиник.</p>
    </div>
    <button class="btn btn-primary" id="loadApiKeys" type="button">Обновить</button>
  </div>
  <form class="card" id="createApiKeyForm" style="margin-bottom:18px">
    <h2>Создать ключ</h2>
    <div class="form-grid">
      <label>ID клиники<input name="clinicId" autocomplete="off" required></label>
      <label>Название ключа<input name="name" required value="Demo key"></label>
    </div>
    <button class="btn btn-success" style="margin-top:16px">Создать</button>
  </form>
  <section aria-live="polite" class="card api-key-secret hidden" id="apiKeySecret">
    <div>
      <h2>Новый API-ключ</h2>
      <p class="muted">Скопируйте его сейчас: повторно секрет показан не будет.</p>
    </div>
    <div class="api-key-secret-row">
      <code id="apiKeyValue"></code>
      <button class="btn btn-secondary" id="copyApiKey" type="button">Скопировать</button>
    </div>
  </section>
  <div id="apiKeysTable" style="margin-top:18px"></div>
`;


let adminAuthenticated = document.querySelector('[data-admin-authenticated]')?.dataset.adminAuthenticated === 'true';

function isAdminAuthenticated() {
    return adminAuthenticated;
}

function applyAdminVisibility() {
    const authenticated = isAdminAuthenticated();

    $$('[data-admin-only]').forEach(element => {
        element.classList.toggle('hidden', !authenticated);
    });

    $('#adminLogout').classList.toggle('hidden', !authenticated);

    const activeView = $('.view.active');

    if (
        activeView?.dataset.adminOnly
        && !authenticated
    ) {
        showView('admin-auth');
    }
}

const ADMIN_VIEW_ROUTES = {
    'admin-auth': '/login', 'admin-clinics': '/dent/clinics', 'admin-catalog': '/dent/catalog',
    'admin-api-keys': '/dent/api-keys', 'admin-notifications': '/dent/notifications', 'admin-users': '/dent/users',
    'admin-docs': '/dent/docs', 'admin-system': '/dent/system'
};
const ADMIN_VIEW_TITLES = {
    'admin-clinics': 'Клиники', 'admin-catalog': 'Каталог услуг',
    'admin-api-keys': 'API-ключи', 'admin-notifications': 'Системные уведомления',
    'admin-users': 'Пользователи и роли', 'admin-docs': 'API-документация',
    'admin-system': 'Состояние системы'
};
const ADMIN_ROUTE_VIEWS = Object.fromEntries(Object.entries(ADMIN_VIEW_ROUTES).map(([v, p]) => [p, v]));

function adminViewFromPath() {
    const path = location.pathname.replace(/\/+$/, '') || '/login';
    return ADMIN_ROUTE_VIEWS[path] || document.querySelector('[data-initial-view]')?.dataset.initialView || 'admin-auth';
}

function showView(name, {updateUrl = true} = {}) {
    const target = $(`#view-${name}`);

    if (!target) {
        return;
    }

    if (target.dataset.adminOnly && !isAdminAuthenticated()) {
        toast('Сначала войдите как администратор', 'error');
        name = 'admin-auth';
    }

    $$('.view').forEach(view => {
        view.classList.toggle(
            'active',
            view.id === `view-${name}`
        );
    });

    $$('.nav-button').forEach(button => {
        const isActive = button.dataset.view === name;
        button.classList.toggle('active', isActive);
        if (isActive) button.setAttribute('aria-current', 'page');
        else button.removeAttribute('aria-current');
    });

    if (name === 'admin-docs') {
        const docsFrame = $('#view-admin-docs .docs-frame[data-src]');
        if (docsFrame && !docsFrame.src) docsFrame.src = docsFrame.dataset.src;
    }

    if (updateUrl) {
        const path = ADMIN_VIEW_ROUTES[name] || '/login';
        if (location.pathname !== path) history.pushState({view: name}, '', path);
    }
    if (ADMIN_VIEW_TITLES[name]) document.title = `${ADMIN_VIEW_TITLES[name]} | BublAPI Admin`;
    refreshCurrentView();
}

$$('.nav-button').forEach(button => {
    button.addEventListener('click', () => {
        showView(button.dataset.view);
    });
});

function openModal(title, bodyHtml, actionsHtml = '') {
    modalReturnFocus = document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    $('#detailModalTitle').textContent = title;
    $('#detailModalBody').innerHTML = bodyHtml;
    $('#detailModalActions').innerHTML = actionsHtml;
    $('#detailModal').classList.remove('hidden');
    $('#detailModal').setAttribute('aria-hidden', 'false');
    $$('.topbar, .layout').forEach(element => {
        element.inert = true;
    });
    document.body.classList.add('modal-open');
    window.requestAnimationFrame(() => {
        $('#detailModal').querySelector('button, input, select, textarea, [tabindex]:not([tabindex="-1"])')?.focus();
    });
}

function closeModal(result = false) {
    $('#detailModal').classList.add('hidden');
    $('#detailModal').setAttribute('aria-hidden', 'true');
    $$('.topbar, .layout').forEach(element => {
        element.inert = false;
    });
    document.body.classList.remove('modal-open');
    $('#detailModalBody').innerHTML = '';
    $('#detailModalActions').innerHTML = '';
    modalReturnFocus?.focus();
    modalReturnFocus = null;
    const resolver = activeModalResolver;
    activeModalResolver = null;
    resolver?.(result);
}

function confirmAction(title, message, confirmLabel) {
    return new Promise(resolve => {
        activeModalResolver = resolve;
        openModal(
            title,
            `<p>${escapeHtml(message)}</p>`,
            `
          <button class="btn btn-secondary" data-modal-close type="button">Отмена</button>
          <button class="btn btn-danger" id="confirmModalAction" type="button">${escapeHtml(confirmLabel)}</button>
        `
        );
        $('#confirmModalAction').addEventListener('click', () => closeModal(true), {once: true});
    });
}

$('#detailModal').addEventListener('click', event => {
    if (event.target.closest('[data-modal-close]')) {
        closeModal();
    }
});

document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        closeModal();
    }

    if (event.key === 'Tab' && !$('#detailModal').classList.contains('hidden')) {
        const focusable = [...$('#detailModal').querySelectorAll(
            'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )];
        if (!focusable.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }
});

function setButtonBusy(button, busy, pendingLabel = 'Загрузка…') {
    if (!button) return;
    if (busy) {
        button.dataset.idleLabel = button.textContent.trim();
        button.textContent = pendingLabel;
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
    } else {
        button.textContent = button.dataset.idleLabel || button.textContent;
        button.disabled = false;
        button.removeAttribute('aria-busy');
        delete button.dataset.idleLabel;
    }
}

function revealApiKey(rawKey) {
    $('#apiKeyValue').textContent = rawKey;
    $('#apiKeySecret').classList.remove('hidden');
    $('#copyApiKey').focus();
}

function updateSession() {
    applyAdminVisibility();

    if (!adminAuthenticated) {
        $('#adminSession').innerHTML = `
      <h3>Нет активной admin-сессии</h3>
      <p class="muted">Войдите под ролью ADMIN.</p>
    `;
        return;
    }

    $('#adminSession').innerHTML = `
    <h3>Защищённая сессия активна</h3>
    <p class="muted">Токен хранится в HttpOnly cookie и недоступен JavaScript.</p>
  `;
}

$('#adminLogout').addEventListener('click', async () => {
    await fetch('/api/auth/logout', {method: 'POST', headers: {'Content-Type': 'application/json'}});
    adminAuthenticated = false;
    stopAutoRefresh();
    location.assign('/login');
});

$('#adminLoginForm').addEventListener('submit', async event => {
    event.preventDefault();

    try {
        const result = await api(
            '/api/auth/login',
            {
                method: 'POST',
                skipAuth: true,
                body: formToObject(event.currentTarget)
            },
            'admin'
        );

        if (!result.authenticated) throw new Error('Не удалось создать сессию');
        adminAuthenticated = true;
        if (window.bublapiNavigate) window.bublapiNavigate('/apis', {replace: true});
        else location.assign('/apis');
    } catch (error) {
        toast(error.message, 'error');
    }
});

async function loadSystemStatus() {
    const render = async (path, target) => {
        try {
            const result = await api(path, {method: 'GET'}, 'admin');
            $(target).textContent = JSON.stringify(result, null, 2);
        } catch (error) {
            $(target).textContent = error.message;
        }
    };
    await Promise.all([
        render('/api/system/health', '#systemHealth'),
        render('/api/system/info', '#systemInfo')
    ]);
}

$('#loadSystemStatus')?.addEventListener('click', loadSystemStatus);
window.addEventListener('admin:unauthorized', () => {
    adminAuthenticated = false;
    location.assign('/login');
});

function renderTable(container, rows, columns, actions) {
    if (!rows?.length) {
        container.innerHTML = '<div class="card empty">Данных пока нет</div>';
        return;
    }

    const headers = columns
        .map(column => `<th scope="col">${column.label}</th>`)
        .join('');

    const body = rows
        .map(row => {
            const cells = columns
                .map(column => {
                    const value = column.render
                        ? column.render(row)
                        : escapeHtml(row[column.key] ?? '—');

                    return `<td>${value}</td>`;
                })
                .join('');

            return `
      <tr>
        ${cells}
        ${actions ? `<td>${actions(row)}</td>` : ''}
      </tr>
    `;
        })
        .join('');

    container.innerHTML = `
    <div aria-label="Таблица данных" class="table-wrap" role="region" tabindex="0">
      <table>
        <thead>
          <tr>
            ${headers}
            ${actions ? '<th scope="col">Действия</th>' : ''}
          </tr>
        </thead>
        <tbody>${body}</tbody>
      </table>
    </div>
  `;
}

function clinicDetailsHtml(clinic) {
    return `
    <div class="detail-grid">
      ${detailItem('Clinic ID', clinic.id, true)}
      ${detailItem('Название', clinic.title)}
      ${detailItem('Статус', clinic.active ? 'Активна' : 'Отключена')}
      ${detailItem('Адрес', clinic.address, true)}
      ${detailItem('Телефон', clinic.phone)}
      ${detailItem('Email', clinic.email)}
      ${detailItem('Website', clinic.website, true)}
      ${detailItem('Timezone', clinic.timezone)}
      ${detailItem('Описание', clinic.description, true)}
    </div>
  `;
}

function showClinicDetails(clinic) {
    openModal(
        clinic.title,
        clinicDetailsHtml(clinic),
        `
        <button class="btn btn-primary clinic-edit-modal" data-id="${clinic.id}">
          Редактировать
        </button>
      `
    );
}

function openClinicEdit(clinic) {
    openModal(
        'Редактировать клинику',
        `
        <form id="modalClinicEditForm" class="form-grid">
          <label>Название<input name="title" required value="${escapeHtml(clinic.title || '')}"></label>
          <label>Телефон<input name="phone" value="${escapeHtml(clinic.phone || '')}"></label>
          <label class="full">Адрес<input name="address" required value="${escapeHtml(clinic.address || '')}"></label>
          <label>Email<input name="email" type="email" value="${escapeHtml(clinic.email || '')}"></label>
          <label>Website<input name="website" value="${escapeHtml(clinic.website || '')}"></label>
          <label>Timezone<input name="timezone" required value="${escapeHtml(clinic.timezone || '')}"></label>
          <label class="full">Описание<textarea name="description">${escapeHtml(clinic.description || '')}</textarea></label>
        </form>
      `,
        `
        <button class="btn btn-primary clinic-save-modal" data-id="${clinic.id}">
          Сохранить
        </button>
      `
    );
}

async function loadClinics({silent = false} = {}) {
    try {
        const clinics = await api(
            '/api/admin/clinics',
            {
                method: 'GET'
            },
            'admin'
        );

        caches.clinics = new Map(
            clinics.map(clinic => [clinic.id, clinic])
        );

        renderTable(
            $('#clinicsTable'),
            clinics,
            [
                {
                    label: 'Название',
                    render: clinic => `
              <button class="link-button clinic-details" data-id="${clinic.id}">
                ${escapeHtml(clinic.title)}
              </button>
            `
                },
                {label: 'Адрес', key: 'address'},
                {label: 'Email', key: 'email'},
                {
                    label: 'Статус',
                    render: clinic => `
              <span class="badge ${clinic.active ? 'success' : 'danger'}">
                ${clinic.active ? 'Активна' : 'Отключена'}
              </span>
            `
                },
                {
                    label: 'Clinic ID',
                    render: clinic => `<span class="code">${clinic.id}</span>`
                }
            ],
            clinic => `
          <div class="actions">
            <button class="btn btn-primary btn-sm clinic-edit" data-id="${clinic.id}">
              Редактировать
            </button>
            <button class="btn btn-secondary btn-sm clinic-toggle" data-id="${clinic.id}" data-active="${clinic.active}">
              ${clinic.active ? 'Отключить' : 'Активировать'}
            </button>
          </div>
        `
        );
    } catch (error) {
        if (!silent) {
            toast(error.message, 'error');
        }
    }
}

$('#loadClinics').addEventListener('click', () => {
    loadClinics();
});

$('#createClinicForm').addEventListener('submit', async event => {
    event.preventDefault();

    try {
        await api(
            '/api/admin/clinics',
            {
                method: 'POST',
                body: formToObject(event.currentTarget)
            },
            'admin'
        );

        event.currentTarget.reset();
        toast('Клиника создана', 'success');
        await loadClinics();
    } catch (error) {
        toast(error.message, 'error');
    }
});

$('#clinicsTable').addEventListener('click', async event => {
    const details = event.target.closest('.clinic-details');
    const edit = event.target.closest('.clinic-edit');
    const toggle = event.target.closest('.clinic-toggle');

    try {
        if (details) {
            const clinic = caches.clinics.get(details.dataset.id);
            if (clinic) {
                showClinicDetails(clinic);
            }
        }

        if (edit) {
            const clinic = caches.clinics.get(edit.dataset.id);
            if (clinic) {
                openClinicEdit(clinic);
            }
        }

        if (toggle) {
            const action = toggle.dataset.active === 'true'
                ? 'deactivation'
                : 'activation';

            await api(
                `/api/admin/clinics/${toggle.dataset.id}/${action}`,
                {
                    method: 'PATCH'
                },
                'admin'
            );

            toast(
                action === 'deactivation'
                    ? 'Клиника отключена'
                    : 'Клиника активирована',
                'success'
            );

            await loadClinics();
        }
    } catch (error) {
        toast(error.message, 'error');
    }
});

function dentalServiceActive(service) {
    if (typeof service.active === 'boolean') {
        return service.active;
    }

    if (typeof service.enabled === 'boolean') {
        return service.enabled;
    }

    return null;
}

function renderDentalServiceStatus(service) {
    const active = dentalServiceActive(service);

    if (active === null) {
        return '<span class="badge warning">Не передан API</span>';
    }

    return `
    <span class="badge ${active ? 'success' : 'danger'}">
      ${active ? 'Активна' : 'Отключена'}
    </span>
  `;
}

async function getDentalServiceById(serviceId) {
    const service = await api(
        `/api/admin/catalog/dental-services/${serviceId}`,
        {
            method: 'GET'
        },
        'admin'
    );

    caches.dentalServices.set(service.id, service);

    openModal(
        service.title,
        `
        <div class="detail-grid">
          ${detailItem('ID', service.id, true)}
          ${detailItem('Категория', service.category)}
          ${detailItem('Длительность', `${service.defaultDurationMinutes} мин`)}
          ${detailItem(
            'Статус',
            dentalServiceActive(service) === null
                ? 'Не передан API'
                : dentalServiceActive(service)
                    ? 'Активна'
                    : 'Отключена'
        )}
          ${detailItem('Описание', service.description, true)}
        </div>
      `,
        `
        <button class="btn btn-primary catalog-edit-modal" data-id="${service.id}">
          Редактировать
        </button>
      `
    );
}

function openDentalServiceEdit(service) {
    openModal(
        'Редактировать услугу каталога',
        `
        <form id="modalCatalogEditForm" class="form-grid">
          <label>Название<input name="title" required value="${escapeHtml(service.title || '')}"></label>
          <label>Категория<input name="category" required value="${escapeHtml(service.category || '')}"></label>
          <label>Длительность<input name="defaultDurationMinutes" required type="number" min="5" value="${service.defaultDurationMinutes ?? ''}"></label>
          <label class="full">Описание<textarea name="description">${escapeHtml(service.description || '')}</textarea></label>
        </form>
      `,
        `
        <button class="btn btn-primary catalog-save-modal" data-id="${service.id}">
          Сохранить
        </button>
      `
    );
}

async function loadCatalog({silent = false} = {}) {
    try {
        const services = await api(
            '/api/admin/catalog/dental-services',
            {
                method: 'GET'
            },
            'admin'
        );

        caches.dentalServices = new Map(
            services.map(service => [service.id, service])
        );

        renderTable(
            $('#catalogTable'),
            services,
            [
                {
                    label: 'Название',
                    render: service => `
              <button class="link-button catalog-details" data-id="${service.id}">
                ${escapeHtml(service.title)}
              </button>
            `
                },
                {label: 'Категория', key: 'category'},
                {
                    label: 'Длительность',
                    render: service => `${service.defaultDurationMinutes} мин`
                },
                {label: 'Описание', key: 'description'},
                {
                    label: 'Статус',
                    render: renderDentalServiceStatus
                },
                {
                    label: 'ID',
                    render: service => `<span class="code">${service.id}</span>`
                }
            ],
            service => {
                const active = dentalServiceActive(service);

                return `
            <div class="actions">
              <button class="btn btn-primary btn-sm catalog-edit" data-id="${service.id}">
                Редактировать
              </button>
              ${active !== false ? `
                <button class="btn btn-danger btn-sm catalog-off" data-id="${service.id}">
                  Отключить
                </button>
              ` : ''}
              ${active !== true ? `
                <button class="btn btn-success btn-sm catalog-on" data-id="${service.id}">
                  Активировать
                </button>
              ` : ''}
            </div>
          `;
            }
        );
    } catch (error) {
        if (!silent) {
            toast(error.message, 'error');
        }
    }
}

$('#loadCatalog').addEventListener('click', () => {
    loadCatalog();
});

$('#createCatalogServiceForm').addEventListener('submit', async event => {
    event.preventDefault();

    const body = formToObject(event.currentTarget);
    body.defaultDurationMinutes = Number(body.defaultDurationMinutes);

    try {
        await api(
            '/api/admin/catalog/dental-services',
            {
                method: 'POST',
                body
            },
            'admin'
        );

        event.currentTarget.reset();
        toast('Услуга создана', 'success');
        await loadCatalog();
    } catch (error) {
        toast(error.message, 'error');
    }
});

$('#catalogTable').addEventListener('click', async event => {
    const details = event.target.closest('.catalog-details');
    const edit = event.target.closest('.catalog-edit');
    const deactivate = event.target.closest('.catalog-off');
    const activate = event.target.closest('.catalog-on');

    try {
        if (details) {
            await getDentalServiceById(details.dataset.id);
        }

        if (edit) {
            const service = caches.dentalServices.get(edit.dataset.id);
            if (service) {
                openDentalServiceEdit(service);
            }
        }

        if (deactivate) {
            await api(
                `/api/admin/catalog/dental-services/${deactivate.dataset.id}/deactivation`,
                {method: 'PATCH'},
                'admin'
            );

            toast('Услуга отключена', 'error');
            await loadCatalog();
        }

        if (activate) {
            await api(
                `/api/admin/catalog/dental-services/${activate.dataset.id}/activation`,
                {method: 'PATCH'},
                'admin'
            );

            toast('Услуга активирована', 'success');
            await loadCatalog();
        }
    } catch (error) {
        toast(error.message, 'error');
    }
});

$('#createApiKeyForm').addEventListener('submit', async event => {
    event.preventDefault();
    const values = formToObject(event.currentTarget);
    const submitButton = event.submitter;
    setButtonBusy(submitButton, true, 'Создаём…');

    try {
        const result = await api(
            `/api/admin/api-keys/${values.clinicId}`,
            {
                method: 'POST',
                body: {
                    name: values.name
                }
            },
            'admin'
        );

        revealApiKey(result.rawKey);
        event.currentTarget.reset();
        toast('API-ключ создан', 'success');
        await loadApiKeys();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonBusy(submitButton, false);
    }
});

$('#copyApiKey').addEventListener('click', async event => {
    const rawKey = $('#apiKeyValue').textContent;
    if (!rawKey) return;
    setButtonBusy(event.currentTarget, true, 'Копируем…');
    try {
        await navigator.clipboard.writeText(rawKey);
        toast('API-ключ скопирован', 'success');
    } catch {
        toast('Не удалось скопировать. Выделите ключ вручную', 'error');
    } finally {
        setButtonBusy(event.currentTarget, false);
    }
});

async function loadApiKeys({silent = false} = {}) {
    const container = $('#apiKeysTable');
    container.setAttribute('aria-busy', 'true');
    if (!container.hasChildNodes()) {
        container.innerHTML = '<div class="card empty">Загружаем API-ключи…</div>';
    }

    try {
        const apiKeys = await api(
            '/api/admin/api-keys',
            {method: 'GET'},
            'admin'
        );

        caches.apiKeys = new Map(apiKeys.map(apiKey => [apiKey.id, apiKey]));

        renderTable(
            container,
            apiKeys,
            [
                {label: 'Название', key: 'name'},
                {
                    label: 'Клиника',
                    render: apiKey => `<span class="code">${escapeHtml(apiKey.clinicId)}</span>`
                },
                {
                    label: 'Префикс',
                    render: apiKey => `<span class="code">${escapeHtml(apiKey.prefix)}</span>`
                },
                {label: 'Истекает', render: apiKey => formatDate(apiKey.expiresAt)},
                {label: 'Льготный период', render: apiKey => formatDate(apiKey.graceUntil)},
                {
                    label: 'Статус',
                    render: apiKey => `
              <span class="badge ${apiKey.active ? 'success' : 'danger'}">
                ${apiKey.active ? 'Активен' : 'Отозван'}
              </span>
            `
                }
            ],
            apiKey => apiKey.active
                ? `
              <div class="actions">
                <button class="btn btn-secondary btn-sm api-key-renew" data-clinic-id="${apiKey.clinicId}">
                  Продлить
                </button>
                <button class="btn btn-primary btn-sm api-key-rotate" data-clinic-id="${apiKey.clinicId}">
                  Сменить ключ
                </button>
                <button class="btn btn-danger btn-sm api-key-revoke" data-id="${apiKey.id}">
                  Отозвать
                </button>
              </div>
            `
                : '<span class="muted">Нет доступных действий</span>'
        );
    } catch (error) {
        if (!silent) {
            toast(error.message, 'error');
        }
    } finally {
        container.setAttribute('aria-busy', 'false');
    }
}

$('#loadApiKeys').addEventListener('click', () => loadApiKeys());

$('#apiKeysTable').addEventListener('click', async event => {
    const renew = event.target.closest('.api-key-renew');
    const rotate = event.target.closest('.api-key-rotate');
    const revoke = event.target.closest('.api-key-revoke');

    if (!renew && !rotate && !revoke) {
        return;
    }

    const actionButton = renew || rotate || revoke;

    try {
        if (renew) {
            setButtonBusy(actionButton, true, 'Продлеваем…');
            await api(
                `/api/admin/api-keys/${renew.dataset.clinicId}/renew`,
                {method: 'PATCH'},
                'admin'
            );
            toast('API-ключ продлён', 'success');
        }

        if (rotate) {
            const confirmed = await confirmAction(
                'Сменить API-ключ?',
                'Текущий ключ сразу перестанет работать. После смены сохраните новый ключ.',
                'Сменить ключ'
            );
            if (!confirmed) {
                return;
            }

            setButtonBusy(actionButton, true, 'Меняем…');
            const result = await api(
                `/api/admin/api-keys/${rotate.dataset.clinicId}/rotate`,
                {method: 'POST'},
                'admin'
            );
            revealApiKey(result.rawKey);
            toast('API-ключ сменён. Сохраните новый ключ', 'success');
        }

        if (revoke) {
            const confirmed = await confirmAction(
                'Отозвать API-ключ?',
                'Клиника потеряет доступ к API. Это действие нельзя отменить.',
                'Отозвать ключ'
            );
            if (!confirmed) {
                return;
            }

            setButtonBusy(actionButton, true, 'Отзываем…');
            await api(
                `/api/admin/api-keys/${revoke.dataset.id}`,
                {method: 'DELETE'},
                'admin'
            );
            toast('API-ключ отозван', 'success');
        }

        await loadApiKeys();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonBusy(actionButton, false);
    }
});

async function getNotificationById(notificationId) {
    const notification = await api(
        `/api/admin/notifications/${notificationId}`,
        {
            method: 'GET'
        },
        'admin'
    );

    openModal(
        notification.title,
        `
        <div class="detail-grid">
          ${detailItem('ID', notification.id, true)}
          ${detailItem('Request ID', notification.requestId, true)}
          ${detailItem('Тип', notification.type)}
          ${detailItem('Канал', notification.channel)}
          ${detailItem('Статус', notification.status)}
          ${detailItem('Создано', formatDate(notification.createdAt))}
          ${detailItem('Отправлено', formatDate(notification.sentAt))}
          ${detailItem('Clinic ID', notification.clinicId, true)}
          ${detailItem('User ID', notification.userId, true)}
          ${detailItem('Appointment ID', notification.appointmentId, true)}
          ${detailItem('Сообщение', notification.message, true)}
          ${detailItem('Ошибка', notification.errorMessage, true)}
        </div>
      `
    );
}

async function loadNotifications({silent = false} = {}) {
    try {
        const notifications = await api(
            '/api/admin/notifications',
            {
                method: 'GET'
            },
            'admin'
        );

        caches.notifications = new Map(
            notifications.map(item => [item.id, item])
        );

        renderTable(
            $('#adminNotifications'),
            notifications,
            [
                {
                    label: 'Создано',
                    render: notification => formatDate(notification.createdAt)
                },
                {label: 'Тип', key: 'type'},
                {label: 'Канал', key: 'channel'},
                {
                    label: 'Статус',
                    render: notification => {
                        let badgeClass = 'warning';

                        if (notification.status === 'SENT') {
                            badgeClass = 'success';
                        }

                        if (notification.status === 'FAILED') {
                            badgeClass = 'danger';
                        }

                        return `
                <span class="badge ${badgeClass}">
                  ${notification.status}
                </span>
              `;
                    }
                },
                {
                    label: 'Заголовок',
                    render: notification => `
              <button class="link-button notification-details" data-id="${notification.id}">
                ${escapeHtml(notification.title)}
              </button>
            `
                },
                {
                    label: 'Clinic/User',
                    render: notification => `
              <span class="code">
                ${notification.clinicId}<br>
                ${notification.userId || '—'}
              </span>
            `
                },
                {
                    label: 'ID',
                    render: notification => `<span class="code">${notification.id}</span>`
                }
            ]
        );
    } catch (error) {
        if (!silent) {
            toast(error.message, 'error');
        }
    }
}

$('#loadAdminNotifications').addEventListener('click', () => {
    loadNotifications();
});

$('#adminNotifications').addEventListener('click', async event => {
    const details = event.target.closest('.notification-details');

    if (!details) {
        return;
    }

    try {
        await getNotificationById(details.dataset.id);
    } catch (error) {
        toast(error.message, 'error');
    }
});


async function getAdminUserById(userId) {
    const user = await api(
        `/api/admin/users/${userId}`,
        {method: 'GET'},
        'admin'
    );

    openModal(
        `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim() || 'Пользователь',
        `
        <div class="detail-grid">
          ${detailItem('User ID', user.id, true)}
          ${detailItem('Email', user.email)}
          ${detailItem('Телефон', user.phone)}
          ${detailItem('Роли', (user.roles || []).join(', '), true)}
          ${detailItem('Статус', user.enabled ? 'Активен' : 'Отключён')}
          ${detailItem('Clinic ID', user.clinicId, true)}
        </div>
      `
    );
}

function roleOptionsHtml(user) {
    const roles = new Set(user.roles || []);
    const selectedRole = selectedAdminRoles.get(String(user.id)) || ROLE_NAMES[0];

    return ROLE_NAMES.map(roleName => {
        const assigned = roles.has(roleName);

        return `
      <option value="${roleName}" ${roleName === selectedRole ? 'selected' : ''}>
        ${roleName}${assigned ? ' — назначена' : ''}
      </option>
    `;
    }).join('');
}

function renderAdminUsers(users) {
    const resultLabel = $('#adminUserSearchResult');

    if (resultLabel) {
        resultLabel.textContent = `Найдено: ${users.length}`;
    }

    caches.users = new Map(
        users.map(user => [user.id, user])
    );

    renderTable(
        $('#adminUsers'),
        users,
        [
            {
                label: 'ФИО',
                render: user => `
            <button class="link-button admin-user-details" data-id="${user.id}">
              ${escapeHtml(user.lastName ?? '')} ${escapeHtml(user.firstName ?? '')}
            </button>
          `
            },
            {label: 'Email', key: 'email'},
            {
                label: 'Роли',
                render: user => escapeHtml((user.roles || []).join(', ') || '—')
            },
            {
                label: 'Статус',
                render: user => `
            <span class="badge ${user.enabled ? 'success' : 'danger'}">
              ${user.enabled ? 'Активен' : 'Отключён'}
            </span>
          `
            }
        ],
        user => `
        <div class="role-manager" data-user-id="${user.id}">
          <select class="admin-role-select" data-user-id="${user.id}">
            ${roleOptionsHtml(user)}
          </select>
          <button class="btn btn-success btn-sm admin-role-assign" data-user-id="${user.id}">
            Назначить
          </button>
          <button class="btn btn-danger btn-sm admin-role-remove" data-user-id="${user.id}">
            Удалить
          </button>
        </div>
      `
    );
}

function filterAdminUsersByEmail() {
    const searchInput = $('#adminUserEmailSearch');
    const query = searchInput?.value.trim().toLowerCase() ?? '';

    if (!query) {
        renderAdminUsers(loadedAdminUsers);
        return;
    }

    const filteredUsers = loadedAdminUsers.filter(user => {
        const email = String(user.email ?? '').toLowerCase();

        return email.includes(query);
    });

    renderAdminUsers(filteredUsers);
}

async function loadAdminUsers({silent = false} = {}) {
    try {
        loadedAdminUsers = await api(
            '/api/admin/users',
            {method: 'GET'},
            'admin'
        );

        filterAdminUsersByEmail();
    } catch (error) {
        if (!silent) {
            toast(error.message, 'error');
        }
    }
}

$('#loadAdminUsers').addEventListener('click', () => {
    loadAdminUsers();
});

$('#adminUserSearchForm').addEventListener('submit', event => {
    event.preventDefault();
    filterAdminUsersByEmail();
});

$('#adminUserEmailSearch').addEventListener('input', () => {
    filterAdminUsersByEmail();
});

$('#clearAdminUserSearch').addEventListener('click', () => {
    $('#adminUserEmailSearch').value = '';
    renderAdminUsers(loadedAdminUsers);
});

$('#adminUsers').addEventListener('change', event => {
    const select = event.target.closest('.admin-role-select');

    if (select) {
        selectedAdminRoles.set(String(select.dataset.userId), select.value);
    }
});

$('#adminUsers').addEventListener('click', async event => {
    const details = event.target.closest('.admin-user-details');
    const assign = event.target.closest('.admin-role-assign');
    const remove = event.target.closest('.admin-role-remove');

    try {
        if (details) {
            await getAdminUserById(details.dataset.id);
            return;
        }

        const actionButton = assign ?? remove;

        if (!actionButton) {
            return;
        }

        const userId = actionButton.dataset.userId;
        const roleSelect = $(`.admin-role-select[data-user-id="${userId}"]`);
        const roleName = roleSelect?.value;

        if (roleName) {
            selectedAdminRoles.set(String(userId), roleName);
        }
        const roleId = ROLE_IDS[roleName];

        if (!roleId) {
            toast('Выберите роль', 'error');
            return;
        }

        await api(
            `/api/users/${userId}/roles/${roleId}`,
            {
                method: assign ? 'POST' : 'DELETE'
            },
            'admin'
        );

        toast(
            assign
                ? `Роль ${roleName} назначена`
                : `Роль ${roleName} удалена`,
            'success'
        );

        // Сохраняем выбор до перерисовки таблицы после ответа API.
        selectedAdminRoles.set(String(userId), roleName);
        await loadAdminUsers();
    } catch (error) {
        toast(error.message, 'error');
    }
});

$('#detailModalActions').addEventListener('click', async event => {
    try {
        const clinicEdit = event.target.closest('.clinic-edit-modal');
        const clinicSave = event.target.closest('.clinic-save-modal');
        const catalogEdit = event.target.closest('.catalog-edit-modal');
        const catalogSave = event.target.closest('.catalog-save-modal');

        if (clinicEdit) {
            const clinic = caches.clinics.get(clinicEdit.dataset.id);
            if (clinic) {
                openClinicEdit(clinic);
            }
        }

        if (clinicSave) {
            await api(
                `/api/admin/clinics/${clinicSave.dataset.id}`,
                {
                    method: 'PATCH',
                    body: formToObject($('#modalClinicEditForm'))
                },
                'admin'
            );

            closeModal();
            toast('Клиника обновлена', 'success');
            await loadClinics();
        }

        if (catalogEdit) {
            const service = caches.dentalServices.get(catalogEdit.dataset.id);
            if (service) {
                openDentalServiceEdit(service);
            }
        }

        if (catalogSave) {
            const body = formToObject($('#modalCatalogEditForm'));

            if (body.defaultDurationMinutes) {
                body.defaultDurationMinutes = Number(body.defaultDurationMinutes);
            }

            await api(
                `/api/admin/catalog/dental-services/${catalogSave.dataset.id}`,
                {
                    method: 'PATCH',
                    body
                },
                'admin'
            );

            closeModal();
            toast('Услуга обновлена', 'success');
            await loadCatalog();
        }
    } catch (error) {
        toast(error.message, 'error');
    }
});

const viewLoaders = {
    'admin-clinics': loadClinics,
    'admin-catalog': loadCatalog,
    'admin-api-keys': loadApiKeys,
    'admin-notifications': loadNotifications,
    'admin-users': loadAdminUsers,
    'admin-system': loadSystemStatus
};

function getCurrentViewName() {
    const activeView = $('.view.active');

    return activeView
        ? activeView.id.replace('view-', '')
        : null;
}

function isAdminEditing() {
    const activeElement = document.activeElement;
    const modalIsOpen = !$('#detailModal').classList.contains('hidden');
    const activeView = $('.view.active');
    const formControlIsFocused = Boolean(
        activeElement
        && activeView?.contains(activeElement)
        && activeElement.matches(
            'input, select, textarea, button, [contenteditable="true"]'
        )
    );
    const dirtyFormExists = Boolean(
        activeView?.querySelector('form[data-user-dirty="true"]')
    );

    return modalIsOpen || formControlIsFocused || dirtyFormExists;
}

function bindAdminDirtyFormTracking() {
    document.addEventListener('input', event => {
        const form = event.target.closest?.('form');
        if (form) form.dataset.userDirty = 'true';
    });

    document.addEventListener('change', event => {
        const form = event.target.closest?.('form');
        if (form) form.dataset.userDirty = 'true';
    });

    document.addEventListener('reset', event => {
        if (event.target instanceof HTMLFormElement) {
            delete event.target.dataset.userDirty;
        }
    });

    document.addEventListener('submit', event => {
        if (event.target instanceof HTMLFormElement) {
            delete event.target.dataset.userDirty;
        }
    });
}

async function refreshCurrentView({automatic = false} = {}) {
    if (
        refreshInProgress
        || document.hidden
        || !isAdminAuthenticated()
    ) {
        return;
    }

    const viewName = getCurrentViewName();

    if (
        automatic
        && (
            !AUTO_REFRESH_VIEWS.has(viewName)
            || isAdminEditing()
        )
    ) {
        return;
    }

    const loader = viewLoaders[viewName];

    if (!loader) {
        return;
    }

    refreshInProgress = true;

    try {
        await loader({
            silent: automatic
        });
    } catch (error) {
        console.error(
            automatic
                ? 'Ошибка автоматического обновления:'
                : 'Ошибка загрузки раздела:',
            error
        );
    } finally {
        refreshInProgress = false;
    }
}

function startAutoRefresh() {
    // Только первоначальная загрузка. Дальнейшее обновление выполняется кнопками
    // «Обновить», чтобы не терять выбранные значения и несохранённые изменения.
    stopAutoRefresh();
    refreshCurrentView();
}

function stopAutoRefresh() {
    if (autoRefreshTimer) {
        window.clearInterval(autoRefreshTimer);
        autoRefreshTimer = null;
    }
}

// Возврат на вкладку не перерисовывает админку автоматически.

function initialize() {
    setupRequiredFieldMarkers();
    bindAdminDirtyFormTracking();
    updateSession();

    // На /apis DOM админки уже подготовлен, но скрыт. Данные Dent загружаются
    // только после клиентского перехода в продукт.
    if (location.pathname.replace(/\/+$/, '') === '/apis') return;

    const initialView = adminViewFromPath();
    showView(initialView, {updateUrl: false});

    if (isAdminAuthenticated()) {
        startAutoRefresh();
    }
}

window.bublapiAdminWorkspace = {
    showView: name => showView(name, {updateUrl: false}),
    suspend: stopAutoRefresh
};
window.dispatchEvent(new Event('bublapi:admin-ready'));

initialize();

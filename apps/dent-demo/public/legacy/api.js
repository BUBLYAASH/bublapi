const USER_TOKEN_KEY = 'bublapi.userToken';
const ADMIN_TOKEN_KEY = 'bublapi.adminToken';
const ACCESS_PROFILE_KEY = 'bublapi.accessProfile';
const LEGACY_USER_TOKEN_KEYS = ['token', 'accessToken', 'bublapiToken'];

function safeStorageGet(storage, key) {
  try {
    return storage.getItem(key) || '';
  } catch {
    return '';
  }
}

function safeStorageSet(storage, key, value) {
  try {
    storage.setItem(key, value);
  } catch {
    // Некоторые браузеры могут ограничивать один из storage-механизмов.
  }
}

export function getToken(kind = 'user') {
  const key = kind === 'admin' ? ADMIN_TOKEN_KEY : USER_TOKEN_KEY;
  const storages = [localStorage, sessionStorage];

  for (const storage of storages) {
    const token = safeStorageGet(storage, key);
    if (token) return token;
  }

  if (kind === 'user') {
    for (const legacyKey of LEGACY_USER_TOKEN_KEYS) {
      for (const storage of storages) {
        const token = safeStorageGet(storage, legacyKey);
        if (token) {
          safeStorageSet(localStorage, USER_TOKEN_KEY, token);
          return token;
        }
      }
    }
  }

  return '';
}

export function setToken(token, kind = 'user') {
  const key = kind === 'admin' ? ADMIN_TOKEN_KEY : USER_TOKEN_KEY;
  const normalizedToken = String(token || '').trim();

  safeStorageSet(localStorage, key, normalizedToken);
  safeStorageSet(sessionStorage, key, normalizedToken);
}

export function clearToken(kind = 'user') {
  const key = kind === 'admin'
      ? ADMIN_TOKEN_KEY
      : USER_TOKEN_KEY;

  localStorage.removeItem(key);
  sessionStorage.removeItem(key);

  if (kind === 'user') {
    clearAccessProfile();
  }
}

export function decodeJwt(token) {
  if (!token) {
    return null;
  }

  try {
    const payload = token.split('.')[1];
    const normalized = payload
    .replace(/-/g, '+')
    .replace(/_/g, '/');

    const decoded = atob(normalized);
    const json = decodeURIComponent(
        decoded
        .split('')
        .map(character => {
          const code = character.charCodeAt(0)
          .toString(16)
          .padStart(2, '0');

          return `%${code}`;
        })
        .join('')
    );

    return JSON.parse(json);
  } catch {
    return null;
  }
}


function extractErrorMessage(data, rawText, status) {
  if (typeof data === 'string' && data.trim()) {
    return data;
  }

  if (typeof data?.message === 'string' && data.message.trim()) {
    return data.message;
  }

  if (typeof data?.error === 'string' && data.error.trim()) {
    return data.error;
  }

  if (typeof data?.details === 'string' && data.details.trim()) {
    return data.details;
  }

  if (Array.isArray(data?.details)) {
    const details = data.details
    .map(detail => {
      if (typeof detail === 'string') {
        return detail;
      }

      return detail?.message
          ?? detail?.defaultMessage
          ?? JSON.stringify(detail);
    })
    .filter(Boolean)
    .join('\n');

    if (details) {
      return details;
    }
  }

  if (data?.validationErrors && typeof data.validationErrors === 'object') {
    const validationMessage = Object.entries(data.validationErrors)
    .map(([field, message]) => `${field}: ${message}`)
    .join('\n');

    if (validationMessage) {
      return validationMessage;
    }
  }

  if (rawText?.trim()) {
    return rawText;
  }

  return `Ошибка HTTP ${status}`;
}

export async function api(path, options = {}, kind = 'user') {
  const { skipAuth = false, ...requestOptions } = options;
  const headers = new Headers(requestOptions.headers || {});
  const token = skipAuth ? '' : getToken(kind);

  if (token) {
    const authorization = token.startsWith('Bearer ')
        ? token
        : `Bearer ${token}`;

    headers.set('Authorization', authorization);
  }

  const hasBody = requestOptions.body !== undefined;
  const isFormData = requestOptions.body instanceof FormData;

  if (hasBody && !isFormData) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(path, {
    ...requestOptions,
    headers,
    cache: requestOptions.cache ?? (String(requestOptions.method ?? 'GET').toUpperCase() === 'GET' ? 'no-store' : 'default'),
    body: hasBody && !isFormData
        ? JSON.stringify(requestOptions.body)
        : requestOptions.body
  });

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    if (kind === 'admin' && (response.status === 401 || response.status === 403)) {
      clearToken('admin');
    }

    const message = extractErrorMessage(
        data,
        text,
        response.status
    );

    const error = new Error(message);
    error.status = response.status;
    error.data = data;

    throw error;
  }

  return data;
}

export async function probeAccess(path, kind = 'user') {
  try {
    await api(
        path,
        {
          method: 'GET'
        },
        kind
    );

    return true;
  } catch (error) {
    return error.status !== 401 && error.status !== 403;
  }
}

export function getAccessProfile() {
  try {
    return JSON.parse(
        localStorage.getItem(ACCESS_PROFILE_KEY)
        || '{}'
    );
  } catch {
    return {};
  }
}

export function setAccessProfile(profile) {
  localStorage.setItem(
      ACCESS_PROFILE_KEY,
      JSON.stringify(profile)
  );
}

export function clearAccessProfile() {
  localStorage.removeItem(ACCESS_PROFILE_KEY);
}

export function formToObject(form) {
  const result = {};

  for (const [key, rawValue] of new FormData(form).entries()) {
    const value = typeof rawValue === 'string'
        ? rawValue.trim()
        : rawValue;

    if (value !== '') {
      if (key === 'phone' && typeof value === 'string') {
        const phoneInput = form.elements.namedItem(key);
        const normalizedPhone = phoneInput?.dataset?.phoneNormalized
            || value.replace(/\D/g, '');

        if (normalizedPhone) {
          result[key] = normalizedPhone;
        }
      } else {
        result[key] = value;
      }
    }
  }

  return result;
}

export function fillForm(form, values) {
  Object.entries(values || {}).forEach(([key, value]) => {
    const field = form.elements.namedItem(key);

    if (!field) {
      return;
    }

    field.value = value ?? '';
  });
}


export function setupRequiredFieldMarkers(root = document) {
  const mark = scope => {
    scope.querySelectorAll('input[required], select[required], textarea[required], input[data-required-marker="true"], select[data-required-marker="true"], textarea[data-required-marker="true"]').forEach(field => {
      if (field.type === 'hidden') return;

      let label = field.closest('label');
      if (!label && field.id) {
        label = scope.querySelector(`label[for="${CSS.escape(field.id)}"]`)
          || document.querySelector(`label[for="${CSS.escape(field.id)}"]`);
      }
      if (!label || label.querySelector('.field-label-title')) return;

      const firstControl = label.querySelector('input, select, textarea');
      if (!firstControl) return;

      // insertBefore требует, чтобы referenceNode был непосредственным ребёнком label.
      // Телефонные поля оборачиваются в .phone-input-group, поэтому сам input может
      // находиться глубже. Находим верхний узел внутри label, содержащий control.
      let controlAnchor = firstControl;
      while (controlAnchor.parentElement && controlAnchor.parentElement !== label) {
        controlAnchor = controlAnchor.parentElement;
      }
      if (controlAnchor.parentElement !== label) return;

      const title = document.createElement('span');
      title.className = 'field-label-title';

      const nodesBeforeControl = [];
      for (const node of Array.from(label.childNodes)) {
        if (node === controlAnchor) break;
        nodesBeforeControl.push(node);
      }

      for (const node of nodesBeforeControl) title.appendChild(node);
      title.normalize();

      const markElement = document.createElement('span');
      markElement.className = 'required-mark';
      markElement.setAttribute('aria-hidden', 'true');
      markElement.textContent = '*';
      title.appendChild(markElement);

      label.insertBefore(title, controlAnchor);
    });
  };

  mark(root);

  const observer = new MutationObserver(mutations => {
    mutations.forEach(mutation => {
      mutation.addedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) mark(node);
      });
    });
  });

  observer.observe(root === document ? document.body : root, {
    childList: true,
    subtree: true
  });
}

export function toast(message, type = '') {
  let container = document.querySelector('.toast-container');

  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.append(container);
  }

  container.setAttribute('aria-live', 'polite');
  container.setAttribute('aria-atomic', 'false');

  const item = document.createElement('div');
  item.className = `toast ${type}`;
  item.setAttribute('role', type === 'error' ? 'alert' : 'status');
  item.textContent = typeof message === 'string'
      ? message
      : JSON.stringify(message, null, 2);
  container.append(item);

  window.setTimeout(
      () => item.remove(),
      4200
  );
}

export function formatDate(value) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
      ? String(value)
      : date.toLocaleString('ru-RU');
}

export function escapeHtml(value) {
  return String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');
}

export function detailItem(label, value, full = false) {
  return `
    <div class="detail-item ${full ? 'full' : ''}">
      <span class="detail-label">${escapeHtml(label)}</span>
      <div>${escapeHtml(value ?? '—')}</div>
    </div>
  `;
}

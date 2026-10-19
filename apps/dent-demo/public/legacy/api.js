const ACCESS_PROFILE_KEY = 'bublapi.accessProfile';
let accessToken = '';
let refreshPromise;
const authEvents = typeof BroadcastChannel === 'function' ? new BroadcastChannel('bublapi-user-auth') : null;
function purgeLegacyTokens() {
  for (const name of ['localStorage', 'sessionStorage']) {
    try {
      for (const key of ['bublapi.userToken', 'bublapi.adminToken', 'token', 'accessToken', 'bublapiToken']) window[name].removeItem(key);
    } catch {}
  }
}
purgeLegacyTokens();
export function getToken(kind = 'user') { return kind === 'user' ? accessToken : ''; }
export function setToken(token, kind = 'user') {
  if (kind !== 'user') throw new Error('Admin сессия доступна только в admin приложении');
  accessToken = String(token || '').trim();
}
export function clearToken() {
  accessToken = '';
  purgeLegacyTokens();
  clearAccessProfile();
}
authEvents?.addEventListener('message', event => {
  if (event.data === 'logout' || event.data === 'login') {
    clearToken();
    location.reload();
  }
});
function withAuthLock(work) {
  if (!navigator.locks) return Promise.reject(new Error('Для безопасного обновления сессии нужен браузер с Web Locks (HTTPS).'));
  return navigator.locks.request('bublapi-user-auth', work);
}
export function refreshSession(rejectedToken) {
  if (refreshPromise) return refreshPromise;
  refreshPromise = withAuthLock(async () => {
    if (rejectedToken && accessToken && accessToken !== rejectedToken) return accessToken;
    const response = await fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) {
      if (response.status === 401) { clearToken(); window.dispatchEvent(new CustomEvent('user:unauthorized')); }
      const error = new Error('Не удалось обновить сессию');
      error.status = response.status;
      throw error;
    }
    const payload = await response.json();
    if (!payload?.accessToken) throw new Error('Backend не вернул accessToken');
    setToken(payload.accessToken);
    return accessToken;
  }).finally(() => { refreshPromise = null; });
  return refreshPromise;
}
export async function restoreSession() {
  try { await refreshSession(); }
  catch (error) {
    if (error.status !== 401) throw error;
  }
}
export async function logoutSession() {
  await withAuthLock(async () => {
    const response = await fetch('/api/v1/auth/logout', { method: 'POST', credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) throw new Error('Не удалось завершить сессию. Повторите выход.');
    clearToken();
    authEvents?.postMessage('logout');
  });
}
export async function sessionToken() {
  if (!accessToken) return '';
  const exp = decodeJwt(accessToken)?.exp;
  if (!exp || exp * 1000 < Date.now() + 30000) await refreshSession(accessToken);
  return accessToken;
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
  if (kind !== 'user') throw new Error('Используйте отдельное admin приложение');
  const { skipAuth = false, ...requestOptions } = options;
  const headers = new Headers(requestOptions.headers || {});
  const authAction = /^\/api\/v1\/auth\//.test(path);
  if (!path.startsWith('/api/v1/') || new URL(path, location.origin).origin !== location.origin) throw new Error('Недопустимый API URL');
  const token = skipAuth || authAction ? '' : await sessionToken();

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

  const init = {
    ...requestOptions, headers, credentials: 'same-origin', cache: 'no-store',
    body: hasBody && !isFormData ? JSON.stringify(requestOptions.body) : requestOptions.body
  };
  let response;
  if (path === '/api/v1/auth/login') {
    response = await withAuthLock(async () => {
      const result = await fetch(path, init);
      if (result.ok) {
        const payload = await result.clone().json();
        if (!payload?.accessToken) throw new Error('Backend не вернул accessToken');
        setToken(payload.accessToken);
        authEvents?.postMessage('login');
      }
      return result;
    });
  } else {
    response = await fetch(path, init);
    if (response.status === 401 && token && !authAction && !skipAuth) {
      await response.body?.cancel();
      const renewed = await refreshSession(token);
      headers.set('Authorization', `Bearer ${renewed}`);
      response = await fetch(path, init);
      if (response.status === 401) { clearToken(); window.dispatchEvent(new CustomEvent('user:unauthorized')); }
    }
  }

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

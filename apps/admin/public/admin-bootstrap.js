(() => {
  'use strict';

  const root = document.documentElement;
  let raf = 0;
  let observedTopbar = null;
  let resizeObserver = null;
  let notificationsObserver = null;

  function rewriteAdminUsersUrl(value) {
    try {
      const absolute = new URL(String(value), window.location.origin);
      if (absolute.pathname === '/api/users' || absolute.pathname.startsWith('/api/users/')) {
        absolute.pathname = absolute.pathname.replace(/^\/api\/users/, '/api/admin/users');
        return absolute.origin === window.location.origin
          ? `${absolute.pathname}${absolute.search}${absolute.hash}`
          : absolute.toString();
      }
    } catch {}
    return value;
  }

  function requestMethod(input, init) {
    return String(
      init?.method ||
      (input instanceof Request ? input.method : 'GET') ||
      'GET'
    ).toUpperCase();
  }

  function requestPath(input) {
    try {
      const value = input instanceof Request ? input.url : String(input);
      return new URL(value, window.location.origin).pathname;
    } catch {
      return '';
    }
  }

  function withFreshQuery(input) {
    const path = requestPath(input);
    const freshTargets = [
      '/api/admin/clinics',
      '/api/admin/catalog/dental-services',
      '/api/admin/users',
      '/api/admin/notifications'
    ];

    if (!freshTargets.some(prefix => path === prefix || path.startsWith(`${prefix}/`))) {
      return input;
    }

    const raw = input instanceof Request ? input.url : String(input);
    const url = new URL(raw, window.location.origin);
    url.searchParams.set('_', String(Date.now()));
    const next = url.origin === window.location.origin
      ? `${url.pathname}${url.search}${url.hash}`
      : url.toString();

    return input instanceof Request ? new Request(next, input) : next;
  }

  function showSuccess(message) {
    let host = document.querySelector('#adminRuntimeSuccessHost');
    if (!host) {
      host = document.createElement('div');
      host.id = 'adminRuntimeSuccessHost';
      Object.assign(host.style, {
        position: 'fixed',
        right: '18px',
        top: 'calc(var(--admin-topbar-height, 72px) + 18px)',
        zIndex: '20000',
        display: 'grid',
        gap: '8px',
        maxWidth: '380px',
        pointerEvents: 'none'
      });
      document.body.append(host);
    }

    const note = document.createElement('div');
    note.textContent = message;
    note.setAttribute('role', 'status');
    Object.assign(note.style, {
      padding: '12px 14px',
      borderRadius: '12px',
      background: 'var(--surface, #fff)',
      color: 'var(--text, #111)',
      boxShadow: '0 10px 30px rgba(0,0,0,.18)',
      border: '1px solid rgba(46, 160, 67, .45)'
    });
    host.append(note);
    setTimeout(() => note.remove(), 3200);
  }

  function showError(message) {
    let host = document.querySelector('#adminRuntimeErrorHost');
    if (!host) {
      host = document.createElement('div');
      host.id = 'adminRuntimeErrorHost';
      Object.assign(host.style, {
        position: 'fixed',
        right: '18px',
        top: 'calc(var(--admin-topbar-height, 72px) + 18px)',
        zIndex: '20001',
        display: 'grid',
        gap: '8px',
        maxWidth: '420px',
        pointerEvents: 'none'
      });
      document.body.append(host);
    }

    const note = document.createElement('div');
    note.textContent = message;
    note.setAttribute('role', 'alert');
    Object.assign(note.style, {
      padding: '12px 14px',
      borderRadius: '12px',
      background: 'var(--surface, #fff)',
      color: 'var(--text, #111)',
      boxShadow: '0 10px 30px rgba(0,0,0,.18)',
      border: '1px solid rgba(220, 53, 69, .55)'
    });
    host.append(note);
    setTimeout(() => note.remove(), 4200);
  }

  function clickRefresh(selector) {
    const button = document.querySelector(selector);
    if (button && !button.disabled) button.click();
  }

  function reactToSuccessfulMutation(path, method) {
    if (method === 'POST' && path === '/api/admin/clinics') {
      showSuccess('Клиника успешно создана');
      setTimeout(() => clickRefresh('#loadClinics'), 50);
      setTimeout(() => clickRefresh('#loadClinics'), 450);
      return;
    }

    if (method === 'POST' && path === '/api/admin/catalog/dental-services') {
      showSuccess('Услуга успешно создана');
      setTimeout(() => clickRefresh('#loadCatalog'), 50);
      setTimeout(() => clickRefresh('#loadCatalog'), 450);
      return;
    }

    if (
      path.startsWith('/api/admin/clinics/') &&
      ['PATCH', 'PUT', 'DELETE'].includes(method)
    ) {
      setTimeout(() => clickRefresh('#loadClinics'), 80);
      return;
    }

    if (
      path.startsWith('/api/admin/catalog/dental-services/') &&
      ['PATCH', 'PUT', 'DELETE'].includes(method)
    ) {
      setTimeout(() => clickRefresh('#loadCatalog'), 80);
      return;
    }

    if (
      path === '/api/admin/users' ||
      path.startsWith('/api/admin/users/')
    ) {
      if (method !== 'GET') setTimeout(() => clickRefresh('#loadAdminUsers'), 80);
      return;
    }

    if (
      method === 'POST' &&
      /^\/api\/admin\/notifications\/[^/]+\/retry$/.test(path)
    ) {
      showSuccess('Повторная отправка уведомления запущена');
      setTimeout(() => clickRefresh('#loadAdminNotifications'), 120);
      setTimeout(() => clickRefresh('#loadAdminNotifications'), 1200);
      return;
    }

    if (
      method === 'DELETE' &&
      /^\/api\/admin\/notifications\/[^/]+$/.test(path)
    ) {
      showSuccess('Уведомление удалено');
      setTimeout(() => clickRefresh('#loadAdminNotifications'), 80);
    }
  }

  // Run before the legacy admin bundle. This removes the old /api/users
  // compatibility dependency and always uses the dedicated system-admin API.
  const nativeFetch = window.fetch.bind(window);
  window.fetch = async function adminFixedFetch(input, init = undefined) {
    let nextInput = input;

    if (input instanceof Request) {
      const rewritten = rewriteAdminUsersUrl(input.url);
      if (rewritten !== input.url) nextInput = new Request(rewritten, input);
    } else {
      nextInput = rewriteAdminUsersUrl(input);
    }

    const method = requestMethod(nextInput, init);
    if (method === 'GET') nextInput = withFreshQuery(nextInput);

    const path = requestPath(nextInput);
    const response = await nativeFetch(nextInput, init);

    if (response.ok && method !== 'GET') {
      queueMicrotask(() => reactToSuccessfulMutation(path, method));
    }

    return response;
  };

  async function runAdminMutation(path, method) {
    const response = await window.fetch(path, {
      method,
      credentials: 'same-origin'
    });

    if (response.ok) return;

    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent('admin:unauthorized'));
    }

    const text = await response.text();
    let message = text;

    if (text) {
      try {
        const data = JSON.parse(text);
        message = data?.message || data?.error || data?.details || text;
      } catch {}
    }

    throw new Error(message || `Ошибка HTTP ${response.status}`);
  }

  function enhanceNotificationsTable() {
    const container = document.querySelector('#adminNotifications');
    const table = container?.querySelector('table');

    if (!table || table.dataset.notificationActionsReady === 'true') {
      return;
    }

    const headerRow = table.tHead?.rows?.[0];
    const bodyRows = table.tBodies?.[0]?.rows;

    if (!headerRow || !bodyRows) {
      return;
    }

    const header = document.createElement('th');
    header.scope = 'col';
    header.textContent = 'Действия';
    headerRow.append(header);

    Array.from(bodyRows).forEach(row => {
      const details = row.querySelector('.notification-details');
      const notificationId = details?.dataset.id;

      if (!notificationId) {
        return;
      }

      const status = row.querySelector('.badge')?.textContent?.trim();
      const cell = document.createElement('td');
      const actions = document.createElement('div');
      actions.className = 'actions';

      if (status === 'FAILED') {
        const retry = document.createElement('button');
        retry.className = 'btn btn-secondary btn-sm notification-retry';
        retry.type = 'button';
        retry.dataset.id = notificationId;
        retry.textContent = 'Повторить';
        actions.append(retry);
      }

      const remove = document.createElement('button');
      remove.className = 'btn btn-danger btn-sm notification-delete';
      remove.type = 'button';
      remove.dataset.id = notificationId;
      remove.textContent = 'Удалить';
      actions.append(remove);

      cell.append(actions);
      row.append(cell);
    });

    table.dataset.notificationActionsReady = 'true';
  }

  async function handleNotificationAction(event) {
    const retry = event.target.closest?.('.notification-retry');
    const remove = event.target.closest?.('.notification-delete');

    if (!retry && !remove) {
      return;
    }

    const button = retry || remove;
    const notificationId = button.dataset.id;

    if (!notificationId || button.disabled) {
      return;
    }

    if (remove) {
      const confirmed = window.confirm(
        'Удалить уведомление? Оно исчезнет из системной админ-панели.'
      );

      if (!confirmed) {
        return;
      }
    }

    const idleLabel = button.textContent;
    button.disabled = true;
    button.textContent = retry ? 'Повторяем…' : 'Удаляем…';
    button.setAttribute('aria-busy', 'true');

    try {
      if (retry) {
        await runAdminMutation(
          `/api/admin/notifications/${notificationId}/retry`,
          'POST'
        );
      } else {
        await runAdminMutation(
          `/api/admin/notifications/${notificationId}`,
          'DELETE'
        );
      }
    } catch (error) {
      showError(error instanceof Error ? error.message : String(error));
      button.disabled = false;
      button.textContent = idleLabel;
      button.removeAttribute('aria-busy');
    }
  }

  function observeNotificationsTable() {
    const container = document.querySelector('#adminNotifications');

    if (!container) {
      return;
    }

    notificationsObserver?.disconnect();
    notificationsObserver = new MutationObserver(() => {
      queueMicrotask(enhanceNotificationsTable);
    });
    notificationsObserver.observe(container, {
      childList: true,
      subtree: true
    });

    enhanceNotificationsTable();
    container.addEventListener('click', handleNotificationAction);
  }

  function updateTopbarHeight() {
    cancelAnimationFrame(raf);
    raf = requestAnimationFrame(() => {
      const topbar = document.querySelector('.topbar');
      if (!topbar) return;

      // offsetHeight is less sensitive to transforms/subpixel layout, while
      // getBoundingClientRect catches wrapped mobile content. Use the larger.
      const rectHeight = Math.ceil(topbar.getBoundingClientRect().height);
      const height = Math.max(rectHeight, topbar.offsetHeight || 0);
      if (height > 0) {
        root.style.setProperty('--admin-topbar-height', `${height}px`);
        root.dataset.adminTopbarMeasured = 'true';
      }

      if (observedTopbar !== topbar && 'ResizeObserver' in window) {
        resizeObserver?.disconnect();
        resizeObserver = new ResizeObserver(updateTopbarHeight);
        resizeObserver.observe(topbar);
        observedTopbar = topbar;
      }
    });
  }

  function start() {
    updateTopbarHeight();
    observeNotificationsTable();

    window.addEventListener('resize', updateTopbarHeight, { passive: true });
    window.addEventListener('orientationchange', updateTopbarHeight, { passive: true });
    window.addEventListener('pageshow', updateTopbarHeight, { passive: true });

    // Mobile browsers can change the visual viewport without firing a normal
    // layout resize (address bar / orientation / keyboard transitions).
    window.visualViewport?.addEventListener('resize', updateTopbarHeight, { passive: true });
    window.visualViewport?.addEventListener('scroll', updateTopbarHeight, { passive: true });

    document.fonts?.ready?.then(updateTopbarHeight).catch(() => {});

    document.addEventListener('click', event => {
      const button = event.target.closest?.('.sidebar .nav-button');
      if (!button) return;

      requestAnimationFrame(() => {
        button.scrollIntoView({
          block: 'nearest',
          inline: 'nearest',
          behavior: 'smooth'
        });
        updateTopbarHeight();
      });
    }, true);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();
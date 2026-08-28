/*
 * Sidebar context synchronizer.
 *
 * The legacy app has two different staff-related groups:
 *  1. .sidebar-context-entry -> one button: "Панель сотрудника"
 *  2. the actual staff workspace group -> Рабочий стол / Пациенты / ...
 *
 * Access visibility and route context are resolved at different moments.
 * This guard makes the final state deterministic and prevents either the
 * workspace buttons leaking into the patient menu or the entry button
 * disappearing after auth/bootstrap.
 */
(() => {
  'use strict';

  const root = document.documentElement;
  let syncing = false;
  let scheduled = false;

  function qs(selector, scope = document) {
    return scope.querySelector(selector);
  }

  function qsa(selector, scope = document) {
    return [...scope.querySelectorAll(selector)];
  }

  function readStoredAccess() {
    for (const storage of [localStorage, sessionStorage]) {
      try {
        const raw = storage.getItem('bublapi.accessProfile');
        if (!raw) continue;
        const profile = JSON.parse(raw);
        return Boolean(profile?.['staff-core'] || profile?.['staff-appointments']);
      } catch {}
    }
    return false;
  }

  function isAuthenticated() {
    const logout = qs('#logoutBtn');
    if (logout && !logout.classList.contains('hidden')) return true;

    try {
      return Boolean(
        localStorage.getItem('bublapi.userToken') ||
        sessionStorage.getItem('bublapi.userToken')
      );
    } catch {
      return false;
    }
  }

  function isStaffRoute() {
    return /^\/staff(?:\/|$)/.test(location.pathname);
  }

  function findStaffWorkspaceGroup(sidebar) {
    return qsa(
      ':scope > .nav-group[data-access="staff-core,staff-appointments"]',
      sidebar
    ).find(group => !group.classList.contains('sidebar-context-entry')) || null;
  }

  function syncSidebarContext() {
    if (syncing || !document.body) return;

    const sidebar = qs('.sidebar');
    const entry = qs('.sidebar-context-entry');
    const exit = qs('.sidebar-context-exit');
    if (!sidebar || !entry || !exit) return;

    syncing = true;
    try {
      const workspace = findStaffWorkspaceGroup(sidebar);
      const authenticated = isAuthenticated();

      // At the exact moment auth bootstrap finishes, applyResolvedAccess()
      // already toggles the real staff group according to fresh permission
      // probes. Later the cached access profile becomes the stable source.
      const staffAllowed = authenticated && Boolean(
        readStoredAccess() ||
        (root.dataset.authReady === 'true' &&
          workspace &&
          !workspace.classList.contains('hidden'))
      );

      const staffMode = isStaffRoute();
      document.body.dataset.sidebarMode = staffMode ? 'staff' : 'patient';

      if (staffMode) {
        entry.classList.add('hidden');
        exit.classList.toggle('hidden', !staffAllowed);

        // Never grant access here. We only keep an already-authorized group
        // visible. If access is absent, legacy access control remains decisive.
        if (!staffAllowed && workspace) workspace.classList.add('hidden');
      } else {
        exit.classList.add('hidden');
        entry.classList.toggle('hidden', !staffAllowed);

        // The full staff navigation belongs strictly inside /staff*.
        if (workspace) workspace.classList.add('hidden');
      }
    } finally {
      syncing = false;
    }
  }

  function scheduleSync() {
    if (scheduled) return;
    scheduled = true;
    queueMicrotask(() => {
      scheduled = false;
      syncSidebarContext();
    });
  }

  function start() {
    syncSidebarContext();

    const sidebar = qs('.sidebar');
    if (sidebar) {
      new MutationObserver(scheduleSync).observe(sidebar, {
        subtree: true,
        attributes: true,
        attributeFilter: ['class']
      });
    }

    new MutationObserver(scheduleSync).observe(root, {
      attributes: true,
      attributeFilter: ['data-auth-ready']
    });

    window.addEventListener('popstate', scheduleSync);

    // history.pushState() does not emit popstate. Nav buttons do, however,
    // change active classes, so the sidebar observer catches normal app
    // navigation. These events cover direct programmatic changes as well.
    document.addEventListener('click', event => {
      if (event.target.closest?.('.nav-button,[data-go],#sidebarEnterStaff,#sidebarExitStaff')) {
        setTimeout(syncSidebarContext, 0);
      }
    }, true);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();

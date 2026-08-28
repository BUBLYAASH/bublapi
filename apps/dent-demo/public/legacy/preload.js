/*
 * Small bootstrap that runs before the legacy module.
 * It intentionally does not own application state; it only removes first-paint
 * flicker, stabilises mobile sticky navigation, makes calendar slot selection
 * click-only, and avoids the expensive service-agnostic 60-day availability
 * fan-out used by the staff dashboard.
 */
(() => {
  'use strict';

  const root = document.documentElement;
  const USER_TOKEN_KEY = 'bublapi.userToken';
  const ACCESS_PROFILE_KEY = 'bublapi.accessProfile';
  const nativeFetch = window.fetch.bind(window);
  const requestInFlight = new Map();
  const responseCache = new Map();
  let pendingCalendarStart = null;
  let pendingApplyGeneration = 0;

  const ROUTE_VIEW_MAP = {
    '/profile': 'profile',
    '/patient/card': 'patient-card',
    '/patient/appointments': 'patient-appointments',
    '/patient/notifications': 'patient-notifications',
    '/staff': 'staff-dashboard',
    '/staff/patients': 'staff-patients',
    '/staff/doctors': 'staff-doctors',
    '/staff/services': 'staff-services',
    '/staff/appointments': 'staff-appointments',
    '/staff/users': 'staff-users'
  };

  const initialPath = location.pathname.replace(/\/+$/, '') || '/';
  const expectedInitialView = ROUTE_VIEW_MAP[initialPath] || null;
  const protectedInitialRoute = Boolean(expectedInitialView);
  if (protectedInitialRoute) {
    root.dataset.protectedInitialRoute = 'true';
    root.dataset.expectedInitialView = expectedInitialView;
  } else {
    root.dataset.routeReady = 'true';
  }

  const qs = (selector, scope = document) => scope.querySelector(selector);
  const qsa = (selector, scope = document) => [...scope.querySelectorAll(selector)];

  function readStorage(key) {
    try {
      return localStorage.getItem(key) ?? sessionStorage.getItem(key);
    } catch {
      return null;
    }
  }

  function readToken() {
    return readStorage(USER_TOKEN_KEY);
  }

  function readCachedAccess() {
    try {
      const raw = readStorage(ACCESS_PROFILE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }

  function requirementAllowed(requirement, profile, authenticated) {
    if (!requirement) return true;
    return String(requirement)
      .split(',')
      .map(item => item.trim())
      .some(item => item === 'authenticated' ? authenticated : Boolean(profile[item]));
  }

  function syncInitialRouteReady(profile = null, authenticated = null) {
    if (!protectedInitialRoute || root.dataset.routeReady === 'true') return;

    const active = document.querySelector('.view.active');
    const activeName = active?.id?.replace(/^view-/, '') || '';
    if (activeName === expectedInitialView) {
      root.dataset.routeReady = 'true';
      return;
    }

    // If the saved session cannot access the requested protected route, the
    // legacy router intentionally falls back to home. Do not leave the loader
    // hanging forever in that case.
    if (root.dataset.authReady === 'true' && profile && authenticated !== null) {
      const needsStaff = expectedInitialView.startsWith('staff-');
      const needsPatient = expectedInitialView.startsWith('patient-');
      const allowed = expectedInitialView === 'profile'
        ? authenticated
        : needsStaff
          ? Boolean(profile['staff-core'] || profile['staff-appointments'])
          : needsPatient
            ? Boolean(profile.patient)
            : true;
      if (!allowed && activeName === 'home') root.dataset.routeReady = 'true';
    }
  }

  function installInitialRouteObserver() {
    if (!protectedInitialRoute) return;
    const check = () => syncInitialRouteReady();
    const observer = new MutationObserver(check);
    observer.observe(document.documentElement, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ['class']
    });
    check();
  }

  function applyResolvedAccess(profile, authenticated) {
    qsa('[data-access]').forEach(element => {
      element.classList.toggle(
        'hidden',
        !requirementAllowed(element.dataset.access, profile, authenticated)
      );
    });

    qsa('[data-guest-only]').forEach(element => {
      element.classList.toggle('hidden', authenticated);
    });

    const logout = qs('#logoutBtn');
    if (logout) logout.classList.toggle('hidden', !authenticated);

    root.dataset.authReady = 'true';
    syncInitialRouteReady(profile, authenticated);
  }

  async function probe(url, token) {
    try {
      const response = await window.fetch(url, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
        cache: 'no-store'
      });
      return { allowed: response.ok, status: response.status };
    } catch {
      return { allowed: false, status: 0 };
    }
  }

  async function resolveSavedSession() {
    const token = readToken();
    if (!token) {
      const apply = () => applyResolvedAccess({}, false);
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', apply, { once: true });
      } else {
        apply();
      }
      return;
    }

    /* Use the cached profile only as an internal warm hint. Nothing protected
       is revealed until the three lightweight permission probes complete. */
    const cached = readCachedAccess();
    const [patient, staffCore, staffAppointments] = await Promise.all([
      probe('/api/notifications/unread-count', token),
      probe('/api/patients', token),
      probe('/api/appointments', token)
    ]);

    const allUnauthorized = [patient, staffCore, staffAppointments]
      .every(result => result.status === 401);

    const profile = allUnauthorized ? {} : {
      patient: patient.allowed,
      'staff-core': staffCore.allowed,
      'staff-appointments': staffAppointments.allowed,
      ...Object.fromEntries(
        Object.entries(cached).filter(([key]) =>
          !['patient', 'staff-core', 'staff-appointments'].includes(key)
        )
      )
    };

    const apply = () => applyResolvedAccess(profile, !allUnauthorized);
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', apply, { once: true });
    } else {
      apply();
    }
  }

  function requestKey(input, init, url) {
    const method = String(init?.method || (input instanceof Request ? input.method : 'GET')).toUpperCase();
    let authorization = '';
    try {
      const headers = new Headers(input instanceof Request ? input.headers : undefined);
      new Headers(init?.headers || {}).forEach((value, key) => headers.set(key, value));
      authorization = headers.get('authorization') || '';
    } catch {}
    return `${method}|${url.href}|${authorization}`;
  }

  function cachedTtl(url) {
    if (/\/api\/public\/doctors\/[^/]+\/working-hours$/.test(url.pathname)) return 45_000;
    if (url.pathname === '/demo-config') return 10_000;
    if (url.pathname === '/api/notifications/unread-count') return 1_500;
    return 0;
  }

  function isDashboardAvailabilityProbe(url) {
    if (!/\/api\/public\/doctors\/[^/]+\/availability$/.test(url.pathname)) return false;
    if (url.searchParams.get('durationMinutes') !== '30') return false;
    if (url.searchParams.get('days') !== '60') return false;

    const modalOpen = qs('#staffWorkflowModal:not(.hidden)');
    const dashboard = qs('#view-staff-dashboard.active');
    return Boolean(dashboard && !modalOpen);
  }

  function dateKey(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  function syntheticDashboardAvailability() {
    /* The dashboard has no service yet, therefore a 30-minute, 60-day server
       availability query is conceptually wrong and expensive. It only used
       those results as a yes/no date gate. Give that gate cheap candidate
       dates; actual cells are still constrained by working hours + existing
       appointments, and the real service duration is validated in the modal. */
    const start = new Date();
    start.setHours(12, 0, 0, 0);
    const days = Array.from({ length: 60 }, (_, index) => {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      return { date: dateKey(date), slots: ['00:00'] };
    });

    return new Response(JSON.stringify(days), {
      status: 200,
      headers: { 'Content-Type': 'application/json; charset=utf-8' }
    });
  }

  /* Install before legacy/api.js starts issuing requests. Besides the local
     dashboard shortcut, identical in-flight GETs are coalesced. This removes
     duplicate working-hours requests during the first staff dashboard load. */
  window.fetch = async function bublapiFetch(input, init = {}) {
    let url;
    try {
      const raw = input instanceof Request ? input.url : input;
      url = new URL(raw, location.origin);
    } catch {
      return nativeFetch(input, init);
    }

    const method = String(init?.method || (input instanceof Request ? input.method : 'GET')).toUpperCase();
    if (method !== 'GET' || url.origin !== location.origin || url.pathname === '/live/appointments') {
      return nativeFetch(input, init);
    }

    if (isDashboardAvailabilityProbe(url)) {
      return syntheticDashboardAvailability();
    }

    const key = requestKey(input, init, url);
    const ttl = cachedTtl(url);
    const cached = responseCache.get(key);
    if (cached && cached.expiresAt > Date.now()) {
      return cached.response.clone();
    }
    if (cached) responseCache.delete(key);

    const existing = requestInFlight.get(key);
    if (existing) {
      const response = await existing;
      return response.clone();
    }

    const request = nativeFetch(input, init)
      .then(response => {
        if (ttl && response.ok) {
          responseCache.set(key, {
            expiresAt: Date.now() + ttl,
            response: response.clone()
          });
        }
        return response;
      })
      .finally(() => requestInFlight.delete(key));

    requestInFlight.set(key, request);
    const response = await request;
    return response.clone();
  };

  function syncTopbarOffset() {
    const topbar = qs('.topbar');
    if (!topbar) return;
    const height = Math.ceil(topbar.getBoundingClientRect().height);
    if (height > 0) root.style.setProperty('--bublapi-topbar-height', `${height}px`);
  }

  function installStickyOffsetObserver() {
    const topbar = qs('.topbar');
    if (!topbar) return;
    syncTopbarOffset();

    if ('ResizeObserver' in window) {
      const observer = new ResizeObserver(syncTopbarOffset);
      observer.observe(topbar);
    } else {
      window.addEventListener('resize', syncTopbarOffset, { passive: true });
    }
  }

  function optimizeDoctorImage(image) {
    if (!(image instanceof HTMLImageElement) || !image.classList.contains('doctor-avatar')) return;
    image.decoding = 'async';
    image.loading = 'lazy';
    try { image.fetchPriority = 'low'; } catch {}
  }

  function installImageOptimizer() {
    qsa('img.doctor-avatar').forEach(optimizeDoctorImage);
    const observer = new MutationObserver(mutations => {
      for (const mutation of mutations) {
        mutation.addedNodes.forEach(node => {
          if (!(node instanceof Element)) return;
          if (node.matches?.('img.doctor-avatar')) optimizeDoctorImage(node);
          qsa('img.doctor-avatar', node).forEach(optimizeDoctorImage);
        });
      }
    });
    observer.observe(document.body, { childList: true, subtree: true });
  }

  function updateCalendarContext(note, warning = false) {
    const strip = qs('#staffAppointmentContext');
    if (!strip || !pendingCalendarStart) return;

    let marker = qs('[data-calendar-start-note]', strip);
    if (!marker) {
      marker = document.createElement('span');
      marker.dataset.calendarStartNote = 'true';
      strip.appendChild(marker);
    }

    marker.innerHTML = `<b>Начало</b>${pendingCalendarStart.time}<small class="staff-calendar-start-note${warning ? ' is-warning' : ''}">${note}</small>`;
    strip.classList.remove('hidden');
  }

  function invokeLegacyCreateAppointment(context) {
    const trigger = document.createElement('button');
    trigger.type = 'button';
    trigger.hidden = true;
    trigger.dataset.staffWorkflow = 'create-appointment';
    trigger.dataset.doctorId = context.doctorId;
    trigger.dataset.date = context.date;
    document.body.appendChild(trigger);
    trigger.click();
    trigger.remove();
  }

  function applyPendingCalendarStart() {
    const pending = pendingCalendarStart;
    if (!pending) return;
    const generation = ++pendingApplyGeneration;
    const startedAt = performance.now();

    const tick = () => {
      if (generation !== pendingApplyGeneration || pending !== pendingCalendarStart) return;

      const modal = qs('#staffWorkflowModal:not(.hidden)');
      const doctor = qs('#staffDoctorSelect');
      const service = qs('#staffServiceSelect');
      const date = qs('#staffAppointmentDate');
      const time = qs('#staffAppointmentTime');

      if (!modal || !doctor || !service || !date || !time) {
        if (performance.now() - startedAt < 4500) window.setTimeout(tick, 60);
        return;
      }

      updateCalendarContext('Длительность определится после выбора услуги.');

      if (!service.value || String(doctor.value) !== String(pending.doctorId)) {
        return;
      }

      /* While the legacy availability request is running both controls are
         disabled. Wait for it; then select the clicked date and let the legacy
         change handler populate service-duration-aware time options. */
      if (date.disabled) {
        if (performance.now() - startedAt < 4500) window.setTimeout(tick, 70);
        return;
      }

      date.value = pending.date;
      date.dispatchEvent(new Event('change', { bubbles: true }));

      window.setTimeout(() => {
        if (generation !== pendingApplyGeneration || pending !== pendingCalendarStart) return;
        const optionExists = [...time.options].some(option => option.value === pending.time);
        if (optionExists) {
          time.value = pending.time;
          time.dispatchEvent(new Event('change', { bubbles: true }));
          updateCalendarContext('Длительность рассчитана по выбранной услуге.');
        } else {
          updateCalendarContext('Для этой услуги выбранное начало недоступно — выберите ближайшее свободное время.', true);
        }
      }, 0);
    };

    tick();
  }

  function installCalendarClickMode() {
    const stopOldDragHandler = event => {
      const slot = event.target?.closest?.('#staffDashboardCalendar [data-calendar-slot]');
      if (!slot) return;
      event.stopImmediatePropagation();
      if (event.type === 'pointerdown') event.preventDefault();
    };

    document.addEventListener('pointerdown', stopOldDragHandler, true);
    document.addEventListener('pointerover', stopOldDragHandler, true);
    document.addEventListener('pointerup', stopOldDragHandler, true);

    document.addEventListener('click', event => {
      const slot = event.target?.closest?.('#staffDashboardCalendar [data-calendar-slot]');
      if (!slot || slot.disabled) return;

      event.preventDefault();
      event.stopImmediatePropagation();

      pendingCalendarStart = {
        doctorId: slot.dataset.doctorId,
        date: slot.dataset.date,
        time: slot.dataset.start
      };

      invokeLegacyCreateAppointment(pendingCalendarStart);
      window.setTimeout(applyPendingCalendarStart, 0);
    }, true);

    document.addEventListener('change', event => {
      if (!pendingCalendarStart) return;
      if (event.target?.matches?.('#staffServiceSelect, #staffDoctorSelect, #staffAppointmentForm [name="quantity"]')) {
        window.setTimeout(applyPendingCalendarStart, 0);
      }
    }, true);

    document.addEventListener('click', event => {
      if (event.target?.closest?.('[data-staff-workflow-close]')) {
        pendingCalendarStart = null;
        pendingApplyGeneration += 1;
      }
    }, true);

    /* Enhance the existing lightweight loading state with bubbles without
       changing the main renderer. */
    const observer = new MutationObserver(() => {
      const calendar = qs('#staffDashboardCalendar');
      if (!calendar) return;
      const empty = qs('.staff-calendar-list-empty', calendar);
      if (!empty || !/Загружаем расписание/i.test(empty.textContent || '')) return;
      if (qs('.staff-calendar-loading-bubbles', empty)) return;
      empty.insertAdjacentHTML(
        'beforeend',
        '<span class="staff-calendar-loading-bubbles" aria-hidden="true"><i></i><i></i><i></i></span>'
      );
    });

    const startObserver = () => {
      const calendar = qs('#staffDashboardCalendar');
      if (calendar) observer.observe(calendar, { childList: true, subtree: true });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', startObserver, { once: true });
    else startObserver();
  }

  function bootstrapDomFeatures() {
    installInitialRouteObserver();
    installStickyOffsetObserver();
    installImageOptimizer();
    installCalendarClickMode();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootstrapDomFeatures, { once: true });
  } else {
    bootstrapDomFeatures();
  }

  /* Start session resolution immediately; it runs in parallel with HTML/legacy
     module startup and no longer waits for /demo-config. */
  void resolveSavedSession();
})();

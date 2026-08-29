/*
 * Final targeted fixes for the Dent demo:
 * - deterministic "Панель сотрудника" entry / staff-only workspace navigation
 * - no past staff calendar dates or times
 * - reliable calendar click -> appointment time propagation
 * - working patient lookup by FIO / phone / email
 * - mobile Safari date field containment
 */
(() => {
  'use strict';

  const root = document.documentElement;
  const USER_TOKEN_KEY = 'bublapi.userToken';
  const ACCESS_PROFILE_KEY = 'bublapi.accessProfile';
  const qs = (selector, scope = document) => scope.querySelector(selector);
  const qsa = (selector, scope = document) => [...scope.querySelectorAll(selector)];

  let freshAccess = null;
  let staffPatients = null;
  let staffPatientsPromise = null;
  let pendingCalendarStart = null;
  let pendingApplyId = 0;
  let navSyncScheduled = false;

  function readStorage(key) {
    for (const storage of [localStorage, sessionStorage]) {
      try {
        const value = storage.getItem(key);
        if (value) return value;
      } catch {}
    }
    return '';
  }

  function readToken() {
    return readStorage(USER_TOKEN_KEY);
  }

  function authorizationHeaders() {
    const token = readToken();
    if (!token) return {};
    return {
      Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`
    };
  }

  function readCachedAccess() {
    try {
      const raw = readStorage(ACCESS_PROFILE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }

  async function probe(path) {
    const token = readToken();
    if (!token) return false;
    try {
      const response = await fetch(path, {
        method: 'GET',
        headers: authorizationHeaders(),
        cache: 'no-store'
      });
      return response.ok;
    } catch {
      return false;
    }
  }

  async function resolveFreshAccess() {
    const token = readToken();
    if (!token) {
      freshAccess = {};
      return freshAccess;
    }

    const [patient, staffCore, staffAppointments] = await Promise.all([
      probe('/api/notifications/unread-count'),
      probe('/api/patients'),
      probe('/api/appointments')
    ]);

    freshAccess = {
      patient,
      'staff-core': staffCore,
      'staff-appointments': staffAppointments
    };
    return freshAccess;
  }

  function currentAccess() {
    return freshAccess || readCachedAccess();
  }

  function hasStaffAccess() {
    const profile = currentAccess();
    return Boolean(profile?.['staff-core'] || profile?.['staff-appointments']);
  }

  function currentViewName() {
    return qs('.view.active')?.id?.replace(/^view-/, '') || '';
  }

  function isStaffMode() {
    return /^\/staff(?:\/|$)/.test(location.pathname) ||
      currentViewName().startsWith('staff-');
  }

  function workspaceGroup(sidebar) {
    return qsa(
      ':scope > .nav-group[data-access="staff-core,staff-appointments"]',
      sidebar
    ).find(group => !group.classList.contains('sidebar-context-entry')) || null;
  }

  function syncStaffNavigation() {
    const sidebar = qs('.sidebar');
    const entry = qs('.sidebar-context-entry');
    const exit = qs('.sidebar-context-exit');

    if (!sidebar || !entry || !exit) return;

    const workspace = workspaceGroup(sidebar);
    const allowed = hasStaffAccess();
    const staffMode = isStaffMode();

    document.body.dataset.sidebarMode = staffMode ? 'staff' : 'patient';

    if (staffMode) {
      entry.classList.add('hidden');
      exit.classList.toggle('hidden', !allowed);

      if (workspace) {
        // Do not manufacture permissions: only show the workspace for a user
        // whose fresh/cached access profile actually contains staff access.
        workspace.classList.toggle('hidden', !allowed);
      }
    } else {
      exit.classList.add('hidden');
      entry.classList.toggle('hidden', !allowed);

      // The detailed staff links must never leak into the patient/public menu.
      if (workspace) workspace.classList.add('hidden');
    }

    root.dataset.staffNavReady = 'true';
  }

  function scheduleStaffNavigationSync() {
    if (navSyncScheduled) return;
    navSyncScheduled = true;
    queueMicrotask(() => {
      navSyncScheduled = false;
      syncStaffNavigation();
    });
  }

  function localDateKey(date = new Date()) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  function minutesFromTime(value) {
    const [hours, minutes] = String(value || '').slice(0, 5).split(':').map(Number);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null;
    return hours * 60 + minutes;
  }

  function isPastDate(dateKey) {
    return Boolean(dateKey && dateKey < localDateKey());
  }

  function isPastDateTime(dateKey, timeValue) {
    if (!dateKey || !timeValue) return false;
    const today = localDateKey();
    if (dateKey < today) return true;
    if (dateKey > today) return false;

    const slotMinutes = minutesFromTime(timeValue);
    const now = new Date();
    const nowMinutes = now.getHours() * 60 + now.getMinutes();
    return slotMinutes != null && slotMinutes <= nowMinutes;
  }

  function clampDateInput(input) {
    if (!(input instanceof HTMLInputElement) || input.type !== 'date') return;
    const today = localDateKey();
    input.min = today;
    if (input.value && input.value < today) {
      input.value = '';
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }

  function prunePastTimeOptions(select, dateValue) {
    if (!(select instanceof HTMLSelectElement)) return;
    const today = localDateKey();
    if (dateValue !== today) return;

    const now = new Date();
    const nowMinutes = now.getHours() * 60 + now.getMinutes();

    [...select.options].forEach(option => {
      if (!option.value) return;
      const minutes = minutesFromTime(option.value);
      if (minutes != null && minutes <= nowMinutes) option.remove();
    });

    if (select.value && isPastDateTime(dateValue, select.value)) {
      select.value = '';
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }

  function enforceNoPastBookingValues() {
    const today = localDateKey();

    for (const prefix of ['staff', 'patient']) {
      const date = qs(`#${prefix}AppointmentDate`);
      const time = qs(`#${prefix}AppointmentTime`);

      if (date instanceof HTMLInputElement && date.type === 'date') {
        date.min = today;
        if (date.value && date.value < today) date.value = '';
      }

      if (time instanceof HTMLSelectElement) {
        prunePastTimeOptions(time, date?.value || '');
      }
    }

    const calendarDate = qs('#staffCalendarDate');
    if (calendarDate instanceof HTMLInputElement) {
      calendarDate.min = today;
      if (calendarDate.value && calendarDate.value < today) {
        calendarDate.value = today;
        calendarDate.dispatchEvent(new Event('change', { bubbles: true }));
      }
    }

    qsa('#staffDashboardCalendar [data-calendar-slot]').forEach(cell => {
      const past = isPastDateTime(cell.dataset.date, cell.dataset.start);
      cell.classList.toggle('hotfix-past-slot', past);
      if (past) {
        cell.disabled = true;
        cell.setAttribute('aria-disabled', 'true');
      }
    });

    qsa('#staffBookingDates [data-booking-date], #patientBookingDates [data-booking-date]')
      .forEach(button => {
        const past = isPastDate(button.dataset.bookingDate);
        button.classList.toggle('hotfix-past-date', past);
        if (past) button.disabled = true;
      });
  }

  function patientSearchText(patient) {
    return [
      patient.lastName,
      patient.firstName,
      patient.middleName,
      patient.email,
      patient.phone,
      patient.phoneNumber
    ].filter(Boolean).join(' ').toLocaleLowerCase('ru-RU');
  }

  function patientSearchDigits(patient) {
    return String(patient.phone ?? patient.phoneNumber ?? '').replace(/\D/g, '');
  }

  async function loadStaffPatientCache() {
    if (Array.isArray(staffPatients)) return staffPatients;
    if (staffPatientsPromise) return staffPatientsPromise;

    staffPatientsPromise = (async () => {
      const response = await fetch('/api/patients', {
        method: 'GET',
        headers: authorizationHeaders(),
        cache: 'no-store'
      });
      if (!response.ok) throw new Error(`Patients HTTP ${response.status}`);
      const data = await response.json();
      staffPatients = Array.isArray(data) ? data : [];
      return staffPatients;
    })().finally(() => {
      staffPatientsPromise = null;
    });

    return staffPatientsPromise;
  }

  function setPatientSelection(patient) {
    const search = qs('#staffPatientSearch');
    const id = qs('#staffPatientId');
    const suggestions = qs('#staffPatientSuggestions');
    if (!search || !id) return;

    id.value = patient?.id || '';
    search.value = patient
      ? [patient.lastName, patient.firstName, patient.middleName].filter(Boolean).join(' ')
      : '';

    id.dispatchEvent(new Event('input', { bubbles: true }));
    id.dispatchEvent(new Event('change', { bubbles: true }));
    suggestions?.classList.add('hidden');
  }

  async function renderPatientSuggestions(query) {
    const target = qs('#staffPatientSuggestions');
    if (!target) return;

    const normalized = String(query || '').trim().toLocaleLowerCase('ru-RU');
    const digits = normalized.replace(/\D/g, '');

    if (normalized.length < 2 && digits.length < 2) {
      target.classList.add('hidden');
      return;
    }

    try {
      const patients = await loadStaffPatientCache();
      const matches = patients.filter(patient => {
        const text = patientSearchText(patient);
        const phone = patientSearchDigits(patient);
        return text.includes(normalized) || (digits && phone.includes(digits));
      }).slice(0, 10);

      target.innerHTML = matches.length
        ? matches.map(patient => {
            const name = [patient.lastName, patient.firstName, patient.middleName].filter(Boolean).join(' ') || 'Без имени';
            const contact = patient.phone || patient.phoneNumber || patient.email || '';
            return `<button type="button" data-hotfix-patient-pick="${String(patient.id).replace(/"/g, '&quot;')}"><strong>${escapeHtml(name)}</strong><small>${escapeHtml(contact)}</small></button>`;
          }).join('')
        : '<div class="staff-patient-no-result">Ничего не найдено</div>';
      target.classList.remove('hidden');
    } catch {
      target.innerHTML = '<div class="staff-patient-no-result">Не удалось загрузить пациентов</div>';
      target.classList.remove('hidden');
    }
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function noteCalendarStart(message, warning = false) {
    const strip = qs('#staffAppointmentContext');
    if (!strip || !pendingCalendarStart) return;

    let marker = qs('[data-hotfix-calendar-note]', strip);
    if (!marker) {
      marker = document.createElement('span');
      marker.dataset.hotfixCalendarNote = 'true';
      strip.appendChild(marker);
    }

    marker.innerHTML = `<b>Начало</b>${escapeHtml(pendingCalendarStart.time)}<small class="staff-calendar-start-note${warning ? ' is-warning' : ''}">${escapeHtml(message)}</small>`;
    strip.classList.remove('hidden');
  }

  function applyPendingCalendarStart() {
    const pending = pendingCalendarStart;
    if (!pending) return;

    const runId = ++pendingApplyId;
    const startedAt = performance.now();

    const tick = () => {
      if (runId !== pendingApplyId || pending !== pendingCalendarStart) return;
      if (performance.now() - startedAt > 9000) return;

      const modal = qs('#staffWorkflowModal:not(.hidden)');
      const doctor = qs('#staffDoctorSelect');
      const service = qs('#staffServiceSelect');
      const date = qs('#staffAppointmentDate');
      const time = qs('#staffAppointmentTime');

      if (!modal || !doctor || !service || !date || !time) {
        setTimeout(tick, 60);
        return;
      }

      clampDateInput(date);

      if (isPastDateTime(pending.date, pending.time)) {
        noteCalendarStart('Это время уже прошло. Выберите актуальное свободное окно.', true);
        pendingCalendarStart = null;
        return;
      }

      // Doctor may be populated asynchronously by the legacy workflow.
      if (String(doctor.value) !== String(pending.doctorId)) {
        const option = [...doctor.options].find(item => String(item.value) === String(pending.doctorId));
        if (option) {
          doctor.value = String(pending.doctorId);
          doctor.dispatchEvent(new Event('change', { bubbles: true }));
        }
        setTimeout(tick, 80);
        return;
      }

      if (!service.value) {
        noteCalendarStart('Выберите услугу — её длительность определит доступность выбранного начала.');
        setTimeout(tick, 100);
        return;
      }

      // Availability request disables date/time while it is in flight.
      if (date.disabled) {
        setTimeout(tick, 80);
        return;
      }

      if (date.value !== pending.date) {
        date.value = pending.date;
        date.dispatchEvent(new Event('change', { bubbles: true }));
        setTimeout(tick, 80);
        return;
      }

      prunePastTimeOptions(time, date.value);

      const optionExists = [...time.options].some(option => option.value === pending.time);
      if (!optionExists || time.disabled) {
        // Options can be rebuilt immediately after date change; keep observing
        // until the availability result stabilises.
        setTimeout(tick, 80);
        return;
      }

      if (time.value !== pending.time) {
        time.value = pending.time;
        time.dispatchEvent(new Event('change', { bubbles: true }));
      }

      noteCalendarStart('Выбранное время подставлено. Длительность рассчитана по услуге.');
    };

    tick();
  }

  function installCalendarSelectionGuard() {
    document.addEventListener('click', event => {
      const slot = event.target.closest?.('#staffDashboardCalendar [data-calendar-slot]');
      if (!slot) return;

      if (isPastDateTime(slot.dataset.date, slot.dataset.start)) {
        event.preventDefault();
        event.stopImmediatePropagation();
        return;
      }

      pendingCalendarStart = {
        doctorId: slot.dataset.doctorId,
        date: slot.dataset.date,
        time: slot.dataset.start
      };

      setTimeout(applyPendingCalendarStart, 0);
    }, true);

    document.addEventListener('change', event => {
      if (event.target?.matches?.(
        '#staffServiceSelect,#staffDoctorSelect,#staffAppointmentForm [name="quantity"],#staffAppointmentDate'
      )) {
        enforceNoPastBookingValues();
        if (pendingCalendarStart) setTimeout(applyPendingCalendarStart, 0);
      }

      if (event.target?.matches?.('#staffAppointmentTime,#patientAppointmentTime')) {
        enforceNoPastBookingValues();
      }
    }, true);

    document.addEventListener('click', event => {
      if (event.target.closest?.('[data-staff-workflow-close]')) {
        pendingCalendarStart = null;
        pendingApplyId += 1;
      }

      const prev = event.target.closest?.('#staffCalendarPrev');
      if (prev) {
        const input = qs('#staffCalendarDate');
        if (input?.value && input.value <= localDateKey()) {
          event.preventDefault();
          event.stopImmediatePropagation();
        }
      }
    }, true);
  }

  function installPatientSearchFix() {
    document.addEventListener('input', event => {
      if (!event.target?.matches?.('#staffPatientSearch')) return;

      // The legacy listener searches a cache that is not populated by the
      // dashboard loader. Stop that listener and use the real /api/patients
      // response instead.
      event.stopImmediatePropagation();

      const id = qs('#staffPatientId');
      if (id) id.value = '';
      void renderPatientSuggestions(event.target.value);
    }, true);

    document.addEventListener('click', event => {
      const button = event.target.closest?.('[data-hotfix-patient-pick]');
      if (!button) return;

      event.preventDefault();
      event.stopImmediatePropagation();

      const id = button.dataset.hotfixPatientPick;
      const patient = (staffPatients || []).find(item => String(item.id) === String(id));
      if (patient) setPatientSelection(patient);
    }, true);

    // Preload patient cache as soon as the appointment workflow opens.
    document.addEventListener('click', event => {
      if (event.target.closest?.('[data-staff-workflow="create-appointment"],[data-calendar-slot]')) {
        void loadStaffPatientCache().catch(() => {});
      }
    }, true);
  }

  function installDomObservers() {
    const observer = new MutationObserver(() => {
      scheduleStaffNavigationSync();
      enforceNoPastBookingValues();
      if (pendingCalendarStart) applyPendingCalendarStart();
    });

    observer.observe(document.body, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ['class', 'disabled', 'value']
    });

    window.setInterval(enforceNoPastBookingValues, 30_000);
  }

  async function bootstrap() {
    enforceNoPastBookingValues();
    installCalendarSelectionGuard();
    installPatientSearchFix();
    installDomObservers();

    // Keep the sidebar hidden from all staff-specific items until a fresh
    // permission result is known, then reveal exactly one context.
    await resolveFreshAccess();
    syncStaffNavigation();

    window.addEventListener('popstate', scheduleStaffNavigationSync);
    document.addEventListener('click', event => {
      if (event.target.closest?.('.nav-button,[data-go],#sidebarEnterStaff,#sidebarExitStaff')) {
        setTimeout(syncStaffNavigation, 0);
      }
    }, true);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootstrap, { once: true });
  } else {
    void bootstrap();
  }
})();

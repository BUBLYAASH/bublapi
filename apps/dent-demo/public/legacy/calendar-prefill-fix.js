/*
 * Calendar UX complement to final-fixes.js.
 *
 * final-fixes.js owns:
 * - patient search by FIO / phone / email
 * - past date/time guards
 * - final verification of a selected start time against service duration
 *
 * This file makes the clicked calendar date/time visible in the modal
 * immediately, before a service is selected.
 */
(() => {
  'use strict';

  const qs = (selector, scope = document) => scope.querySelector(selector);

  function localDateKey(date = new Date()) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  function minutes(value) {
    const [h, m] = String(value || '').slice(0, 5).split(':').map(Number);
    return Number.isFinite(h) && Number.isFinite(m) ? h * 60 + m : null;
  }

  function isPast(date, time) {
    const today = localDateKey();
    if (!date) return false;
    if (date < today) return true;
    if (date > today || !time) return false;

    const value = minutes(time);
    const now = new Date();
    return value != null && value <= now.getHours() * 60 + now.getMinutes();
  }

  function setTemporaryTimeOption(select, value) {
    if (!(select instanceof HTMLSelectElement) || !value) return;

    let option = [...select.options].find(item => item.value === value);
    if (!option) {
      option = document.createElement('option');
      option.value = value;
      option.textContent = value;
      option.dataset.calendarPrefill = 'true';
      select.append(option);
    }

    select.value = value;
  }

  function prefill(slot) {
    const dateValue = slot.dataset.date || '';
    const timeValue = slot.dataset.start || '';

    if (isPast(dateValue, timeValue)) return;

    const startedAt = performance.now();

    const tick = () => {
      if (performance.now() - startedAt > 5000) return;

      const modal = qs('#staffWorkflowModal:not(.hidden)');
      const date = qs('#staffAppointmentDate');
      const time = qs('#staffAppointmentTime');

      if (!modal || !date || !time) {
        setTimeout(tick, 40);
        return;
      }

      date.min = localDateKey();
      date.value = dateValue;

      // The legacy form keeps the controls disabled until service availability
      // is known. A selected option is still rendered by browsers while the
      // control is disabled, so the employee sees the chosen start at once.
      setTemporaryTimeOption(time, timeValue);

      date.dataset.calendarPrefilled = 'true';
      time.dataset.calendarPrefilled = 'true';
    };

    tick();
  }

  // Run before the legacy bubbling handler opens the workflow. We only remember
  // and mirror the value; final-fixes.js still performs the authoritative check.
  document.addEventListener('click', event => {
    const slot = event.target.closest?.('#staffDashboardCalendar [data-calendar-slot]');
    if (!slot || slot.disabled) return;

    if (isPast(slot.dataset.date, slot.dataset.start)) {
      event.preventDefault();
      event.stopImmediatePropagation();
      return;
    }

    prefill(slot);
  }, true);

  // Native date inputs must never allow an earlier calendar day even if a
  // browser ignores dynamically rendered slot attributes.
  document.addEventListener('focusin', event => {
    if (event.target?.matches?.('#staffAppointmentDate,#patientAppointmentDate,#staffCalendarDate')) {
      event.target.min = localDateKey();
    }
  }, true);
})();

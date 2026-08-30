/*
 * Calendar UX complement to final-fixes.js.
 *
 * final-fixes.js owns:
 * - patient search by FIO / phone / email
 * - past date/time guards
 * - final verification of a selected start time against service duration
 *
 * This file mirrors the clicked calendar date/time into the modal immediately,
 * before a service is selected. Legacy code may rebuild/reset those controls
 * while the modal opens, so the clicked values are re-applied for a short time
 * until the user selects a service. After that final-fixes.js performs the
 * authoritative service-duration-aware availability check.
 */
(() => {
  'use strict';

  const qs = (selector, scope = document) => scope.querySelector(selector);
  let prefillGeneration = 0;

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

    if (select.value !== value) select.value = value;
  }

  function mirrorClickedStart(dateValue, timeValue) {
    const date = qs('#staffAppointmentDate');
    const time = qs('#staffAppointmentTime');
    if (!date || !time) return false;

    date.min = localDateKey();
    if (date.value !== dateValue) date.value = dateValue;
    setTemporaryTimeOption(time, timeValue);

    date.dataset.calendarPrefilled = 'true';
    time.dataset.calendarPrefilled = 'true';
    return true;
  }

  function prefill(slot) {
    const dateValue = slot.dataset.date || '';
    const timeValue = slot.dataset.start || '';
    if (isPast(dateValue, timeValue)) return;

    const generation = ++prefillGeneration;
    const startedAt = performance.now();

    const tick = () => {
      if (generation !== prefillGeneration) return;
      if (performance.now() - startedAt > 8000) return;

      const modal = qs('#staffWorkflowModal:not(.hidden)');
      if (!modal) {
        setTimeout(tick, 40);
        return;
      }

      const service = qs('#staffServiceSelect');

      // Once a service is selected, stop forcing the temporary value. The
      // service-duration-aware logic in final-fixes.js now owns date/time.
      if (service?.value) return;

      // The legacy workflow can reset/rebuild these fields several times while
      // opening the modal. Keep the clicked start visible until service choice.
      mirrorClickedStart(dateValue, timeValue);
      setTimeout(tick, 80);
    };

    tick();
  }

  // Run before the legacy bubbling handler opens the workflow.
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

  // Cancel an old mirror loop when the workflow closes.
  document.addEventListener('click', event => {
    if (event.target.closest?.('[data-staff-workflow-close]')) {
      prefillGeneration += 1;
    }
  }, true);

  // Native date inputs must never allow an earlier calendar day even if a
  // browser ignores dynamically rendered slot attributes.
  document.addEventListener('focusin', event => {
    if (event.target?.matches?.('#staffAppointmentDate,#patientAppointmentDate,#staffCalendarDate')) {
      event.target.min = localDateKey();
    }
  }, true);
})();

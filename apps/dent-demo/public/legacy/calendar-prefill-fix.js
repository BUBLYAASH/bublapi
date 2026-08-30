/*
 * Staff calendar -> appointment form prefill.
 *
 * The legacy appointment workflow normally calculates availability only after
 * both doctor and service are selected. A calendar click is different: the
 * employee has already chosen a concrete doctor/date/start time, so those
 * values must be visible immediately and must survive the async service /
 * availability rebuilds.
 *
 * This complement deliberately does NOT own availability calculation. It:
 *  1. captures the real pointer selection before the legacy pointerup handler;
 *  2. mirrors doctor/date/time while the workflow is opening;
 *  3. filters services by the clicked doctor once;
 *  4. seeds the clicked date/time back immediately after service change, before
 *     loadAppointmentAvailability() snapshots previous values;
 *  5. after availability finishes, asks the existing date-change handler to
 *     rebuild times for the clicked date and keeps the clicked start only when
 *     it is valid for the selected service duration.
 */
(() => {
  'use strict';

  const qs = (selector, scope = document) => scope.querySelector(selector);

  let drag = null;
  let calendarDraft = null;
  let draftGeneration = 0;
  let doctorFilterStarted = false;
  let internalChangeDepth = 0;

  function localDateKey(date = new Date()) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  function minutesFromTime(value) {
    const [hours, minutes] = String(value || '').slice(0, 5).split(':').map(Number);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null;
    return hours * 60 + minutes;
  }

  function timeFromMinutes(total) {
    const value = Math.max(0, Math.min(24 * 60 - 1, Number(total) || 0));
    const hours = Math.floor(value / 60);
    const minutes = value % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  }

  function isPast(dateValue, timeValue) {
    if (!dateValue) return false;

    const today = localDateKey();
    if (dateValue < today) return true;
    if (dateValue > today || !timeValue) return false;

    const slotMinutes = minutesFromTime(timeValue);
    const now = new Date();
    const nowMinutes = now.getHours() * 60 + now.getMinutes();
    return slotMinutes != null && slotMinutes <= nowMinutes;
  }

  function workflowElements() {
    return {
      modal: qs('#staffWorkflowModal:not(.hidden)'),
      form: qs('#staffAppointmentForm'),
      doctor: qs('#staffDoctorSelect'),
      service: qs('#staffServiceSelect'),
      date: qs('#staffAppointmentDate'),
      time: qs('#staffAppointmentTime'),
      quickDates: qs('#staffBookingDates')
    };
  }

  function withInternalChange(callback) {
    internalChangeDepth += 1;
    try {
      callback();
    } finally {
      internalChangeDepth -= 1;
    }
  }

  function dispatchInternalChange(element) {
    if (!element) return;
    withInternalChange(() => {
      element.dispatchEvent(new Event('change', { bubbles: true }));
    });
  }

  function ensureTemporaryTimeOption(select, value) {
    if (!(select instanceof HTMLSelectElement) || !value) return false;

    let option = [...select.options].find(item => item.value === value);
    if (!option) {
      option = document.createElement('option');
      option.value = value;
      option.textContent = value;
      option.dataset.calendarPrefill = 'true';
      select.append(option);
    }

    select.value = value;
    return true;
  }

  function selectDraftDoctor(doctor, { dispatch = false } = {}) {
    if (!(doctor instanceof HTMLSelectElement) || !calendarDraft?.doctorId) {
      return false;
    }

    const option = [...doctor.options].find(
      item => String(item.value) === String(calendarDraft.doctorId)
    );
    if (!option) return false;

    const changed = String(doctor.value) !== String(calendarDraft.doctorId);
    if (changed) doctor.value = String(calendarDraft.doctorId);

    if (dispatch) {
      dispatchInternalChange(doctor);
    }

    return true;
  }

  function mirrorDraftImmediately() {
    if (!calendarDraft) return false;

    const { modal, doctor, date, time } = workflowElements();
    if (!modal || !doctor || !date || !time) return false;

    selectDraftDoctor(doctor);

    date.min = localDateKey();
    date.value = calendarDraft.date;
    date.dataset.calendarPrefilled = 'true';

    ensureTemporaryTimeOption(time, calendarDraft.time);
    time.dataset.calendarPrefilled = 'true';
    doctor.dataset.calendarPrefilled = 'true';

    return true;
  }

  function noteDraft(message, warning = false) {
    const strip = qs('#staffAppointmentContext');
    if (!strip || !calendarDraft) return;

    let marker = qs('[data-calendar-prefill-note]', strip);
    if (!marker) {
      marker = document.createElement('span');
      marker.dataset.calendarPrefillNote = 'true';
      strip.append(marker);
    }

    marker.innerHTML =
      `<b>Выбранное начало</b>${escapeHtml(calendarDraft.time)}` +
      `<small class="staff-calendar-start-note${warning ? ' is-warning' : ''}">${escapeHtml(message)}</small>`;
    strip.classList.remove('hidden');
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function beginDraftSync() {
    const generation = ++draftGeneration;
    const startedAt = performance.now();

    const tick = () => {
      if (generation !== draftGeneration || !calendarDraft) return;
      if (performance.now() - startedAt > 12_000) return;

      const { modal, doctor, service } = workflowElements();
      if (!modal || !doctor || !service) {
        setTimeout(tick, 40);
        return;
      }

      /*
       * The legacy openStaffWorkflow() awaits loadStaffAppointments(), then
       * resets the form and calls loadAppointmentAvailability('staff') with
       * only doctor selected. That call clears date/time again. Re-mirroring
       * here after each async rebuild is intentional.
       */
      mirrorDraftImmediately();

      /*
       * Trigger the existing doctor-change workflow exactly once. That filters
       * the service select to services actually assigned to this doctor.
       * Its own resetAvailabilityControls() may clear date/time; the loop puts
       * them straight back without triggering date availability prematurely.
       */
      if (!doctorFilterStarted && selectDraftDoctor(doctor)) {
        doctorFilterStarted = true;
        dispatchInternalChange(doctor);
        setTimeout(tick, 60);
        return;
      }

      /*
       * Before a service is chosen, doctor/date/time are display context from
       * the calendar. Once a service exists, its change handler owns the async
       * availability request; validateDraftAfterService() will finish the job.
       */
      if (!service.value) {
        setTimeout(tick, 70);
      }
    };

    tick();
  }

  function availabilityLooksSettled(elements) {
    const { date, quickDates } = elements;
    if (!date) return false;

    if (!date.disabled) return true;

    const text = String(quickDates?.textContent || '').trim().toLowerCase();
    return (
      text.includes('нет доступных дат') ||
      text.includes('не удалось загрузить') ||
      text.includes('нет свобод') ||
      text.includes('расписание')
    ) && !text.includes('загрузка');
  }

  function validateDraftAfterService(serviceValue, generation) {
    const startedAt = performance.now();

    const tick = () => {
      if (
        generation !== draftGeneration ||
        !calendarDraft ||
        performance.now() - startedAt > 12_000
      ) {
        return;
      }

      const elements = workflowElements();
      const { modal, doctor, service, date, time } = elements;
      if (!modal || !doctor || !service || !date || !time) return;

      // The user changed the service again while this validation was waiting.
      if (String(service.value) !== String(serviceValue)) return;

      /*
       * filterDoctorsByService() rebuilds the doctor <select>. Because the
       * service list was already filtered by the clicked doctor, the doctor
       * should still be present. Re-select it without firing another change,
       * otherwise we'd start a second filtering/availability cycle.
       */
      const doctorExists = selectDraftDoctor(doctor);
      if (!doctorExists) {
        noteDraft('Эта услуга недоступна у выбранного врача. Выберите другую услугу.', true);
        return;
      }

      if (!availabilityLooksSettled(elements)) {
        setTimeout(tick, 70);
        return;
      }

      /*
       * The legacy loader may have selected the first available date because
       * its earlier reset erased the calendar date. Put the clicked date back
       * and dispatch the NORMAL date change: app.js will synchronously rebuild
       * the time options from its freshly loaded appointmentAvailability map.
       */
      date.value = calendarDraft.date;
      dispatchInternalChange(date);

      const exactTimeExists = [...time.options].some(
        option => option.value === calendarDraft.time
      );

      if (exactTimeExists && !time.disabled) {
        time.value = calendarDraft.time;
        dispatchInternalChange(time);
        noteDraft('Врач, дата и время сохранены. Осталось выбрать пациента и заполнить запись.');
      } else {
        time.value = '';
        noteDraft(
          `Начало ${calendarDraft.time} не подходит для выбранной услуги по длительности. Выберите другое свободное время.`,
          true
        );
      }
    };

    // Give the existing async service-change handler a chance to enter loading.
    setTimeout(tick, 40);
  }

  function startCalendarDraft({ doctorId, date, time }) {
    if (!doctorId || !date || !time || isPast(date, time)) return;

    calendarDraft = {
      doctorId: String(doctorId),
      date: String(date),
      time: String(time).slice(0, 5)
    };
    doctorFilterStarted = false;
    beginDraftSync();
  }

  function clearCalendarDraft() {
    calendarDraft = null;
    drag = null;
    doctorFilterStarted = false;
    draftGeneration += 1;
  }

  /*
   * Capture the real drag/click selection. The legacy calendar calls
   * preventDefault() on pointerdown and opens the workflow on window pointerup,
   * so relying on a later "click" event is unreliable (and was the cause of
   * the previous broken version).
   */
  document.addEventListener('pointerdown', event => {
    const slot = event.target.closest?.('#staffDashboardCalendar [data-calendar-slot]');
    if (!slot || slot.disabled) return;

    const minute = Number(
      slot.dataset.minute ?? minutesFromTime(slot.dataset.start)
    );
    if (!Number.isFinite(minute)) return;

    drag = {
      pointerId: event.pointerId,
      doctorId: slot.dataset.doctorId,
      date: slot.dataset.date,
      startMinute: minute,
      currentMinute: minute
    };
  }, true);

  document.addEventListener('pointerover', event => {
    if (!drag) return;

    const slot = event.target.closest?.('#staffDashboardCalendar [data-calendar-slot]');
    if (
      !slot ||
      slot.disabled ||
      String(slot.dataset.doctorId) !== String(drag.doctorId)
    ) {
      return;
    }

    const minute = Number(
      slot.dataset.minute ?? minutesFromTime(slot.dataset.start)
    );
    if (Number.isFinite(minute)) drag.currentMinute = minute;
  }, true);

  window.addEventListener('pointerup', event => {
    if (!drag || drag.pointerId !== event.pointerId) return;

    const selected = drag;
    drag = null;

    const startMinute = Math.min(selected.startMinute, selected.currentMinute);
    startCalendarDraft({
      doctorId: selected.doctorId,
      date: selected.date,
      time: timeFromMinutes(startMinute)
    });
  }, true);

  window.addEventListener('pointercancel', event => {
    if (drag?.pointerId === event.pointerId) drag = null;
  }, true);

  /*
   * Crucial race fix:
   * app.js service-change listener synchronously calls resetAvailabilityControls
   * and then awaits filterDoctorsByService(). Queueing this mirror from capture
   * phase means it runs AFTER that synchronous reset but BEFORE
   * loadAppointmentAvailability() snapshots previousSelectedDate/Time.
   */
  document.addEventListener('change', event => {
    if (!calendarDraft) return;

    const target = event.target;

    if (target?.matches?.('#staffServiceSelect')) {
      const serviceValue = target.value;
      if (!serviceValue) {
        mirrorDraftImmediately();
        return;
      }

      const generation = draftGeneration;

      queueMicrotask(() => {
        if (
          generation !== draftGeneration ||
          !calendarDraft ||
          String(qs('#staffServiceSelect')?.value || '') !== String(serviceValue)
        ) {
          return;
        }

        // Seed exactly the values the legacy availability loader should retain.
        mirrorDraftImmediately();
      });

      validateDraftAfterService(serviceValue, generation);
      return;
    }

    if (internalChangeDepth > 0) return;

    // A deliberate manual change means the employee no longer wants the
    // original calendar context to be forced back.
    if (target?.matches?.('#staffDoctorSelect,#staffAppointmentDate,#staffAppointmentTime')) {
      clearCalendarDraft();
    }
  }, true);

  document.addEventListener('click', event => {
    if (event.target.closest?.('[data-staff-workflow-close]')) {
      clearCalendarDraft();
    }
  }, true);

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') clearCalendarDraft();
  }, true);

  // Keep native date controls from ever exposing past days.
  document.addEventListener('focusin', event => {
    if (event.target?.matches?.(
      '#staffAppointmentDate,#patientAppointmentDate,#staffCalendarDate'
    )) {
      event.target.min = localDateKey();
    }
  }, true);
})();

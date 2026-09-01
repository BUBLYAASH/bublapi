import { api, escapeHtml, toast } from './api.js';

(() => {
  'use strict';

  const $ = (selector, scope = document) => scope.querySelector(selector);
  const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

  const DAY_NAMES = {
    MONDAY: 'ПОНЕДЕЛЬНИК',
    TUESDAY: 'ВТОРНИК',
    WEDNESDAY: 'СРЕДА',
    THURSDAY: 'ЧЕТВЕРГ',
    FRIDAY: 'ПЯТНИЦА',
    SATURDAY: 'СУББОТА',
    SUNDAY: 'ВОСКРЕСЕНЬЕ'
  };
  const DAY_ORDER = Object.keys(DAY_NAMES);

  function time24(value) {
    return value ? String(value).slice(0, 5) : '—';
  }

  function currentScheduleDoctorId() {
    return (
      $('#doctorScheduleForm [name="doctorId"]')?.value ||
      $('#doctorScheduleList [data-doctor-id]')?.dataset.doctorId ||
      ''
    );
  }

  function renderSchedule(hours, doctorId) {
    const target = $('#doctorScheduleList');
    if (!target || !doctorId) return;

    const normalized = Array.isArray(hours) ? hours : [];
    const byDay = new Map(DAY_ORDER.map(day => [day, []]));

    normalized.forEach(interval => {
      if (!byDay.has(interval.dayOfWeek)) byDay.set(interval.dayOfWeek, []);
      byDay.get(interval.dayOfWeek).push(interval);
    });

    target.innerHTML = `
      <div class="working-hours-list">
        ${DAY_ORDER.map(day => {
          const intervals = (byDay.get(day) || [])
            .slice()
            .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)));

          const markup = intervals.length
            ? intervals.map(interval => `
                <div class="working-hours-interval">
                  <span>${escapeHtml(time24(interval.startTime))}–${escapeHtml(time24(interval.endTime))}</span>
                  <span class="actions">
                    <button
                      class="btn btn-primary btn-sm schedule-edit"
                      type="button"
                      data-doctor-id="${escapeHtml(doctorId)}"
                      data-schedule-id="${escapeHtml(interval.id)}"
                      data-start-time="${escapeHtml(time24(interval.startTime))}"
                      data-end-time="${escapeHtml(time24(interval.endTime))}">
                      Изменить
                    </button>
                    <button
                      class="btn btn-danger btn-sm schedule-delete"
                      type="button"
                      data-doctor-id="${escapeHtml(doctorId)}"
                      data-schedule-id="${escapeHtml(interval.id)}">
                      Удалить
                    </button>
                  </span>
                </div>
              `).join('')
            : '<span class="muted">Выходной</span>';

          return `
            <div class="working-hours-day-row">
              <div class="working-hours-day-name">${escapeHtml(DAY_NAMES[day])}</div>
              <div class="working-hours-intervals">${markup}</div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  async function refreshSchedule(doctorId = currentScheduleDoctorId()) {
    if (!doctorId || !$('#doctorScheduleList')) return;

    try {
      const separator = `/api/public/doctors/${doctorId}/working-hours`.includes('?') ? '&' : '?';
      const hours = await api(
        `/api/public/doctors/${doctorId}/working-hours${separator}_=${Date.now()}`,
        { method: 'GET', cache: 'no-store' }
      );
      renderSchedule(hours, doctorId);
    } catch {
      // The legacy handler already shows its own error. Do not duplicate it.
    }
  }

  function scheduleRefreshSoon(doctorId) {
    setTimeout(() => refreshSchedule(doctorId), 250);
    setTimeout(() => refreshSchedule(doctorId), 900);
  }

  // Fix stale working-hours UI after create/update/delete without Cmd+R.
  document.addEventListener('click', event => {
    const save = event.target.closest?.('.schedule-save-modal');
    if (save) {
      scheduleRefreshSoon(save.dataset.doctorId);
      return;
    }

    const remove = event.target.closest?.('.schedule-delete');
    if (remove) {
      scheduleRefreshSoon(remove.dataset.doctorId);
    }
  }, true);

  document.addEventListener('submit', event => {
    if (!event.target.matches?.('#doctorScheduleForm')) return;
    const doctorId = event.target.querySelector('[name="doctorId"]')?.value;
    scheduleRefreshSoon(doctorId);
  }, true);

  // Fix nested schedule edit modal appearing below the already opened workflow modal.
  function fixModalStacking() {
    const editForm = $('#modalScheduleEditForm');
    const editModal = editForm?.closest('.modal');
    if (!editModal) return;

    $$('.modal:not(.hidden)').forEach(modal => {
      if (modal === editModal) {
        modal.style.zIndex = '10050';
      } else if (!modal.style.zIndex || Number(modal.style.zIndex) >= 10050) {
        modal.style.zIndex = '10000';
      }
    });

    const backdrop = $('.modal-backdrop', editModal);
    if (backdrop) backdrop.style.zIndex = '0';

    const dialog = $('.modal-dialog', editModal);
    if (dialog) {
      dialog.style.position = 'relative';
      dialog.style.zIndex = '1';
    }
  }

  new MutationObserver(fixModalStacking).observe(document.documentElement, {
    subtree: true,
    childList: true,
    attributes: true,
    attributeFilter: ['class']
  });

  document.addEventListener('click', event => {
    if (event.target.closest?.('.schedule-edit')) {
      requestAnimationFrame(fixModalStacking);
      setTimeout(fixModalStacking, 0);
    }
  }, true);

  // "Выходные и исключения" for a doctor's schedule.
  let sessionExceptions = [];

  function renderSessionExceptions() {
    const target = $('#doctorScheduleExceptionSessionList');
    if (!target) return;

    if (!sessionExceptions.length) {
      target.innerHTML = '<p class="muted" style="margin:0">Добавленные в этой сессии исключения появятся здесь.</p>';
      return;
    }

    target.innerHTML = sessionExceptions.map(item => {
      const typeLabel = item.type === 'DAY_OFF' ? 'Выходной' : 'Особые часы';
      const hours = item.type === 'CUSTOM_WORKING_HOURS'
        ? ` · ${escapeHtml(time24(item.startTime))}–${escapeHtml(time24(item.endTime))}`
        : '';
      const reason = item.reason ? ` · ${escapeHtml(item.reason)}` : '';

      return `
        <div class="working-hours-day-row">
          <div class="working-hours-day-name">${escapeHtml(item.date)}</div>
          <div class="working-hours-intervals">
            <span>${typeLabel}${hours}${reason}</span>
            <button
              class="btn btn-danger btn-sm"
              type="button"
              data-delete-schedule-exception
              data-doctor-id="${escapeHtml(item.doctorId)}"
              data-exception-id="${escapeHtml(item.id)}">
              Удалить
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  function syncExceptionDoctor() {
    const regularDoctor = $('#doctorScheduleForm [name="doctorId"]');
    const hiddenDoctor = $('#doctorScheduleExceptionForm [name="doctorId"]');
    if (hiddenDoctor && regularDoctor) hiddenDoctor.value = regularDoctor.value || '';
  }

  function toggleExceptionTimes() {
    const form = $('#doctorScheduleExceptionForm');
    if (!form) return;

    const type = form.querySelector('[name="type"]')?.value;
    const times = $('#doctorScheduleExceptionTimes');
    const start = form.querySelector('[name="startTime"]');
    const end = form.querySelector('[name="endTime"]');
    const custom = type === 'CUSTOM_WORKING_HOURS';

    times?.classList.toggle('hidden', !custom);
    if (start) start.required = custom;
    if (end) end.required = custom;
  }

  function installExceptionPanel() {
    const scheduleForm = $('#doctorScheduleForm');
    if (!scheduleForm || $('#doctorScheduleExceptionForm')) return;

    const card = document.createElement('form');
    card.id = 'doctorScheduleExceptionForm';
    card.className = 'card workflow-card';
    card.style.marginTop = '16px';
    card.innerHTML = `
      <h3>Выходные и исключения</h3>
      <p class="muted">
        Задайте полный выходной или особые часы работы врача на конкретную дату.
      </p>
      <input name="doctorId" type="hidden">
      <div class="form-grid">
        <label>
          Дата
          <input name="date" type="date" required>
        </label>
        <label>
          Тип
          <select name="type" required>
            <option value="DAY_OFF">Выходной день</option>
            <option value="CUSTOM_WORKING_HOURS">Особые часы работы</option>
          </select>
        </label>
        <div class="full form-grid hidden" id="doctorScheduleExceptionTimes">
          <label>
            Начало
            <input name="startTime" type="time">
          </label>
          <label>
            Окончание
            <input name="endTime" type="time">
          </label>
        </div>
        <label class="full">
          Причина / комментарий
          <input name="reason" maxlength="255" placeholder="Например: отпуск">
        </label>
      </div>
      <div class="workflow-footer" style="margin-top:16px">
        <button class="btn btn-primary" type="submit">Добавить исключение</button>
      </div>
      <div id="doctorScheduleExceptionSessionList" style="margin-top:16px"></div>
    `;

    scheduleForm.insertAdjacentElement('afterend', card);
    syncExceptionDoctor();
    toggleExceptionTimes();
    renderSessionExceptions();

    const date = card.querySelector('[name="date"]');
    if (date) {
      const now = new Date();
      const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
        .toISOString()
        .slice(0, 10);
      date.min = local;
    }
  }

  document.addEventListener('change', event => {
    if (event.target.matches?.('#doctorScheduleForm [name="doctorId"]')) {
      syncExceptionDoctor();
      sessionExceptions = [];
      renderSessionExceptions();
    }

    if (event.target.matches?.('#doctorScheduleExceptionForm [name="type"]')) {
      toggleExceptionTimes();
    }
  });

  document.addEventListener('submit', async event => {
    const form = event.target;
    if (!form.matches?.('#doctorScheduleExceptionForm')) return;

    event.preventDefault();

    const doctorId = form.querySelector('[name="doctorId"]')?.value || currentScheduleDoctorId();
    const date = form.querySelector('[name="date"]')?.value;
    const type = form.querySelector('[name="type"]')?.value;
    const reason = form.querySelector('[name="reason"]')?.value?.trim() || null;
    const startTime = form.querySelector('[name="startTime"]')?.value || null;
    const endTime = form.querySelector('[name="endTime"]')?.value || null;

    if (!doctorId) {
      toast('Сначала выберите врача', 'error');
      return;
    }

    if (type === 'CUSTOM_WORKING_HOURS' && (!startTime || !endTime)) {
      toast('Для особых часов укажите начало и окончание', 'error');
      return;
    }

    const submit = form.querySelector('button[type="submit"]');
    if (submit) submit.disabled = true;

    try {
      const created = await api(
        `/api/doctors/${doctorId}/schedule-exceptions`,
        {
          method: 'POST',
          body: {
            date,
            type,
            startTime: type === 'CUSTOM_WORKING_HOURS' ? startTime : null,
            endTime: type === 'CUSTOM_WORKING_HOURS' ? endTime : null,
            reason
          }
        }
      );

      sessionExceptions = [
        ...sessionExceptions.filter(item => item.id !== created?.id),
        created
      ].filter(Boolean);

      toast(type === 'DAY_OFF' ? 'Выходной задан' : 'Особые часы добавлены', 'success');
      form.querySelector('[name="reason"]').value = '';
      renderSessionExceptions();
      scheduleRefreshSoon(doctorId);
    } catch (error) {
      toast(error.message || 'Не удалось добавить исключение', 'error');
    } finally {
      if (submit) submit.disabled = false;
    }
  });

  document.addEventListener('click', async event => {
    const button = event.target.closest?.('[data-delete-schedule-exception]');
    if (!button) return;

    const { doctorId, exceptionId } = button.dataset;
    if (!doctorId || !exceptionId) return;

    button.disabled = true;
    try {
      await api(
        `/api/doctors/${doctorId}/schedule-exceptions/${exceptionId}`,
        { method: 'DELETE' }
      );
      sessionExceptions = sessionExceptions.filter(item => String(item.id) !== String(exceptionId));
      renderSessionExceptions();
      toast('Исключение удалено', 'success');
      scheduleRefreshSoon(doctorId);
    } catch (error) {
      toast(error.message || 'Не удалось удалить исключение', 'error');
      button.disabled = false;
    }
  });

  function start() {
    installExceptionPanel();
    fixModalStacking();

    new MutationObserver(() => {
      installExceptionPanel();
      fixModalStacking();
    }).observe(document.body, { subtree: true, childList: true });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();

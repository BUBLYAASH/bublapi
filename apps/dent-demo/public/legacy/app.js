import {
  api,
  clearToken,
  decodeJwt,
  detailItem,
  escapeHtml,
  fillForm,
  formToObject,
  formatDate,
  getAccessProfile,
  getToken,
  probeAccess,
  setAccessProfile,
  setToken,
  setupRequiredFieldMarkers,
  toast
} from './api.js';

import './phone.js';

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];

const AUTO_REFRESH_INTERVAL_MS = 15000;
const AUTO_REFRESH_VIEWS = new Set([
  'public-doctors',
  'public-services',
  'patient-card',
  'patient-appointments',
  'patient-notifications',
  'staff-dashboard',
  'staff-patients',
  'staff-doctors',
  'staff-services',
  'staff-appointments',
  'staff-users'
]);

const DAY_OF_WEEK_NAMES = {
  MONDAY: 'ПОНЕДЕЛЬНИК',
  TUESDAY: 'ВТОРНИК',
  WEDNESDAY: 'СРЕДА',
  THURSDAY: 'ЧЕТВЕРГ',
  FRIDAY: 'ПЯТНИЦА',
  SATURDAY: 'СУББОТА',
  SUNDAY: 'ВОСКРЕСЕНЬЕ'
};

function formatDayOfWeek(dayOfWeek) {
  return DAY_OF_WEEK_NAMES[dayOfWeek] ?? dayOfWeek ?? '—';
}

function formatTime24(value) {
  if (!value) {
    return '—';
  }

  return String(value).slice(0, 5);
}

const appointmentAvailability = {
  patient: new Map(),
  staff: new Map()
};

const availabilityRequestVersion = {
  patient: 0,
  staff: 0
};

function formatDateOption(dateValue) {
  const date = new Date(`${dateValue}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return dateValue;
  }

  return new Intl.DateTimeFormat('ru-RU', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  }).format(date);
}

function getBookingDateQuickList(kind) {
  return $(kind === 'patient' ? '#patientBookingDates' : '#staffBookingDates');
}

function localTodayKey() {
  return dashboardDateKey(new Date());
}

function filterPastTodaySlots(dateValue, slots = []) {
  if (dateValue !== localTodayKey()) return slots;
  const now = new Date();
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  return slots.filter(time => {
    const [hours, minutes] = String(time).split(':').map(Number);
    return Number.isFinite(hours) && Number.isFinite(minutes) && (hours * 60 + minutes) > nowMinutes;
  });
}

function renderBookingDateQuickList(kind) {
  const target = getBookingDateQuickList(kind);
  const { dateSelect } = getAppointmentFormElements(kind);
  if (!target || !dateSelect) return;

  const dates = [...appointmentAvailability[kind].keys()]
    .filter(date => (appointmentAvailability[kind].get(date) ?? []).length)
    .slice(0, 8);

  if (!dates.length) {
    target.innerHTML = '<span class="muted">Нет доступных дат.</span>';
    return;
  }

  target.innerHTML = dates.map(date => {
    const d = new Date(`${date}T00:00:00`);
    const weekday = new Intl.DateTimeFormat('ru-RU', { weekday:'short' }).format(d).replace('.', '');
    const dayMonth = new Intl.DateTimeFormat('ru-RU', { day:'numeric', month:'short' }).format(d).replace('.', '');
    return `<button class="booking-date-chip ${dateSelect.value === date ? 'selected' : ''}" type="button" data-booking-date="${escapeHtml(date)}"><span>${escapeHtml(weekday)}</span><strong>${escapeHtml(dayMonth)}</strong></button>`;
  }).join('');
}

function resetAvailabilityControls(kind, message = 'Сначала выберите врача и услугу') {
  const prefix = kind === 'patient' ? 'patient' : 'staff';
  const dateSelect = $(`#${prefix}AppointmentDate`);
  const timeSelect = $(`#${prefix}AppointmentTime`);

  appointmentAvailability[kind] = new Map();
  if (dateSelect) {
    dateSelect.value = '';
    dateSelect.disabled = true;
    dateSelect.removeAttribute('min');
    dateSelect.removeAttribute('max');
    dateSelect.dataset.emptyMessage = message;
  }
  if (timeSelect) {
    timeSelect.innerHTML = '<option value="">Выберите время</option>';
    timeSelect.disabled = true;
  }
  const quick = getBookingDateQuickList(kind);
  if (quick) quick.innerHTML = `<span class="muted">${escapeHtml(message)}</span>`;
  if (kind === 'patient') rebuildPatientBookingSlots();
}

function getAppointmentFormElements(kind) {
  const prefix = kind === 'patient' ? 'patient' : 'staff';
  const form = $(`#${prefix}AppointmentForm`);

  return {
    form,
    doctorSelect: $(`#${prefix}DoctorSelect`),
    serviceSelect: $(`#${prefix}ServiceSelect`),
    dateSelect: $(`#${prefix}AppointmentDate`),
    timeSelect: $(`#${prefix}AppointmentTime`),
    quantityInput: form?.querySelector('[name="quantity"]')
  };
}

function getSelectedService(kind) {
  const { serviceSelect } = getAppointmentFormElements(kind);
  const serviceId = serviceSelect?.value;

  if (!serviceId) {
    return null;
  }

  return kind === 'patient'
      ? caches.publicServices.get(serviceId)
      : caches.services.get(serviceId);
}

function getRequestedDurationMinutes(kind) {
  const service = getSelectedService(kind);
  const { quantityInput } = getAppointmentFormElements(kind);

  if (!service) {
    return null;
  }

  const quantity = Math.max(1, Number(quantityInput?.value || 1));
  return Number(service.durationMinutes) * quantity;
}

function populateAvailableTimes(kind, dateValue) {
  const { timeSelect } = getAppointmentFormElements(kind);
  if (!timeSelect) return;
  const slots = filterPastTodaySlots(dateValue, appointmentAvailability[kind].get(dateValue) ?? []);

  timeSelect.innerHTML = `
    <option value="">${slots.length ? 'Выберите время' : 'Свободного времени нет'}</option>
    ${slots.map(time => `<option value="${escapeHtml(time)}">${escapeHtml(formatTime24(time))}</option>`).join('')}
  `;

  timeSelect.disabled = slots.length === 0;
  renderBookingDateQuickList(kind);
  if (kind === 'patient') rebuildPatientBookingSlots();
}

async function loadAppointmentAvailability(kind, { silent = false } = {}) {
  const {
    doctorSelect,
    serviceSelect,
    dateSelect,
    timeSelect
  } = getAppointmentFormElements(kind);

  const doctorId = doctorSelect?.value;
  const serviceId = serviceSelect?.value;
  const durationMinutes = getRequestedDurationMinutes(kind);
  const requestVersion = ++availabilityRequestVersion[kind];

  if (!doctorId || !serviceId || !durationMinutes) {
    resetAvailabilityControls(kind);
    return;
  }

  const previouslySelectedDate = dateSelect?.value ?? '';
  const previouslySelectedTime = timeSelect?.value ?? '';

  dateSelect.disabled = true;
  timeSelect.disabled = true;
  const quickDates = getBookingDateQuickList(kind);
  if (quickDates) quickDates.innerHTML = '<span class="muted">Загрузка доступных дат…</span>';
  timeSelect.innerHTML = '<option value="">Сначала выберите дату</option>';

  try {
    const availability = await api(
        `/api/public/doctors/${doctorId}/availability?durationMinutes=${durationMinutes}&days=60`,
        { method: 'GET' }
    );

    if (requestVersion !== availabilityRequestVersion[kind]) {
      return;
    }

    appointmentAvailability[kind] = new Map(
        availability.map(item => [
          item.date,
          filterPastTodaySlots(item.date, (item.slots ?? []).map(formatTime24))
        ])
    );

    const dates = [...appointmentAvailability[kind].keys()]
      .filter(date => (appointmentAvailability[kind].get(date) ?? []).length);

    dateSelect.disabled = dates.length === 0;
    if (dates.length) {
      dateSelect.min = dates[0];
      dateSelect.max = dates[dates.length - 1];
    }

    if (previouslySelectedDate && appointmentAvailability[kind].has(previouslySelectedDate)
        && (appointmentAvailability[kind].get(previouslySelectedDate) ?? []).length) {
      dateSelect.value = previouslySelectedDate;
      populateAvailableTimes(kind, previouslySelectedDate);

      if ([...timeSelect.options].some(option => option.value === previouslySelectedTime)) {
        timeSelect.value = previouslySelectedTime;
      }
    } else if (dates.length) {
      dateSelect.value = dates[0];
      populateAvailableTimes(kind, dates[0]);
    } else {
      dateSelect.value = '';
      timeSelect.disabled = true;
    }
    renderBookingDateQuickList(kind);
  } catch (error) {
    if (requestVersion !== availabilityRequestVersion[kind]) {
      return;
    }

    resetAvailabilityControls(kind, 'Не удалось загрузить расписание');

    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

function getAllAppointmentDoctors(kind) {
  return kind === 'patient'
      ? [...caches.publicDoctors.values()]
      : [...caches.doctors.values()];
}

function getAllAppointmentServices(kind) {
  return kind === 'patient'
      ? [...caches.publicServices.values()]
      : [...caches.services.values()];
}

function relationToService(relation, fallbackServices) {
  const cached = fallbackServices.find(service => {
    return String(service.id) === String(relation.clinicServiceId);
  });

  return cached ?? {
    id: relation.clinicServiceId,
    dentalServiceId: relation.dentalServiceId,
    title: relation.title,
    price: relation.price,
    durationMinutes: relation.durationMinutes,
    active: true
  };
}

function relationToDoctor(relation, fallbackDoctors) {
  return fallbackDoctors.find(doctor => {
    return String(doctor.id) === String(relation.doctorId);
  });
}

function renderAppointmentDoctorSelect(kind, doctors, selectedId = '') {
  const { doctorSelect } = getAppointmentFormElements(kind);

  populateSelect(
      doctorSelect,
      doctors,
      {
        placeholder: doctors.length
            ? 'Выберите врача'
            : 'Нет врачей для выбранной услуги',
        getValue: doctor => doctor.id,
        getLabel: doctor => {
          const fullName = [
            doctor.lastName,
            doctor.firstName,
            doctor.middleName
          ]
          .filter(Boolean)
          .join(' ');

          return `${fullName} — ${doctor.specialty ?? 'специализация не указана'}`;
        }
      }
  );

  if (doctors.some(doctor => String(doctor.id) === String(selectedId))) {
    doctorSelect.value = selectedId;
  }

  if (kind === 'patient') rebuildPatientBookingChoices('doctor');
}

function renderAppointmentServiceSelect(kind, services, selectedId = '') {
  const { serviceSelect } = getAppointmentFormElements(kind);

  populateSelect(
      serviceSelect,
      services,
      {
        placeholder: services.length
            ? 'Выберите услугу'
            : 'Нет услуг для выбранного врача',
        getValue: service => service.id,
        getLabel: service => {
          const title = service.title ?? `Услуга ${service.id}`;
          return `${title} — ${service.price} ₽, ${service.durationMinutes} мин`;
        }
      }
  );

  if (services.some(service => String(service.id) === String(selectedId))) {
    serviceSelect.value = selectedId;
  }

  if (kind === 'patient') rebuildPatientBookingChoices('service');
}

async function filterServicesByDoctor(kind, doctorId, { silent = false } = {}) {
  const { serviceSelect } = getAppointmentFormElements(kind);
  const selectedServiceId = serviceSelect?.value ?? '';
  const allServices = getAllAppointmentServices(kind);

  if (!doctorId) {
    renderAppointmentServiceSelect(kind, allServices, selectedServiceId);
    return;
  }

  try {
    const relations = await api(
        `/api/public/doctors/${doctorId}/services`,
        { method: 'GET' }
    );

    const services = relations
    .map(relation => relationToService(relation, allServices))
    .filter(Boolean);

    renderAppointmentServiceSelect(kind, services, selectedServiceId);
  } catch (error) {
    renderAppointmentServiceSelect(kind, [], '');

    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

async function filterDoctorsByService(kind, clinicServiceId, { silent = false } = {}) {
  const { doctorSelect } = getAppointmentFormElements(kind);
  const selectedDoctorId = doctorSelect?.value ?? '';
  const allDoctors = getAllAppointmentDoctors(kind);

  if (!clinicServiceId) {
    renderAppointmentDoctorSelect(kind, allDoctors, selectedDoctorId);
    return;
  }

  try {
    const relations = await api(
        `/api/public/services/${clinicServiceId}/doctors`,
        { method: 'GET' }
    );

    const doctors = relations
    .map(relation => relationToDoctor(relation, allDoctors))
    .filter(Boolean);

    renderAppointmentDoctorSelect(kind, doctors, selectedDoctorId);
  } catch (error) {
    renderAppointmentDoctorSelect(kind, [], '');

    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

function bindAppointmentAvailability(kind) {
  const {
    doctorSelect,
    serviceSelect,
    dateSelect,
    quantityInput
  } = getAppointmentFormElements(kind);

  doctorSelect?.addEventListener('change', async event => {
    resetAvailabilityControls(kind);

    await filterServicesByDoctor(
        kind,
        event.currentTarget.value
    );

    await loadAppointmentAvailability(kind);
  });

  serviceSelect?.addEventListener('change', async event => {
    resetAvailabilityControls(kind);

    await filterDoctorsByService(
        kind,
        event.currentTarget.value
    );

    await loadAppointmentAvailability(kind);
  });

  quantityInput?.addEventListener('change', () => {
    loadAppointmentAvailability(kind);
  });

  dateSelect?.addEventListener('change', event => {
    const dateValue = event.currentTarget.value;
    populateAvailableTimes(kind, dateValue);
    if (dateValue && !appointmentAvailability[kind].has(dateValue)) {
      toast('На выбранную дату нет свободных окон', 'error');
    }
  });

  getBookingDateQuickList(kind)?.addEventListener('click', event => {
    const button = event.target.closest('[data-booking-date]');
    if (!button || !dateSelect) return;
    dateSelect.value = button.dataset.bookingDate;
    dateSelect.dispatchEvent(new Event('change', { bubbles:true }));
  });

  resetAvailabilityControls(kind);
}



let patientBookingStep = 1;

function getPatientSelectedOption(selectId) {
  const select = $(selectId);
  return select?.selectedOptions?.[0] ?? null;
}

function rebuildPatientBookingChoices(type) {
  const isService = type === 'service';
  const select = $(isService ? '#patientServiceSelect' : '#patientDoctorSelect');
  const target = $(isService ? '#patientBookingServices' : '#patientBookingDoctors');
  if (!select || !target) return;

  const query = isService ? String($('#patientServiceSearch')?.value || '').trim().toLocaleLowerCase('ru-RU') : '';
  const allOptions = [...select.options].filter(option => option.value);
  const options = allOptions.filter(option => {
    if (!query) return true;
    const cached = caches.publicServices.get(option.value);
    const searchable = [cached?.title, cached?.description, cached?.category, option.textContent].filter(Boolean).join(' ').toLocaleLowerCase('ru-RU');
    return searchable.includes(query);
  });

  if (isService) {
    const meta = $('#patientServiceSearchMeta');
    if (meta) meta.textContent = query ? `Найдено: ${options.length} из ${allOptions.length}` : `${allOptions.length} услуг доступно`;
  }

  if (!allOptions.length) {
    target.innerHTML = `<div class="booking-empty">${isService ? 'Для выбранного врача пока нет доступных услуг.' : 'Для выбранной услуги пока нет доступных специалистов.'}</div>`;
    return;
  }
  if (!options.length) {
    target.innerHTML = '<div class="booking-empty">По вашему запросу ничего не найдено. Попробуйте другое название.</div>';
    return;
  }

  target.innerHTML = options.map(option => {
    const selected = String(option.value) === String(select.value);
    const cached = isService ? caches.publicServices.get(option.value) : caches.publicDoctors.get(option.value);
    const title = isService
      ? (cached?.title ?? option.textContent.split(' — ')[0])
      : ([cached?.lastName, cached?.firstName, cached?.middleName].filter(Boolean).join(' ') || option.textContent.split(' — ')[0]);
    const meta = isService
      ? `${cached?.price ?? ''}${cached?.price != null ? ' ₽' : ''}${cached?.durationMinutes ? ` · ${cached.durationMinutes} мин` : ''}`
      : (cached?.specialty ?? option.textContent.split(' — ')[1] ?? 'Специалист');
    return `<button class="booking-choice-card ${selected ? 'selected' : ''}" type="button" data-booking-choice="${type}" data-value="${escapeHtml(option.value)}">
      <span class="booking-choice-icon" aria-hidden="true">${isService ? '✦' : '◉'}</span>
      <span><strong>${escapeHtml(title)}</strong><small>${escapeHtml(meta)}</small></span>
      <i aria-hidden="true">✓</i>
    </button>`;
  }).join('');
}

function rebuildPatientBookingSlots() {
  const dateSelect = $('#patientAppointmentDate');
  const timeSelect = $('#patientAppointmentTime');
  const target = $('#patientBookingSlots');
  if (!dateSelect || !timeSelect || !target) return;

  if (!dateSelect.value) {
    target.innerHTML = '<div class="booking-empty">Выберите доступную дату, чтобы увидеть свободное время.</div>';
    return;
  }

  const slots = filterPastTodaySlots(dateSelect.value, appointmentAvailability.patient.get(dateSelect.value) ?? []);
  if (!slots.length) {
    target.innerHTML = '<div class="booking-empty">На выбранную дату свободных окон нет.</div>';
    return;
  }

  target.innerHTML = slots.map(time => `<button class="booking-slot ${timeSelect.value === time ? 'selected' : ''}" type="button" data-booking-time="${escapeHtml(time)}">${escapeHtml(formatTime24(time))}</button>`).join('');
}

function updatePatientBookingSummary() {
  const service = getPatientSelectedOption('#patientServiceSelect');
  const doctor = getPatientSelectedOption('#patientDoctorSelect');
  const date = $('#patientAppointmentDate')?.value;
  const time = $('#patientAppointmentTime')?.value;
  const summary = $('#patientBookingSummary');
  const confirm = $('#patientBookingConfirm');

  if (summary) {
    summary.innerHTML = `<div><span>Услуга</span><strong>${escapeHtml(service?.textContent?.split(' — ')[0] || '—')}</strong></div><div><span>Врач</span><strong>${escapeHtml(doctor?.textContent?.split(' — ')[0] || '—')}</strong></div>`;
  }

  if (confirm) {
    confirm.innerHTML = `
      <div class="booking-confirm-row"><span>Услуга</span><strong>${escapeHtml(service?.textContent?.split(' — ')[0] || '—')}</strong></div>
      <div class="booking-confirm-row"><span>Специалист</span><strong>${escapeHtml(doctor?.textContent?.split(' — ')[0] || '—')}</strong></div>
      <div class="booking-confirm-row"><span>Дата и время</span><strong>${escapeHtml(date ? formatDateOption(date) : '—')}${time ? ` · ${escapeHtml(time)}` : ''}</strong></div>`;
  }
}

function showPatientBookingStep(step) {
  const targetStep = Math.min(4, Math.max(1, Number(step) || 1));
  patientBookingStep = targetStep;
  $$('.booking-step[data-booking-step]').forEach(section => section.classList.toggle('active', Number(section.dataset.bookingStep) === targetStep));
  $$('.booking-progress-step').forEach(button => {
    const n = Number(button.dataset.bookingJump);
    button.classList.toggle('active', n === targetStep);
    button.classList.toggle('complete', n < targetStep);
  });
  updatePatientBookingSummary();
  document.querySelector('#view-patient-appointments .booking-stepper')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function canAdvancePatientBooking(nextStep) {
  if (nextStep >= 2 && !$('#patientServiceSelect')?.value) {
    toast('Сначала выберите услугу', 'error'); return false;
  }
  if (nextStep >= 3 && !$('#patientDoctorSelect')?.value) {
    toast('Выберите специалиста', 'error'); return false;
  }
  if (nextStep >= 4 && (!$('#patientAppointmentDate')?.value || !$('#patientAppointmentTime')?.value)) {
    toast('Выберите дату и время', 'error'); return false;
  }
  return true;
}

function initPatientBookingStepper() {
  const form = $('#patientAppointmentForm');
  if (!form || form.dataset.stepperReady === 'true') return;
  form.dataset.stepperReady = 'true';

  form.addEventListener('click', async event => {
    const choice = event.target.closest('[data-booking-choice]');
    const slot = event.target.closest('[data-booking-time]');
    const next = event.target.closest('[data-booking-next]');
    const prev = event.target.closest('[data-booking-prev]');
    const jump = event.target.closest('[data-booking-jump]');

    if (choice) {
      const type = choice.dataset.bookingChoice;
      const select = $(type === 'service' ? '#patientServiceSelect' : '#patientDoctorSelect');
      select.value = choice.dataset.value;
      select.dispatchEvent(new Event('change', { bubbles: true }));
      rebuildPatientBookingChoices(type);
      updatePatientBookingSummary();
      return;
    }

    if (slot) {
      const select = $('#patientAppointmentTime');
      select.value = slot.dataset.bookingTime;
      rebuildPatientBookingSlots();
      updatePatientBookingSummary();
      return;
    }

    if (next) {
      const step = Number(next.dataset.bookingNext);
      if (canAdvancePatientBooking(step)) showPatientBookingStep(step);
      return;
    }
    if (prev) return showPatientBookingStep(Number(prev.dataset.bookingPrev));
    if (jump) {
      const step = Number(jump.dataset.bookingJump);
      if (step <= patientBookingStep || canAdvancePatientBooking(step)) showPatientBookingStep(step);
    }
  });

  $('#patientAppointmentDate')?.addEventListener('change', () => {
    rebuildPatientBookingSlots(); updatePatientBookingSummary();
  });
  $('#patientAppointmentTime')?.addEventListener('change', updatePatientBookingSummary);
  $('#patientServiceSearch')?.addEventListener('input', () => rebuildPatientBookingChoices('service'));
  $('#patientServiceSearch')?.addEventListener('search', () => rebuildPatientBookingChoices('service'));
  rebuildPatientBookingChoices('service');
  rebuildPatientBookingChoices('doctor');
  rebuildPatientBookingSlots();
  showPatientBookingStep(1);
}

function combineDateAndTime(date, time) {
  if (!date || !time) {
    return null;
  }

  return `${date}T${time}:00`;
}

function getDoctorDisplayName(source) {
  const directName = source.doctorFullName
      ?? source.doctorName
      ?? source.fullName;

  if (directName) {
    return directName;
  }

  const nestedDoctor = source.doctor ?? {};

  const parts = [
    source.doctorLastName ?? nestedDoctor.lastName,
    source.doctorFirstName ?? nestedDoctor.firstName,
    source.doctorMiddleName ?? nestedDoctor.middleName
  ].filter(Boolean);

  if (parts.length) {
    return parts.join(' ');
  }

  const cachedDoctor = caches.doctors.get(source.doctorId)
      ?? caches.publicDoctors.get(source.doctorId);

  if (cachedDoctor) {
    return [cachedDoctor.lastName, cachedDoctor.firstName, cachedDoctor.middleName]
    .filter(Boolean)
    .join(' ');
  }

  return source.doctorId ?? '—';
}

let autoRefreshTimer = null;
let refreshInProgress = false;
let accessProfile = getAccessProfile();

const DEFAULT_DOCTOR_AVATAR = '/default.png';

function getDoctorAvatarUrl(doctor = {}) {
  const value = doctor.avatarUrl ?? doctor.avatar_url;
  return value && String(value).trim() ? String(value).trim() : DEFAULT_DOCTOR_AVATAR;
}

function doctorAvatarHtml(doctor, className = 'doctor-avatar') {
  const source = escapeHtml(getDoctorAvatarUrl(doctor));
  return `<img class="${escapeHtml(className)}" src="${source}" alt="Аватар врача" onerror="this.onerror=null;this.src='${DEFAULT_DOCTOR_AVATAR}'">`;
}

async function uploadDoctorAvatar(file) {
  if (!file) return null;
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
    throw new Error('Поддерживаются только PNG, JPG и WEBP');
  }
  if (file.size > 5 * 1024 * 1024) {
    throw new Error('Размер аватара не должен превышать 5 МБ');
  }
  const dataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Не удалось прочитать файл'));
    reader.readAsDataURL(file);
  });
  const response = await fetch('/demo/avatar-upload', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ filename: file.name, dataUrl })
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(result.message || 'Не удалось загрузить аватар');
  return result.avatarUrl;
}

function doctorFormSnapshot(form) {
  const values = {};
  new FormData(form).forEach((value, key) => {
    if (key === 'avatarFile') return;
    values[key] = typeof value === 'string' ? value : '';
  });
  return JSON.stringify(values);
}

function setupDoctorAvatarForm(form, submitButton, { initialSnapshot = null } = {}) {
  if (!form || !submitButton) return;

  const fileInput = form.querySelector('[name="avatarFile"]');
  const preview = form.querySelector('.doctor-avatar-preview');
  const baseline = initialSnapshot ?? doctorFormSnapshot(form);
  let previewObjectUrl = null;

  const releasePreviewUrl = () => {
    if (previewObjectUrl) {
      URL.revokeObjectURL(previewObjectUrl);
      previewObjectUrl = null;
    }
  };

  const updateButtonState = () => {
    const hasAvatarFile = Boolean(fileInput?.files?.length);
    const changed = doctorFormSnapshot(form) !== baseline || hasAvatarFile;
    submitButton.disabled = !changed || !form.checkValidity();
  };

  const updatePreview = () => {
    const file = fileInput?.files?.[0];
    releasePreviewUrl();

    if (file && preview) {
      previewObjectUrl = URL.createObjectURL(file);
      preview.src = previewObjectUrl;
    }

    updateButtonState();
  };

  form.addEventListener('input', updateButtonState);
  form.addEventListener('change', event => {
    if (event.target === fileInput) updatePreview();
    else updateButtonState();
  });

  form.addEventListener('reset', () => {
    setTimeout(() => {
      releasePreviewUrl();
      if (preview) preview.src = DEFAULT_DOCTOR_AVATAR;
      updateButtonState();
    }, 0);
  });

  updateButtonState();
}

const caches = {
  publicDoctors: new Map(),
  publicServices: new Map(),
  patients: new Map(),
  doctors: new Map(),
  services: new Map(),
  dentalCatalog: new Map(),
  appointments: new Map(),
  users: new Map(),
  notifications: new Map()
};

function hydratePublicCatalogFromSsr() {
  const seedElement = document.getElementById('publicCatalogSsrData');
  const hydrated = { doctors: false, services: false };

  if (!seedElement) {
    return hydrated;
  }

  try {
    const seed = JSON.parse(seedElement.textContent || '{}');

    if (seed.doctors?.loaded && Array.isArray(seed.doctors.items)) {
      resetOpaqueRefsByPrefix('publicDoctor');
      seed.doctors.items.forEach((doctor, index) => {
        opaqueRefs.set(`publicDoctor_ssr_${index}`, doctor.id);
        doctorScheduleCache.set(
          String(doctor.id),
          Array.isArray(doctor.workingHours) ? doctor.workingHours : []
        );
      });
      caches.publicDoctors = new Map(
        seed.doctors.items.map(doctor => [doctor.id, doctor])
      );
      hydrated.doctors = true;
    }

    if (seed.services?.loaded && Array.isArray(seed.services.items)) {
      resetOpaqueRefsByPrefix('publicService');
      const groupedServices = [
        ['active', seed.services.items.filter(service => service.active !== false)],
        ['inactive', seed.services.items.filter(service => service.active === false)]
      ];
      groupedServices.forEach(([status, services]) => {
        services.forEach((service, index) => {
          opaqueRefs.set(`publicService_ssr_${status}_${index}`, service.id);
        });
      });
      caches.publicServices = new Map(
        seed.services.items.map(service => [service.id, service])
      );
      hydrated.services = true;
    }

    if (hydrated.doctors || hydrated.services) {
      populatePatientAppointmentSelects();
    }
  } catch (error) {
    console.warn('SSR-каталог не удалось восстановить:', error);
  }

  return hydrated;
}

function populateSelect(
    select,
    items,
    {
      placeholder,
      getValue,
      getLabel
    }
) {
  if (!select) {
    return;
  }

  const previousValue = select.value;

  const optionsHtml = items
  .map(item => {
    const value = escapeHtml(getValue(item));
    const label = escapeHtml(getLabel(item));

    return `<option value="${value}">${label}</option>`;
  })
  .join('');

  select.innerHTML = `
    <option value="">${escapeHtml(placeholder)}</option>
    ${optionsHtml}
  `;

  const previousOptionStillExists = items.some(item => {
    return String(getValue(item)) === previousValue;
  });

  if (previousOptionStillExists) {
    select.value = previousValue;
  }
}

function populatePatientAppointmentSelects() {
  const doctors = [...caches.publicDoctors.values()];
  const services = [...caches.publicServices.values()];
  const selectedDoctorId = $('#patientDoctorSelect')?.value ?? '';
  const selectedServiceId = $('#patientServiceSelect')?.value ?? '';

  renderAppointmentDoctorSelect('patient', doctors, selectedDoctorId);
  renderAppointmentServiceSelect('patient', services, selectedServiceId);

  if (selectedDoctorId) {
    filterServicesByDoctor(
        'patient',
        selectedDoctorId,
        { silent: true }
    ).then(() => loadAppointmentAvailability('patient', { silent: true }));
  } else if (selectedServiceId) {
    filterDoctorsByService(
        'patient',
        selectedServiceId,
        { silent: true }
    ).then(() => loadAppointmentAvailability('patient', { silent: true }));
  } else {
    resetAvailabilityControls('patient');
  }
}

function populateStaffAppointmentSelects(doctors, services) {
  const selectedDoctorId = $('#staffDoctorSelect')?.value ?? '';
  const selectedServiceId = $('#staffServiceSelect')?.value ?? '';

  renderAppointmentDoctorSelect('staff', doctors, selectedDoctorId);
  renderAppointmentServiceSelect('staff', services, selectedServiceId);

  if (selectedDoctorId) {
    filterServicesByDoctor(
        'staff',
        selectedDoctorId,
        { silent: true }
    ).then(() => loadAppointmentAvailability('staff', { silent: true }));
  } else if (selectedServiceId) {
    filterDoctorsByService(
        'staff',
        selectedServiceId,
        { silent: true }
    ).then(() => loadAppointmentAvailability('staff', { silent: true }));
  } else {
    resetAvailabilityControls('staff');
  }
}

function hasAccess(requirement) {
  if (!requirement) {
    return true;
  }

  return requirement
  .split(',')
  .map(item => item.trim())
  .some(item => {
    if (item === 'authenticated') {
      return Boolean(getToken());
    }

    return Boolean(accessProfile[item]);
  });
}

function hasStaffAccess() {
  return Boolean(accessProfile['staff-core'] || accessProfile['staff-appointments']);
}

function ensureSidebarContextControls() {
  const sidebar = $('.sidebar');
  if (!sidebar) return;

  const staffGroup = sidebar.querySelector('.nav-group[data-access="staff-core,staff-appointments"]');
  const title = staffGroup?.querySelector(':scope > .nav-title');
  if (title) title.textContent = 'Панель сотрудника';

  const enterButton = $('#sidebarEnterStaff');
  const exitButton = $('#sidebarExitStaff');
  if (!enterButton || !exitButton || enterButton.dataset.bound === 'true') return;
  enterButton.dataset.bound = 'true';
  exitButton.dataset.bound = 'true';

  enterButton.addEventListener('click', () => {
    showView(accessProfile['staff-core'] ? 'staff-dashboard' : 'staff-appointments');
  });

  exitButton.addEventListener('click', () => {
    showView(accessProfile.patient ? 'profile' : 'home');
  });
}

function syncSidebarContext(viewName = $('.view.active')?.id.replace('view-', '') || 'home') {
  const staffMode = viewName.startsWith('staff-');
  document.body.dataset.sidebarMode = staffMode ? 'staff' : 'patient';
  $('.sidebar')?.setAttribute('aria-label', staffMode ? 'Навигация панели сотрудника' : 'Навигация пациента');

  const enter = $('.sidebar-context-entry');
  if (enter) enter.classList.toggle('hidden', staffMode || !hasStaffAccess());

  const exit = $('.sidebar-context-exit');
  if (exit) exit.classList.toggle('hidden', !staffMode);
}

function applyAccessVisibility() {
  const authenticated = Boolean(getToken());

  $$('[data-access]').forEach(element => {
    const allowed = hasAccess(element.dataset.access);
    element.classList.toggle('hidden', !allowed);
  });

  $$('[data-guest-only]').forEach(element => {
    element.classList.toggle('hidden', authenticated);
  });

  const activeView = $('.view.active');

  if (
      activeView
      && activeView.dataset.access
      && !hasAccess(activeView.dataset.access)
  ) {
    showView('home');
  }

  syncSidebarContext();
}

async function resolveAccessProfile() {
  if (!getToken()) {
    accessProfile = {};
    setAccessProfile(accessProfile);
    applyAccessVisibility();
    return;
  }

  const [patient, staffCore, staffAppointments] = await Promise.all([
    probeAccess('/api/notifications/unread-count'),
    probeAccess('/api/patients'),
    probeAccess('/api/appointments')
  ]);

  accessProfile = {
    patient,
    'staff-core': staffCore,
    'staff-appointments': staffAppointments
  };

  setAccessProfile(accessProfile);
  applyAccessVisibility();
}

const VIEW_ROUTES = {
  home: '/',
  'public-doctors': '/doctors',
  'public-services': '/services',
  login: '/login',
  register: '/register',
  profile: '/profile',
  'patient-card': '/patient/card',
  'patient-appointments': '/patient/appointments',
  'patient-notifications': '/patient/notifications',
  'staff-dashboard': '/staff',
  'staff-patients': '/staff/patients',
  'staff-doctors': '/staff/doctors',
  'staff-services': '/staff/services',
  'staff-appointments': '/staff/appointments',
  'staff-users': '/staff/users'
};
const ROUTE_VIEWS = Object.fromEntries(Object.entries(VIEW_ROUTES).map(([view,path]) => [path,view]));
function viewFromPathname(){ const path=(location.pathname.replace(/\/+$/,'')||'/'); return ROUTE_VIEWS[path] || document.querySelector('[data-initial-view]')?.dataset.initialView || 'home'; }

const opaqueRefs = new Map();
function makeOpaqueRef(type, id) {
  const key = `${type}_${crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2)}`;
  opaqueRefs.set(key, id);
  return key;
}
function resolveOpaqueRef(key) { return opaqueRefs.get(key); }
function resetOpaqueRefsByPrefix(prefix) { for (const key of [...opaqueRefs.keys()]) if (key.startsWith(`${prefix}_`)) opaqueRefs.delete(key); }

function showView(name, { updateUrl = true, load = true } = {}) {
  const target = $(`#view-${name}`);

  if (!target) {
    return;
  }

  if (target.dataset.access && !hasAccess(target.dataset.access)) {
    toast('Для этого раздела недостаточно прав', 'error');
    return;
  }

  const currentView = $('.view.active')?.id.replace('view-', '') || 'home';
  const currentPortal = currentView.startsWith('staff-') ? 'staff' : 'patient';
  const nextPortal = name.startsWith('staff-') ? 'staff' : 'patient';

  const commitView = () => {
    $$('.view').forEach(view => {
      view.classList.toggle(
          'active',
          view.id === `view-${name}`
      );
    });

    $$('.nav-button').forEach(button => {
      const active = button.dataset.view === name || (name === 'register' && button.dataset.view === 'login');
      button.classList.toggle('active', active);
      if (active) button.setAttribute('aria-current', 'page');
      else button.removeAttribute('aria-current');
    });

    syncSidebarContext(name);

    if (updateUrl) { const targetPath = VIEW_ROUTES[name] || '/'; if (location.pathname !== targetPath) history.pushState({ view:name }, '', targetPath); }
  };

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (currentPortal !== nextPortal && document.startViewTransition && !reduceMotion) {
    document.documentElement.dataset.portalTransition = `${currentPortal}-${nextPortal}`;
    const transition = document.startViewTransition(commitView);
    transition.finished.finally(() => delete document.documentElement.dataset.portalTransition);
  } else {
    commitView();
  }

  // Переход в раздел всегда сразу запускает его загрузчик.
  // Это не зависит от таймера автообновления и не блокируется фокусом
  // в формах: пользователь должен увидеть данные сразу после открытия страницы.
  if (load) {
    void loadView(name, { silent: false });
  }
}

$$('.nav-button').forEach(button => {
  button.addEventListener('click', () => {
    showView(button.dataset.view);
  });
});

$$('[data-go]').forEach(control => {
  control.addEventListener('click', event => {
    event.preventDefault();
    showView(control.dataset.go);
  });
});

$('#openAuthBtn').addEventListener('click', () => {
  showView('login');
});

$$('[data-password-toggle]').forEach(button => {
  button.addEventListener('click', () => {
    const input = button.closest('.password-field')?.querySelector('input');
    if (!input) return;

    const revealing = input.type === 'password';
    input.type = revealing ? 'text' : 'password';
    button.textContent = revealing ? 'Скрыть' : 'Показать';
    button.setAttribute('aria-label', revealing ? 'Скрыть пароль' : 'Показать пароль');
  });
});

window.addEventListener('popstate', () => { showView(viewFromPathname(), { updateUrl:false }); });


const STAFF_WORKFLOW_COPY = {
  'create-patient': ['Новый пациент', 'Создайте карточку пациента. После сохранения он сразу появится в списке.'],
  'create-doctor': ['Добавить врача', 'Профиль специалиста создаётся отдельно от расписания и услуг.'],
  'doctor-schedule': ['Расписание врача', 'Настройте регулярные рабочие интервалы выбранного специалиста.'],
  'doctor-services': ['Услуги врача', 'Управляйте связью специалиста с услугами клиники.'],
  'add-service': ['Подключить услугу', 'Выберите услугу из глобального каталога и задайте параметры клиники.'],
  'create-appointment': ['Новая запись', 'Найдите пациента и выберите услугу, специалиста и свободное время.']
};

function closeStaffWorkflow() {
  const modal = $('#staffWorkflowModal');
  if (!modal) return;
  modal.classList.add('hidden');
  modal.setAttribute('aria-hidden', 'true');
  $$('.staff-workflow-panel', modal).forEach(panel => panel.classList.add('hidden'));
}

function setStaffPatientSelection(patient) {
  const idInput = $('#staffPatientId');
  const search = $('#staffPatientSearch');
  const suggestions = $('#staffPatientSuggestions');
  if (!idInput || !search) return;
  if (!patient) {
    idInput.value = '';
    search.value = '';
  } else {
    idInput.value = patient.id;
    search.value = [patient.lastName, patient.firstName, patient.middleName].filter(Boolean).join(' ');
  }
  suggestions?.classList.add('hidden');
}

function renderStaffAppointmentContext(context = {}) {
  const strip = $('#staffAppointmentContext');
  if (!strip) return;
  const parts = [];
  if (context.patientId) {
    const patient = caches.patients.get(String(context.patientId)) || caches.patients.get(context.patientId);
    if (patient) parts.push(`<span><b>Пациент</b>${escapeHtml([patient.lastName, patient.firstName].filter(Boolean).join(' '))}</span>`);
  }
  if (context.doctorId) {
    const doctor = caches.doctors.get(String(context.doctorId)) || caches.doctors.get(context.doctorId);
    if (doctor) parts.push(`<span><b>Врач</b>${escapeHtml([doctor.lastName, doctor.firstName].filter(Boolean).join(' '))}</span>`);
  }
  if (context.serviceId) {
    const service = caches.services.get(String(context.serviceId)) || caches.services.get(context.serviceId);
    if (service) parts.push(`<span><b>Услуга</b>${escapeHtml(getClinicServiceTitle(service))}</span>`);
  }
  if (context.date) parts.push(`<span><b>Дата</b>${escapeHtml(formatDateOption(context.date))}</span>`);
  if (context.time) parts.push(`<span><b>Время</b>${escapeHtml(context.time)}${context.endTime ? `–${escapeHtml(context.endTime)}` : ''}</span>`);
  strip.innerHTML = parts.join('');
  strip.classList.toggle('hidden', !parts.length);
}

async function openStaffWorkflow(kind, context = {}) {
  const modal = $('#staffWorkflowModal');
  if (!modal) return;
  const copy = STAFF_WORKFLOW_COPY[kind] || ['Действие', ''];
  $('#staffWorkflowTitle').textContent = copy[0];
  $('#staffWorkflowSubtitle').textContent = copy[1];
  $$('.staff-workflow-panel', modal).forEach(panel => panel.classList.toggle('hidden', panel.dataset.workflowPanel !== kind));
  modal.classList.remove('hidden');
  modal.setAttribute('aria-hidden', 'false');

  if (kind === 'create-appointment') {
    await loadStaffAppointments({ silent:true });
    const form = $('#staffAppointmentForm');
    if (form) {
      form.reset();
      resetAvailabilityControls('staff');
      if (context.patientId) {
        const patient = caches.patients.get(String(context.patientId)) || caches.patients.get(context.patientId);
        setStaffPatientSelection(patient || null);
      }
      if (context.doctorId && $('#staffDoctorSelect')) $('#staffDoctorSelect').value = String(context.doctorId);
      if (context.serviceId && $('#staffServiceSelect')) $('#staffServiceSelect').value = String(context.serviceId);
      if (context.doctorId || context.serviceId) await loadAppointmentAvailability('staff', { silent:true });
      if (context.date && $('#staffAppointmentDate')) {
        const dateInput = $('#staffAppointmentDate');
        if (appointmentAvailability.staff.has(context.date)) {
          dateInput.value = context.date;
          populateAvailableTimes('staff', context.date);
        }
      }
      if (context.time && $('#staffAppointmentTime')) {
        const timeSelect = $('#staffAppointmentTime');
        if ([...timeSelect.options].some(option => option.value === context.time)) timeSelect.value = context.time;
      }
      renderStaffAppointmentContext(context);
    }
  }

  if (kind === 'doctor-schedule') {
    await loadStaffDoctors({ silent:true });
    if (context.doctorId && $('#doctorScheduleDoctorSelect')) {
      $('#doctorScheduleDoctorSelect').value = String(context.doctorId);
      await loadDoctorSchedule(context.doctorId, { silent:true });
    }
  }

  if (kind === 'doctor-services') {
    await loadDoctorServiceManagementOptions({ silent:true });
    if (context.doctorId && $('#doctorServiceDoctorSelect')) {
      $('#doctorServiceDoctorSelect').value = String(context.doctorId);
      await loadAssignedDoctorServices(context.doctorId, { silent:true });
    }
  }

  if (kind === 'add-service') await loadDentalCatalogForClinicServices({ silent:true });
}

function applyStaffTableFilter(targetId) {
  const container = document.getElementById(targetId);
  if (!container) return;
  const search = document.querySelector(`[data-filter-target="${targetId}"]`);
  const query = String(search?.value || '').trim().toLowerCase();
  const activeQuick = document.querySelector(`[data-filter-table="${targetId}"] [data-table-filter].active`);
  const quick = String(activeQuick?.dataset.tableFilter || '').trim().toLowerCase();
  container.querySelectorAll('tbody tr').forEach(row => {
    const text = row.textContent.toLowerCase();
    row.hidden = Boolean((query && !text.includes(query)) || (quick && !text.includes(quick)));
  });
}

document.addEventListener('input', event => {
  const search = event.target.closest('[data-filter-target]');
  if (search) applyStaffTableFilter(search.dataset.filterTarget);
});

document.addEventListener('click', event => {
  const quick = event.target.closest('[data-table-filter]');
  if (quick) {
    const group = quick.closest('[data-filter-table]');
    group?.querySelectorAll('[data-table-filter]').forEach(button => button.classList.toggle('active', button === quick));
    if (group) applyStaffTableFilter(group.dataset.filterTable);
    return;
  }
  const workflow = event.target.closest('[data-staff-workflow]');
  if (workflow) {
    void openStaffWorkflow(workflow.dataset.staffWorkflow, {
      patientId: workflow.dataset.patientId,
      doctorId: workflow.dataset.doctorId,
      serviceId: workflow.dataset.serviceId,
      date: workflow.dataset.date
    });
    return;
  }
  if (event.target.closest('[data-staff-workflow-close]')) closeStaffWorkflow();
});

function openModal(title, bodyHtml, actionsHtml = '') {
  $('#detailModalTitle').textContent = title;
  $('#detailModalBody').innerHTML = bodyHtml;
  $('#detailModalActions').innerHTML = actionsHtml;
  $('#detailModal').classList.remove('hidden');
  $('#detailModal').setAttribute('aria-hidden', 'false');
}

function closeModal() {
  $('#detailModal').classList.add('hidden');
  $('#detailModal').setAttribute('aria-hidden', 'true');
  $('#detailModalBody').innerHTML = '';
  $('#detailModalActions').innerHTML = '';
}

$('#detailModal').addEventListener('click', async event => {
  if (event.target.closest('[data-modal-close]')) {
    closeModal();
    return;
  }

  const linkButton = event.target.closest('.doctor-user-link-confirm');
  if (!linkButton) return;

  linkButton.disabled = true;
  try {
    await linkDoctorToUser(
        linkButton.dataset.doctorId,
        linkButton.dataset.userEmail,
        linkButton.dataset.userPhone
    );
    closeModal();
    toast('Пользователь привязан к врачу', 'success');
    await loadStaffDoctors();
  } catch (error) {
    linkButton.disabled = false;
    toast(error.message, 'error');
  }
});

document.addEventListener('keydown', event => {
  if (event.key === 'Escape') {
    closeModal();
    closeStaffWorkflow();
  }
});

function renderTable(container, rows, columns, actions) {
  if (!rows?.length) {
    container.innerHTML = '<div class="card empty">Данных пока нет</div>';
    return;
  }

  const headers = columns
  .map(column => `<th>${column.label}</th>`)
  .join('');

  const rowsHtml = rows
  .map(row => {
    const cells = columns
    .map(column => {
      const value = column.render
          ? column.render(row)
          : escapeHtml(row[column.key] ?? '—');

      return `<td>${value}</td>`;
    })
    .join('');

    const actionCell = actions
        ? `<td>${actions(row)}</td>`
        : '';

    return `<tr>${cells}${actionCell}</tr>`;
  })
  .join('');

  container.innerHTML = `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            ${headers}
            ${actions ? '<th>Действия</th>' : ''}
          </tr>
        </thead>
        <tbody>${rowsHtml}</tbody>
      </table>
    </div>
  `;
}

function renderCards(container, items, renderer) {
  container.innerHTML = items?.length
      ? items.map(renderer).join('')
      : '<div class="card empty">Данных пока нет</div>';
}

function updateAuthState() {
  const token = getToken();

  $('#logoutBtn')?.classList.toggle('hidden', !token);

  const authInfo = $('#authInfo');
  if (authInfo) {
    authInfo.innerHTML = '';
    authInfo.classList.add('hidden');
  }

  applyAccessVisibility();
}

function setAuthStatus(id, message = '', type = 'error') {
  const status = $(`#${id}`);
  if (!status) return;
  status.textContent = message;
  status.classList.toggle('hidden', !message);
  status.dataset.type = type;
}

function setAuthSubmitting(form, submitting, pendingLabel) {
  const button = form?.querySelector('button[type="submit"]');
  if (!button) return;
  if (!button.dataset.defaultLabel) button.dataset.defaultLabel = button.textContent;
  button.disabled = submitting;
  button.textContent = submitting ? pendingLabel : button.dataset.defaultLabel;
  form.setAttribute('aria-busy', String(submitting));
}

$('#logoutBtn').addEventListener('click', () => {
  clearToken();
  accessProfile = {};
  stopAutoRefresh();
  stopAppointmentLiveUpdates();
  updateAuthState();
  showView('home');
  toast('Сессия завершена');
});

$('#loginForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  setAuthStatus('loginStatus');
  setAuthSubmitting(form, true, 'Входим…');

  try {
    const result = await api('/api/auth/login', {
      method: 'POST',
      body: formToObject(form)
    });

    const token = result?.token ?? result?.accessToken ?? result?.access_token;

    if (!token || typeof token !== 'string') {
      throw new Error('Сервер не вернул токен авторизации');
    }

    setToken(token);
    await resolveAccessProfile();
    updateAuthState();
    startAutoRefresh();
    startAppointmentLiveUpdates();

    // После входа уводим пользователя с формы авторизации. Иначе адрес
    // остаётся с #auth, и после обновления страницы кажется, что вход пропал,
    // хотя JWT уже сохранён в localStorage.
    if (accessProfile.patient) {
      showView('profile');
    } else {
      showView('home');
    }

    toast('Вход выполнен', 'success');
  } catch (error) {
    setAuthStatus('loginStatus', error.message);
    toast(error.message, 'error');
  } finally {
    setAuthSubmitting(form, false, 'Входим…');
  }
});

$('#registerForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const registrationData = formToObject(form);
  setAuthStatus('registerStatus');
  setAuthSubmitting(form, true, 'Создаём аккаунт…');

  try {
    const result = await api('/api/auth/register', {
      method: 'POST',
      body: registrationData
    });

    const authInfo = $('#authInfo');
    if (authInfo) {
      authInfo.innerHTML = '';
      authInfo.classList.add('hidden');
    }

    toast(
        result.patientCardMessage || 'Регистрация завершена',
        'success'
    );
    form.reset();
    const loginEmail = $('#loginForm input[name="email"]');
    if (loginEmail) loginEmail.value = registrationData.email || '';
    showView('login');
    setAuthStatus('loginStatus', 'Аккаунт создан. Теперь войдите с новым паролем.', 'success');
  } catch (error) {
    setAuthStatus('registerStatus', error.message);
    toast(error.message, 'error');
  } finally {
    setAuthSubmitting(form, false, 'Создаём аккаунт…');
  }
});

async function checkConnection() {
  try {
    const response = await fetch('/demo-config');
    const config = await response.json();
    const badge = $('#connectionStatus');
    if (!badge) return;
    badge.textContent = config.apiKeyConfigured ? 'Система доступна' : 'Сервис временно недоступен';
    badge.className = `badge ${config.apiKeyConfigured ? 'success' : 'warning'}`;
  } catch {
    const badge = $('#connectionStatus');
    if (badge) { badge.textContent = 'Сервис временно недоступен'; badge.className = 'badge danger'; }
  }
}

async function getPublicDoctorById(doctorId) {
  const doctor = await api(`/api/public/doctors/${doctorId}`, {
    method: 'GET'
  });

  const hours = await api(
      `/api/public/doctors/${doctorId}/working-hours`,
      {
        method: 'GET'
      }
  );

  openModal(
      `${doctor.lastName} ${doctor.firstName}`,
      `
        <div class="doctor-profile-head">
          ${doctorAvatarHtml(doctor, 'doctor-avatar doctor-avatar-large')}
          <div>
            <h3>${escapeHtml([doctor.lastName, doctor.firstName, doctor.middleName].filter(Boolean).join(' '))}</h3>
            <p class="muted">${escapeHtml(doctor.specialty || 'Специальность не указана')}</p>
          </div>
        </div>
        <div class="detail-grid">
          ${detailItem('Специальность', doctor.specialty)}
          ${detailItem('Статус', doctor.active ? 'Активен' : 'Недоступен')}
          ${detailItem('Описание', doctor.description, true)}
          <div class="detail-item full">
            <span class="detail-label">Рабочие часы</span>
            ${renderWorkingHours(hours)}
          </div>
        </div>
      `
  );
}



const APPOINTMENT_STATUS_SINGLE = { CREATED:'Создана', CONFIRMED:'Подтверждена', COMPLETED:'Завершена', CANCELLED:'Отменена' };
function appointmentStatusLabel(status){ return APPOINTMENT_STATUS_SINGLE[String(status || '').toUpperCase()] || String(status || '—'); }
const APPOINTMENT_STATUS_LABELS = {
  CREATED: 'Созданные',
  CONFIRMED: 'Подтверждённые',
  COMPLETED: 'Завершённые',
  CANCELLED: 'Отменённые'
};

const APPOINTMENT_STATUS_ORDER = [
  'CREATED',
  'CONFIRMED',
  'COMPLETED',
  'CANCELLED'
];

function renderAppointmentGroups(
    container,
    appointments,
    columns,
    actions,
    openByDefault = ['CREATED', 'CONFIRMED']
) {
  const previouslyOpened = new Set(
      [...container.querySelectorAll('.appointment-group[open]')]
      .map(group => group.dataset.status)
  );

  const grouped = new Map();

  appointments.forEach(appointment => {
    const status = appointment.status || 'UNKNOWN';
    const items = grouped.get(status) ?? [];
    items.push(appointment);
    grouped.set(status, items);
  });

  const statuses = [
    ...APPOINTMENT_STATUS_ORDER,
    ...[...grouped.keys()].filter(status => {
      return !APPOINTMENT_STATUS_ORDER.includes(status);
    })
  ];

  container.innerHTML = '';

  let renderedAnyGroup = false;

  statuses.forEach(status => {
    const items = grouped.get(status) ?? [];

    if (!items.length) {
      return;
    }

    renderedAnyGroup = true;

    const details = document.createElement('details');
    details.className = 'appointment-group';
    details.dataset.status = status;

    if (
        previouslyOpened.has(status)
        || (!previouslyOpened.size && openByDefault.includes(status))
    ) {
      details.open = true;
    }

    const summary = document.createElement('summary');
    summary.innerHTML = `
      <span>${escapeHtml(APPOINTMENT_STATUS_LABELS[status] ?? status)}</span>
      <span class="badge">${items.length}</span>
    `;

    const content = document.createElement('div');
    content.className = 'appointment-group-content';

    details.append(summary, content);
    container.append(details);

    renderTable(
        content,
        items,
        columns,
        actions
    );
  });

  if (!renderedAnyGroup) {
    container.innerHTML = '<div class="card empty">Записей пока нет</div>';
  }
}

async function loadPublicDoctors({ silent = false } = {}) {
  try {
    resetOpaqueRefsByPrefix('publicDoctor');
    const doctors = await api('/api/public/doctors', {
      method: 'GET'
    });

    const doctorsWithHours = await Promise.all(
        doctors.map(async doctor => {
          try {
            const hours = await api(
                `/api/public/doctors/${doctor.id}/working-hours`,
                { method: 'GET', cache: 'no-store' }
            );

            const normalizedHours = Array.isArray(hours) ? hours : [];
            doctorScheduleCache.set(String(doctor.id), normalizedHours);
            return { ...doctor, workingHours: normalizedHours };
          } catch {
            return { ...doctor, workingHours: [] };
          }
        })
    );

    caches.publicDoctors = new Map(
        doctorsWithHours.map(doctor => [doctor.id, doctor])
    );

    populatePatientAppointmentSelects();

    renderCards(
        $('#publicDoctors'),
        doctorsWithHours,
        doctor => { const ref = makeOpaqueRef('publicDoctor', doctor.id); return `
          <article class="card clickable-card public-doctor-card" data-ref="${ref}">
            <div class="doctor-card-layout">
              ${doctorAvatarHtml(doctor)}
              <div class="doctor-card-content">
                <span class="badge ${doctor.active ? 'success' : 'danger'}">
                  ${doctor.active ? 'Активен' : 'Недоступен'}
                </span>
                <h3>
                  <button class="link-button public-doctor-link" data-ref="${ref}">
                    ${escapeHtml([doctor.lastName, doctor.firstName, doctor.middleName].filter(Boolean).join(' '))}
                  </button>
                </h3>
                <p><strong>${escapeHtml(doctor.specialty || 'Специальность не указана')}</strong></p>
                <p class="muted">${escapeHtml(doctor.description || 'Описание не указано')}</p>
                <div class="doctor-card-schedule">
                  <span class="doctor-card-schedule-icon" aria-hidden="true">◷</span>
                  <div class="doctor-card-schedule-content"><strong>Рабочие часы:</strong>${renderCompactWorkingHours(doctor.workingHours)}</div>
                </div>
              </div>
              <span class="doctor-card-arrow" aria-hidden="true">›</span>
            </div>
          </article>
        `; }
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadPublicDoctors').addEventListener('click', () => {
  loadPublicDoctors();
});

$('#publicDoctors').addEventListener('click', async event => {
  const target = event.target.closest('.public-doctor-card, .public-doctor-link');

  if (!target) {
    return;
  }

  try {
    await getPublicDoctorById(resolveOpaqueRef(target.dataset.ref));
  } catch (error) {
    toast(error.message, 'error');
  }
});

async function getPublicServiceById(serviceId) {
  const service = await api(`/api/public/services/${serviceId}`, {
    method: 'GET'
  });

  openModal(
      service.title || 'Услуга',
      `
        <div class="detail-grid">
          ${service.description ? detailItem('Описание', service.description, true) : ''}
          ${detailItem('Цена', `${service.price} ₽`)}
          ${detailItem('Длительность', `${service.durationMinutes} мин`)}
          ${detailItem('Статус', service.active === false ? 'Отключена' : 'Активна')}
        </div>
      `
  );
}

async function loadPublicServices({ silent = false } = {}) {
  try {
    resetOpaqueRefsByPrefix('publicService');
    const services = await api('/api/public/services', {
      method: 'GET'
    });

    caches.publicServices = new Map(
        services.map(service => [service.id, service])
    );

    populatePatientAppointmentSelects();

    renderServiceStatusGroups(
        $('#publicServices'),
        services,
        (container, groupServices) => {
          container.classList.add('grid', 'grid-3');
          renderCards(
              container,
              groupServices,
              service => { const ref = makeOpaqueRef('publicService', service.id); return `
                <article class="card clickable-card public-service-card" data-ref="${ref}">
                  <span class="badge ${service.active === false ? 'danger' : 'success'}">
                    ${service.active === false ? 'Отключена' : 'Активна'}
                  </span>
                  <span class="badge">${service.durationMinutes} мин</span>
                  <h3>
                    <button class="link-button public-service-link" data-ref="${ref}">
                      ${escapeHtml(service.title || 'Стоматологическая услуга')}
                    </button>
                  </h3>
                  <div class="metric">${service.price} ₽</div>
                  <p class="muted">${escapeHtml(service.description || 'Подробности можно уточнить при записи.')}</p>
                </article>
              `; }
          );
        }
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadPublicServices').addEventListener('click', () => {
  loadPublicServices();
});

$('#publicServices').addEventListener('click', async event => {
  const target = event.target.closest('.public-service-card, .public-service-link');

  if (!target) {
    return;
  }

  try {
    await getPublicServiceById(resolveOpaqueRef(target.dataset.ref));
  } catch (error) {
    toast(error.message, 'error');
  }
});

async function loadPatientCard({ silent = false } = {}) {
  try {
    const patient = await api('/api/patient/patient-card', {
      method: 'GET'
    });

    $('#patientCardResult').innerHTML = `
      <h2>${escapeHtml(patient.lastName)} ${escapeHtml(patient.firstName)}</h2>
      <p>${escapeHtml(patient.email || '—')} · ${escapeHtml(patient.phone || '—')}</p>
      <p><strong>Дата рождения:</strong> ${escapeHtml(patient.birthDate || '—')}</p>
      <p><strong>Аллергии:</strong> ${escapeHtml(patient.allergies || '—')}</p>
      <p><strong>Хронические заболевания:</strong> ${escapeHtml(patient.chronicDiseases || '—')}</p>
      <p><strong>Примечания:</strong> ${escapeHtml(patient.notes || '—')}</p>
    `;

    fillForm($('#patientCardForm'), patient);
  } catch (error) {
    $('#patientCardResult').innerHTML = `
      <div class="empty">${escapeHtml(error.message)}</div>
    `;

    if (!silent && error.status !== 404) {
      toast(error.message, 'error');
    }
  }
}

$('#loadPatientCard').addEventListener('click', () => {
  loadPatientCard();
});

$('#createPatientCard').addEventListener('click', async () => {
  try {
    await api('/api/patient/patient-card', {
      method: 'POST',
      body: formToObject($('#patientCardForm'))
    });

    toast('Карточка создана', 'success');
    await loadPatientCard();
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#updatePatientCard').addEventListener('click', async () => {
  try {
    await api('/api/patient/patient-card', {
      method: 'PATCH',
      body: formToObject($('#patientCardForm'))
    });

    toast('Карточка обновлена', 'success');
    await loadPatientCard();
  } catch (error) {
    toast(error.message, 'error');
  }
});

function appointmentBody(form) {
  const values = formToObject(form);
  const scheduledAt = combineDateAndTime(
      values.appointmentDate,
      values.appointmentTime
  );

  if (!scheduledAt) {
    throw new Error('Выберите дату и время записи');
  }

  return {
    doctorId: values.doctorId,
    scheduledAt,
    services: [
      {
        clinicServiceId: values.clinicServiceId,
        quantity: Number(values.quantity || 1)
      }
    ],
    comment: values.comment || null
  };
}

function appointmentDetailsHtml(appointment) {
  return `
    <div class="detail-grid">
      ${detailItem('Дата', formatDate(appointment.scheduledAt))}
      ${detailItem('Окончание', formatDate(appointment.endAt))}
      ${detailItem('Врач', getDoctorDisplayName(appointment))}
      ${detailItem('Статус', appointmentStatusLabel(appointment.status))}
      ${detailItem('Цена', `${appointment.totalPrice ?? 0} ₽`)}
      ${detailItem('Комментарий', appointment.comment, true)}
      ${detailItem(
        'Услуги',
        (appointment.services || [])
        .map(service => `${service.title} ×${service.quantity}`)
        .join(', '),
        true
      )}
    </div>
  `;
}

async function getPatientAppointmentById(appointmentId) {
  const appointment = await api(
      `/api/patient/appointments/${appointmentId}`,
      {
        method: 'GET'
      }
  );

  openModal('Запись пациента', appointmentDetailsHtml(appointment));
}

async function loadPatientAppointments({ silent = false } = {}) {
  try {
    resetOpaqueRefsByPrefix('patientAppointment');
    const appointments = await api('/api/patient/appointments', {
      method: 'GET'
    });

    renderAppointmentGroups(
        $('#patientAppointments'),
        appointments,
        [
          {
            label: 'Дата',
            render: appointment => { const ref = makeOpaqueRef('patientAppointment', appointment.id); return `
              <button class="link-button patient-appointment-link" data-ref="${ref}">
                ${escapeHtml(formatDate(appointment.scheduledAt))}
              </button>
            `; }
          },
          {
            label: 'Врач',
            render: appointment => escapeHtml(getDoctorDisplayName(appointment))
          },
          {
            label: 'Услуги',
            render: appointment => escapeHtml(
                (appointment.services || [])
                .map(service => `${service.title} ×${service.quantity}`)
                .join(', ')
            )
          },
          {
            label: 'Цена',
            render: appointment => `${appointment.totalPrice ?? 0} ₽`
          },
          {
            label: 'Статус',
            render: appointment => `<span class="badge">${escapeHtml(appointmentStatusLabel(appointment.status))}</span>`
          }
        ],
        appointment => { const ref = makeOpaqueRef('patientAppointment', appointment.id); return String(appointment.status || '').toUpperCase() === 'CANCELLED'
            ? ''
            : `
              <button class="btn btn-danger btn-sm patient-cancel" data-ref="${ref}">
                Отменить
              </button>
            `; }
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadPatientAppointments').addEventListener('click', () => {
  loadPatientAppointments();
});

$('#patientAppointmentForm').addEventListener('submit', async event => {
  event.preventDefault();

  const form = event.currentTarget;

  try {
    await api('/api/patient/appointments', {
      method: 'POST',
      body: appointmentBody(form)
    });

    toast('Запись создана', 'success');

    form.reset();
    resetAvailabilityControls('patient');
    showPatientBookingStep(1);
    rebuildPatientBookingChoices('service');
    rebuildPatientBookingChoices('doctor');

    await loadPatientAppointments();
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#patientAppointments').addEventListener('click', async event => {
  const link = event.target.closest('.patient-appointment-link');
  const cancelButton = event.target.closest('.patient-cancel');

  try {
    if (link) {
      await getPatientAppointmentById(resolveOpaqueRef(link.dataset.ref));
    }

    if (cancelButton) {
      await api(
          `/api/patient/appointments/${resolveOpaqueRef(cancelButton.dataset.ref)}/cancel`,
          {
            method: 'PATCH'
          }
      );

      toast('Запись отменена', 'error');
      await loadPatientAppointments();
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

async function getNotificationById(notificationId) {
  const notification = await api(`/api/notifications/${notificationId}`, {
    method: 'GET'
  });

  openModal(
      notification.title,
      `
        <div class="detail-grid">
          ${detailItem('Отправлено', formatDate(notification.sentAt))}
          ${detailItem('Сообщение', notification.message, true)}
        </div>
      `
  );
}

async function loadNotifications({ silent = false } = {}) {
  try {
    resetOpaqueRefsByPrefix('notification');
    const [notifications, unread] = await Promise.all([
      api('/api/notifications', { method: 'GET' }),
      api('/api/notifications/unread-count', { method: 'GET' })
    ]);

    caches.notifications = new Map(
        notifications.map(item => [item.id, item])
    );

    $('#unreadBadge').textContent = `${unread.count} непрочитанных`;

    renderCards(
        $('#notifications'),
        notifications,
        notification => { const ref = makeOpaqueRef('notification', notification.id); return `
          <article class="card clickable-card notification-card" data-ref="${ref}">
            <div class="section-head">
              <div>
                <h3 style="margin-top:0">
                  <button class="link-button notification-link" data-ref="${ref}">
                    ${escapeHtml(notification.title)}
                  </button>
                </h3>
              </div>
              <span class="muted">${formatDate(notification.sentAt)}</span>
            </div>
            <p>${escapeHtml(notification.message)}</p>
            <div class="actions">
              <button class="btn btn-secondary btn-sm notification-read" data-ref="${ref}">
                Прочитать
              </button>
              <button class="btn btn-danger btn-sm notification-delete" data-ref="${ref}">
                Удалить
              </button>
            </div>
          </article>
        `; }
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadNotifications').addEventListener('click', () => {
  loadNotifications();
});

$('#readAllNotifications').addEventListener('click', async () => {
  try {
    await api('/api/notifications/read-all', {
      method: 'PATCH'
    });

    toast('Все уведомления прочитаны', 'success');
    await loadNotifications();
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#notifications').addEventListener('click', async event => {
  const link = event.target.closest('.notification-link');
  const card = event.target.closest('.notification-card');
  const readButton = event.target.closest('.notification-read');
  const deleteButton = event.target.closest('.notification-delete');

  try {
    if (readButton) {
      await api(`/api/notifications/${resolveOpaqueRef(readButton.dataset.ref)}/read`, {
        method: 'PATCH'
      });
      await loadNotifications();
      return;
    }

    if (deleteButton) {
      await api(`/api/notifications/${resolveOpaqueRef(deleteButton.dataset.ref)}`, {
        method: 'DELETE'
      });
      await loadNotifications();
      return;
    }

    const target = link || card;

    if (target) {
      await getNotificationById(resolveOpaqueRef(target.dataset.ref));
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

const profileForm = $('#profileForm');
const saveProfileButton = $('#saveProfile');
let profileBaseline = {};

function normalizeProfileValue(input) {
  if (input.name === 'phone') {
    return input.dataset.phoneNormalized || String(input.value || '').replace(/\D/g, '');
  }

  return String(input.value ?? '').trim();
}

function setProfilePhoneValue(input, rawPhone) {
  const digits = String(rawPhone ?? '').replace(/\D/g, '');
  const group = input.closest('.phone-input-group');
  const countrySelect = group?.querySelector('.phone-country-select');

  if (!countrySelect) {
    input.value = digits;
    input.dataset.phoneNormalized = digits;
    return;
  }

  const codes = [...countrySelect.options]
      .map(option => option.value)
      .filter((value, index, array) => array.indexOf(value) === index)
      .sort((a, b) => b.length - a.length);
  const code = codes.find(item => digits.startsWith(item)) || '7';
  countrySelect.value = code;
  input.value = digits.slice(code.length);
  input.dispatchEvent(new Event('input', { bubbles: true }));
}

function captureProfileBaseline() {
  profileBaseline = {};
  profileForm.querySelectorAll('[name]').forEach(input => {
    profileBaseline[input.name] = normalizeProfileValue(input);
    input.classList.remove('field-changed');
  });
  delete profileForm.dataset.userDirty;
  saveProfileButton.disabled = true;
}

function updateProfileDirtyState() {
  let changed = false;

  profileForm.querySelectorAll('[name]').forEach(input => {
    const isChanged = normalizeProfileValue(input) !== (profileBaseline[input.name] ?? '');
    input.classList.toggle('field-changed', isChanged);
    changed ||= isChanged;
  });

  saveProfileButton.disabled = !changed;
  if (changed) profileForm.dataset.userDirty = 'true';
  else delete profileForm.dataset.userDirty;
}

async function loadProfile({ silent = false } = {}) {
  try {
    const profile = await api('/api/profile', { method: 'GET' });

    profileForm.elements.email.value = profile?.email ?? '';
    setProfilePhoneValue(profileForm.elements.phone, profile?.phone ?? '');
    profileForm.elements.firstName.value = profile?.firstName ?? '';
    profileForm.elements.lastName.value = profile?.lastName ?? '';
    profileForm.elements.middleName.value = profile?.middleName ?? '';
    profileForm.elements.password.value = '';

    captureProfileBaseline();
  } catch (error) {
    if (!silent) toast(error.message, 'error');
  }
}

profileForm.addEventListener('input', updateProfileDirtyState);
profileForm.addEventListener('change', updateProfileDirtyState);

profileForm.addEventListener('submit', async event => {
  event.preventDefault();

  const body = {};
  profileForm.querySelectorAll('[name]').forEach(input => {
    const value = normalizeProfileValue(input);
    if (value !== (profileBaseline[input.name] ?? '')) {
      body[input.name] = value || null;
    }
  });

  if (!Object.keys(body).length) return;

  try {
    const updated = await api('/api/profile', {
      method: 'PATCH',
      body
    });

    if (updated) {
      profileForm.elements.email.value = updated.email ?? profileForm.elements.email.value;
      setProfilePhoneValue(profileForm.elements.phone, updated.phone ?? normalizeProfileValue(profileForm.elements.phone));
      profileForm.elements.firstName.value = updated.firstName ?? profileForm.elements.firstName.value;
      profileForm.elements.lastName.value = updated.lastName ?? profileForm.elements.lastName.value;
      profileForm.elements.middleName.value = updated.middleName ?? profileForm.elements.middleName.value;
    }
    profileForm.elements.password.value = '';
    captureProfileBaseline();
    toast('Профиль обновлён', 'success');
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#deactivateProfile').addEventListener('click', async () => {
  if (!confirm('Отключить аккаунт?')) {
    return;
  }

  try {
    await api('/api/profile/deactivation', {
      method: 'PATCH'
    });

    clearToken();
    accessProfile = {};
    stopAutoRefresh();
    stopAppointmentLiveUpdates();
    updateAuthState();
    showView('home');
    toast('Аккаунт отключён', 'error');
  } catch (error) {
    toast(error.message, 'error');
  }
});

function dashboardDateKey(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function getAppointmentDateValue(appointment) {
  return appointment.scheduledAt
      ?? appointment.appointmentDate
      ?? appointment.startAt
      ?? appointment.dateTime
      ?? appointment.date;
}

function getPatientCreatedValue(patient) {
  return patient.createdAt ?? patient.createdDate ?? patient.registrationDate ?? patient.registeredAt;
}

function renderDashboardOccupancy(appointments) {
  const chart = $('#dashboardOccupancyChart');
  const formatter = new Intl.DateTimeFormat('ru-RU', { weekday: 'short' });
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const days = [];

  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - offset);
    days.push({
      date,
      key: dashboardDateKey(date),
      count: 0
    });
  }

  const byKey = new Map(days.map(day => [day.key, day]));
  appointments.forEach(appointment => {
    const item = byKey.get(dashboardDateKey(getAppointmentDateValue(appointment)));
    if (item) item.count += 1;
  });

  const max = Math.max(1, ...days.map(day => day.count));
  chart.innerHTML = days.map(day => {
    const height = day.count === 0 ? 3 : Math.max(10, Math.round((day.count / max) * 100));
    return `
      <div class="dashboard-bar-column" title="${escapeHtml(day.date.toLocaleDateString('ru-RU'))}: ${day.count}">
        <div class="dashboard-bar-value">${day.count}</div>
        <div class="dashboard-bar-track"><div class="dashboard-bar" style="height:${height}%"></div></div>
        <div class="dashboard-bar-label">${escapeHtml(formatter.format(day.date))}</div>
      </div>
    `;
  }).join('');
}

function isDoctorWorkingNow(hours, now = new Date()) {
  const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const currentDay = days[now.getDay()];
  const currentMinutes = now.getHours() * 60 + now.getMinutes();

  return (hours || []).some(interval => {
    if (String(interval.dayOfWeek).toUpperCase() !== currentDay) return false;
    const toMinutes = value => {
      const [hoursPart, minutesPart] = String(value || '').slice(0, 5).split(':').map(Number);
      return hoursPart * 60 + minutesPart;
    };
    const start = toMinutes(interval.startTime);
    const end = toMinutes(interval.endTime);
    return Number.isFinite(start) && Number.isFinite(end) && currentMinutes >= start && currentMinutes < end;
  });
}

function isDoctorAvailableForWork(doctor) {
  if (doctor.active === false) return false;

  const unavailableBooleanFields = [
    'onVacation',
    'onLeave',
    'dayOff',
    'absent',
    'temporarilyUnavailable'
  ];

  if (unavailableBooleanFields.some(field => doctor[field] === true)) return false;

  const status = String(
    doctor.workStatus
      ?? doctor.availabilityStatus
      ?? doctor.status
      ?? ''
  ).toUpperCase();

  return ![
    'VACATION',
    'ON_VACATION',
    'ON_LEAVE',
    'DAY_OFF',
    'ABSENT',
    'SICK_LEAVE',
    'UNAVAILABLE',
    'INACTIVE'
  ].includes(status);
}

async function countDoctorsWorkingNow(doctors) {
  const activeDoctors = doctors.filter(doctor => doctor.active !== false);
  const availableDoctors = activeDoctors.filter(isDoctorAvailableForWork);

  const results = await Promise.all(availableDoctors.map(async doctor => {
    try {
      const hours = await api(`/api/public/doctors/${doctor.id}/working-hours`, { method: 'GET' });
      return isDoctorWorkingNow(hours);
    } catch (_) {
      return false;
    }
  }));

  return {
    working: results.filter(Boolean).length,
    total: activeDoctors.length
  };
}


function staffAppointmentPatientName(appointment, patientMap) {
  const direct = appointment.patientFullName ?? appointment.patientName;
  if (direct) return direct;
  const patient = patientMap.get(String(appointment.patientId));
  if (patient) return [patient.lastName, patient.firstName, patient.middleName].filter(Boolean).join(' ');
  return 'Пациент';
}

function staffAppointmentDoctorName(appointment, doctorMap) {
  const direct = getDoctorDisplayName(appointment);
  if (direct && direct !== '—') return direct;
  const doctor = doctorMap.get(String(appointment.doctorId));
  return doctor ? [doctor.lastName, doctor.firstName, doctor.middleName].filter(Boolean).join(' ') : 'Врач';
}

const staffDashboardCalendarState = {
  appointments: [],
  doctors: [],
  patients: [],
  schedules: new Map(),
  availability30: new Map(),
  selectedDate: dashboardDateKey(new Date()),
  mode: 'day',
  scheduleRequestVersion: 0,
  drag: null
};

function parseDashboardDateKey(value) {
  const parts = String(value || '').split('-').map(Number);
  if (parts.length !== 3 || parts.some(Number.isNaN)) return new Date();
  return new Date(parts[0], parts[1] - 1, parts[2], 12, 0, 0, 0);
}

function formatStaffCalendarDateLabel(dateKey) {
  const date = parseDashboardDateKey(dateKey);
  const label = new Intl.DateTimeFormat('ru-RU', { weekday:'long', day:'numeric', month:'long' }).format(date);
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function setStaffCalendarDate(dateKey) {
  staffDashboardCalendarState.selectedDate = dateKey || dashboardDateKey(new Date());
  const input = $('#staffCalendarDate');
  if (input) input.value = staffDashboardCalendarState.selectedDate;
  const label = $('#staffCalendarDateLabel');
  if (label) label.textContent = `${formatStaffCalendarDateLabel(staffDashboardCalendarState.selectedDate)} · записи по специалистам и времени`;
  renderStaffDashboardCalendar();
  loadStaffCalendarSchedulesForDate();
}

function shiftStaffCalendarDate(days) {
  const date = parseDashboardDateKey(staffDashboardCalendarState.selectedDate);
  date.setDate(date.getDate() + Number(days || 0));
  setStaffCalendarDate(dashboardDateKey(date));
}

function renderStaffCalendarEvent(a, patientMap) {
  const status = String(a.status || '').toUpperCase();
  const time = formatTime24(String(a.scheduledAt || '').split('T')[1]);
  const end = formatTime24(String(a.endAt || '').split('T')[1]);
  return `<button type="button" class="staff-calendar-event status-${status.toLowerCase()}" data-calendar-appointment="${escapeHtml(a.id)}"><span>${escapeHtml(time)}${end !== '—' ? `–${escapeHtml(end)}` : ''}</span><strong>${escapeHtml(staffAppointmentPatientName(a, patientMap))}</strong><small>${escapeHtml(appointmentStatusLabel(status))}</small></button>`;
}

function staffCalendarDayOfWeek(dateKey) {
  const day = parseDashboardDateKey(dateKey).getDay();
  return ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'][day];
}

function minutesFromTime(value) {
  const [hours, minutes] = String(value || '').slice(0,5).split(':').map(Number);
  return Number.isFinite(hours) && Number.isFinite(minutes) ? hours * 60 + minutes : null;
}

function timeFromMinutes(total) {
  const value = Math.max(0, Math.min(24 * 60, Number(total) || 0));
  const hours = Math.floor(value / 60);
  const minutes = value % 60;
  return `${String(hours).padStart(2,'0')}:${String(minutes).padStart(2,'0')}`;
}

function doctorIntervalsForCalendar(doctorId, dateKey) {
  const hours = staffDashboardCalendarState.schedules.get(String(doctorId)) || [];
  const day = staffCalendarDayOfWeek(dateKey);
  return hours
    .filter(interval => interval.dayOfWeek === day)
    .map(interval => ({ start:minutesFromTime(interval.startTime), end:minutesFromTime(interval.endTime) }))
    .filter(interval => interval.start != null && interval.end != null && interval.end > interval.start)
    .sort((a,b) => a.start - b.start);
}

function appointmentMinuteRange(appointment) {
  const start = minutesFromTime(String(appointment.scheduledAt || '').split('T')[1]);
  let end = minutesFromTime(String(appointment.endAt || '').split('T')[1]);
  if (start == null) return null;
  if (end == null || end <= start) end = start + 30;
  return { start, end };
}

function calendarRangeOverlapsAppointment(doctorId, start, end, appointments) {
  return appointments.some(appointment => {
    if (String(appointment.doctorId) !== String(doctorId)) return false;
    const range = appointmentMinuteRange(appointment);
    return range && start < range.end && end > range.start;
  });
}

function calendarCellAvailable(doctorId, start, end, dateKey, appointments) {
  const withinSchedule = doctorIntervalsForCalendar(doctorId, dateKey)
    .some(interval => start >= interval.start && end <= interval.end);
  return withinSchedule && !calendarRangeOverlapsAppointment(doctorId, start, end, appointments);
}

async function loadStaffCalendarSchedulesForDate({ force = false } = {}) {
  const requestVersion = ++staffDashboardCalendarState.scheduleRequestVersion;
  const activeDoctors = staffDashboardCalendarState.doctors.filter(d => d.active !== false);
  const missing = activeDoctors.filter(doctor => force || !staffDashboardCalendarState.schedules.has(String(doctor.id)) || !staffDashboardCalendarState.availability30.has(String(doctor.id)));

  if (missing.length) {
    await Promise.all(missing.map(async doctor => {
      const cacheBuster = force ? `?refresh=${Date.now()}` : '';
      const [hoursResult, availabilityResult] = await Promise.allSettled([
        api(`/api/public/doctors/${doctor.id}/working-hours${cacheBuster}`, { method:'GET', cache:'no-store' }),
        api(`/api/public/doctors/${doctor.id}/availability?durationMinutes=30&days=60${force ? `&refresh=${Date.now()}` : ''}`, { method:'GET', cache:'no-store' })
      ]);
      const hours = hoursResult.status === 'fulfilled' && Array.isArray(hoursResult.value) ? hoursResult.value : [];
      const availability = availabilityResult.status === 'fulfilled' && Array.isArray(availabilityResult.value) ? availabilityResult.value : [];
      staffDashboardCalendarState.schedules.set(String(doctor.id), hours);
      staffDashboardCalendarState.availability30.set(String(doctor.id), new Set(availability.filter(item => (item.slots ?? []).length).map(item => String(item.date))));
    }));
  }

  if (requestVersion === staffDashboardCalendarState.scheduleRequestVersion) renderStaffDashboardCalendar();
}

function renderStaffDashboardCalendar(appointments = staffDashboardCalendarState.appointments, doctors = staffDashboardCalendarState.doctors, patients = staffDashboardCalendarState.patients) {
  const target = $('#staffDashboardCalendar');
  if (!target) return;

  staffDashboardCalendarState.appointments = Array.isArray(appointments) ? appointments : [];
  staffDashboardCalendarState.doctors = Array.isArray(doctors) ? doctors : [];
  staffDashboardCalendarState.patients = Array.isArray(patients) ? patients : [];

  const selectedKey = staffDashboardCalendarState.selectedDate || dashboardDateKey(new Date());
  const patientMap = new Map(staffDashboardCalendarState.patients.map(p => [String(p.id), p]));
  const doctorMap = new Map(staffDashboardCalendarState.doctors.map(d => [String(d.id), d]));
  const dayAppointments = staffDashboardCalendarState.appointments
    .filter(a => dashboardDateKey(getAppointmentDateValue(a)) === selectedKey && String(a.status || '').toUpperCase() !== 'CANCELLED')
    .sort((a,b) => new Date(a.scheduledAt) - new Date(b.scheduledAt));

  target.classList.toggle('list-mode', staffDashboardCalendarState.mode === 'list');
  target.classList.toggle('timeline-mode', staffDashboardCalendarState.mode !== 'list');

  if (staffDashboardCalendarState.mode === 'list') {
    if (!dayAppointments.length) {
      target.innerHTML = `<div class="staff-calendar-list-empty"><strong>Записей нет</strong><span>${escapeHtml(formatStaffCalendarDateLabel(selectedKey))} пока свободен.</span></div>`;
      return;
    }
    target.innerHTML = `<div class="staff-calendar-list">${dayAppointments.map(a => {
      const doctorName = staffAppointmentDoctorName(a, doctorMap);
      const specialty = doctorMap.get(String(a.doctorId))?.specialty || '';
      return `<article class="staff-calendar-list-row"><div class="staff-calendar-list-time"><strong>${escapeHtml(formatTime24(String(a.scheduledAt || '').split('T')[1]))}</strong><small>${escapeHtml(formatTime24(String(a.endAt || '').split('T')[1]))}</small></div><div class="staff-calendar-list-main"><strong>${escapeHtml(staffAppointmentPatientName(a, patientMap))}</strong><span>${escapeHtml(doctorName)}${specialty ? ` · ${escapeHtml(specialty)}` : ''}</span></div><button class="btn btn-secondary btn-sm" type="button" data-calendar-appointment="${escapeHtml(a.id)}">Открыть</button></article>`;
    }).join('')}</div>`;
    return;
  }

  const activeDoctors = staffDashboardCalendarState.doctors.filter(d => d.active !== false);
  const scheduleLoaded = activeDoctors.every(doctor => staffDashboardCalendarState.schedules.has(String(doctor.id)));
  if (!scheduleLoaded) {
    target.innerHTML = '<div class="staff-calendar-list-empty"><strong>Загружаем расписание</strong><span>Проверяем доступных специалистов на выбранный день…</span></div>';
    loadStaffCalendarSchedulesForDate();
    return;
  }

  const selectedDateIsFutureOrToday = selectedKey >= localTodayKey();
  const availableDoctors = activeDoctors.filter(doctor => {
    if (!doctorIntervalsForCalendar(doctor.id, selectedKey).length) return false;
    const hasAppointment = dayAppointments.some(appointment => String(appointment.doctorId) === String(doctor.id));
    if (!selectedDateIsFutureOrToday) return true;
    const availableDates = staffDashboardCalendarState.availability30.get(String(doctor.id));
    return hasAppointment || Boolean(availableDates?.has(selectedKey));
  });
  if (!availableDoctors.length) {
    target.innerHTML = `<div class="staff-calendar-list-empty"><strong>В этот день врачи не работают</strong><span>${escapeHtml(formatStaffCalendarDateLabel(selectedKey))}: нет рабочих интервалов.</span></div>`;
    return;
  }

  const intervals = availableDoctors.flatMap(doctor => doctorIntervalsForCalendar(doctor.id, selectedKey));
  const minMinute = Math.floor(Math.min(...intervals.map(interval => interval.start)) / 30) * 30;
  const maxMinute = Math.ceil(Math.max(...intervals.map(interval => interval.end)) / 30) * 30;
  const rows = [];
  for (let minute = minMinute; minute < maxMinute; minute += 30) rows.push(minute);

  const headers = `<div class="staff-timeline-corner">Время</div>${availableDoctors.map(doctor => `<div class="staff-timeline-doctor"><strong>${escapeHtml([doctor.lastName, doctor.firstName].filter(Boolean).join(' '))}</strong><small>${escapeHtml(doctor.specialty || 'Специалист')}</small></div>`).join('')}`;
  const times = rows.map((minute, rowIndex) => `<div class="staff-timeline-time" style="grid-row:${rowIndex + 2};grid-column:1"><span>${escapeHtml(timeFromMinutes(minute))}</span></div>`).join('');

  const cells = availableDoctors.flatMap((doctor, doctorIndex) => rows.map((minute, rowIndex) => {
    const end = minute + 30;
    const available = calendarCellAvailable(doctor.id, minute, end, selectedKey, dayAppointments);
    return `<button type="button" class="staff-timeline-cell ${available ? 'is-free' : 'is-blocked'}" style="grid-row:${rowIndex + 2};grid-column:${doctorIndex + 2}" ${available ? '' : 'disabled'} data-calendar-slot data-doctor-id="${escapeHtml(doctor.id)}" data-date="${escapeHtml(selectedKey)}" data-start="${escapeHtml(timeFromMinutes(minute))}" data-end="${escapeHtml(timeFromMinutes(end))}" data-minute="${minute}" aria-label="${available ? `Создать запись ${timeFromMinutes(minute)}` : `Недоступно ${timeFromMinutes(minute)}`}"></button>`;
  })).join('');

  const events = dayAppointments.map(appointment => {
    const doctorIndex = availableDoctors.findIndex(doctor => String(doctor.id) === String(appointment.doctorId));
    const range = appointmentMinuteRange(appointment);
    if (doctorIndex < 0 || !range) return '';
    const rowStart = Math.max(0, Math.floor((range.start - minMinute) / 30));
    const span = Math.max(1, Math.ceil((range.end - range.start) / 30));
    const status = String(appointment.status || '').toLowerCase();
    return `<button type="button" class="staff-timeline-appointment status-${escapeHtml(status)}" style="grid-column:${doctorIndex + 2};grid-row:${rowStart + 2} / span ${span}" data-calendar-appointment="${escapeHtml(appointment.id)}"><span>${escapeHtml(timeFromMinutes(range.start))}–${escapeHtml(timeFromMinutes(range.end))}</span><strong>${escapeHtml(staffAppointmentPatientName(appointment, patientMap))}</strong><small>${escapeHtml(appointmentStatusLabel(appointment.status))}</small></button>`;
  }).join('');

  target.style.setProperty('--staff-calendar-columns', String(availableDoctors.length));
  target.innerHTML = `<div class="staff-timeline-grid" style="--timeline-columns:${availableDoctors.length};--timeline-rows:${rows.length}">${headers}${times}${cells}${events}</div>`;
}

function clearStaffCalendarDrag() {
  $$('#staffDashboardCalendar .staff-timeline-cell.is-selecting').forEach(cell => cell.classList.remove('is-selecting'));
  staffDashboardCalendarState.drag = null;
}

function paintStaffCalendarDrag() {
  const drag = staffDashboardCalendarState.drag;
  if (!drag) return;
  const min = Math.min(drag.startMinute, drag.endMinute);
  const max = Math.max(drag.startMinute, drag.endMinute);
  $$('#staffDashboardCalendar [data-calendar-slot]').forEach(cell => {
    const minute = Number(cell.dataset.minute);
    cell.classList.toggle('is-selecting', String(cell.dataset.doctorId) === String(drag.doctorId) && minute >= min && minute <= max && !cell.disabled);
  });
}

function renderStaffAttention(appointments, patients) {
  const list = $('#staffAttentionList');
  const count = $('#staffAttentionCount');
  if (!list || !count) return;
  const todayKey = dashboardDateKey(new Date());
  const patientMap = new Map(patients.map(p => [String(p.id), p]));
  const today = appointments.filter(a => dashboardDateKey(getAppointmentDateValue(a)) === todayKey);
  const created = appointments
    .filter(a => String(a.status || '').toUpperCase() === 'CREATED')
    .sort((a, b) => new Date(a.scheduledAt || 0) - new Date(b.scheduledAt || 0));
  const confirmed = today.filter(a => String(a.status || '').toUpperCase() === 'CONFIRMED');
  const cancelled = today.filter(a => String(a.status || '').toUpperCase() === 'CANCELLED');
  const items = [];
  if (created.length) items.push({ tone:'warning', icon:'◷', title:'Все неподтверждённые записи', value:created.length, text:'Ожидают подтверждения сотрудником клиники — независимо от даты приёма.', view:'staff-appointments' });
  if (confirmed.length) items.push({ tone:'info', icon:'◎', title:'Подтверждены на сегодня', value:confirmed.length, text:'Проверьте готовность кабинетов и специалистов.', view:'staff-appointments' });
  if (cancelled.length) items.push({ tone:'danger', icon:'×', title:'Отмены сегодня', value:cancelled.length, text:'Можно использовать освободившиеся окна для новых записей.', view:'staff-appointments' });
  const upcoming = today.filter(a => {
    const t = new Date(a.scheduledAt).getTime();
    const diff = t - Date.now();
    return diff >= 0 && diff <= 60*60*1000 && !['CANCELLED','COMPLETED'].includes(String(a.status||'').toUpperCase());
  });
  if (upcoming.length) items.unshift({ tone:'success', icon:'→', title:'Приёмы в ближайший час', value:upcoming.length, text:`Ближайший: ${staffAppointmentPatientName(upcoming[0], patientMap)}.`, view:'staff-appointments' });

  count.textContent = String(items.reduce((sum, item) => sum + Number(item.value || 0), 0));
  list.innerHTML = items.length ? items.map(item => `<button class="staff-attention-item ${item.tone}" type="button" data-go="${item.view}"><span class="staff-attention-icon">${item.icon}</span><span><strong>${escapeHtml(item.title)}</strong><small>${escapeHtml(item.text)}</small></span><b>${item.value}</b></button>`).join('') : '<div class="staff-attention-empty"><strong>Всё спокойно</strong><span>На сейчас нет срочных задач.</span></div>';
}

async function refreshStaffDashboard({ silent = false } = {}) {
  try {
    const [patients, doctors, appointments] = await Promise.all([
      api('/api/patients', { method: 'GET' }),
      api('/api/doctors', { method: 'GET' }),
      api('/api/appointments', { method: 'GET' })
    ]);

    const todayKey = dashboardDateKey(new Date());
    const appointmentsToday = appointments.filter(appointment =>
      dashboardDateKey(getAppointmentDateValue(appointment)) === todayKey
      && String(appointment.status || '').toUpperCase() !== 'CANCELLED'
    ).length;

    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    const patientsWeekValues = patients
      .map(getPatientCreatedValue)
      .filter(Boolean)
      .map(value => new Date(value))
      .filter(date => !Number.isNaN(date.getTime()) && date >= weekAgo);

    const doctorStats = await countDoctorsWorkingNow(doctors);

    $('#metricPatients').textContent = patients.length;
    $('#metricPatientsWeek').textContent = patientsWeekValues.length
      ? `+${patientsWeekValues.length} за неделю`
      : '0 за неделю';
    $('#metricAppointmentsToday').textContent = appointmentsToday;
    $('#metricDoctorsTotal').textContent = doctorStats.total;
    $('#metricDoctorsWorking').textContent = `${doctorStats.working} работают сейчас`;
    $('#metricAppointments').textContent = `${appointments.length} всего`;
    renderDashboardOccupancy(appointments);
    staffDashboardCalendarState.appointments = appointments;
    staffDashboardCalendarState.doctors = doctors;
    staffDashboardCalendarState.patients = patients;
    const dateInput = $('#staffCalendarDate');
    if (dateInput && !dateInput.value) dateInput.value = staffDashboardCalendarState.selectedDate;
    const dateLabel = $('#staffCalendarDateLabel');
    if (dateLabel) dateLabel.textContent = `${formatStaffCalendarDateLabel(staffDashboardCalendarState.selectedDate)} · записи по специалистам и времени`;
    renderStaffDashboardCalendar();
    loadStaffCalendarSchedulesForDate();
    renderStaffAttention(appointments, patients);
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#refreshStaffDashboard').addEventListener('click', () => {
  refreshStaffDashboard();
});

$('#view-staff-dashboard')?.addEventListener('click', async event => {
  const appointmentButton = event.target.closest('[data-calendar-appointment]');
  const modeButton = event.target.closest('[data-calendar-mode]');
  const createButton = event.target.closest('[data-calendar-create]');

  if (createButton) { await openStaffWorkflow('create-appointment', { doctorId:createButton.dataset.doctorId, date:createButton.dataset.date }); return; }

  if (modeButton) {
    staffDashboardCalendarState.mode = modeButton.dataset.calendarMode === 'list' ? 'list' : 'day';
    $$('#view-staff-dashboard [data-calendar-mode]').forEach(button => button.classList.toggle('active', button === modeButton));
    renderStaffDashboardCalendar();
    return;
  }

  if (event.target.closest('#staffCalendarPrev')) { shiftStaffCalendarDate(-1); return; }
  if (event.target.closest('#staffCalendarNext')) { shiftStaffCalendarDate(1); return; }
  if (event.target.closest('#staffCalendarToday')) { setStaffCalendarDate(dashboardDateKey(new Date())); return; }

  if (appointmentButton) {
    try { await getStaffAppointmentById(appointmentButton.dataset.calendarAppointment); }
    catch (error) { toast(error.message, 'error'); }
  }
});

$('#staffCalendarDate')?.addEventListener('change', event => {
  if (event.currentTarget.value) setStaffCalendarDate(event.currentTarget.value);
});


$('#staffDashboardCalendar')?.addEventListener('pointerdown', event => {
  const cell = event.target.closest('[data-calendar-slot]');
  if (!cell || cell.disabled || staffDashboardCalendarState.mode === 'list') return;
  event.preventDefault();
  const minute = Number(cell.dataset.minute);
  staffDashboardCalendarState.drag = {
    pointerId:event.pointerId,
    doctorId:cell.dataset.doctorId,
    date:cell.dataset.date,
    startMinute:minute,
    endMinute:minute
  };
  paintStaffCalendarDrag();
});

$('#staffDashboardCalendar')?.addEventListener('pointerover', event => {
  const drag = staffDashboardCalendarState.drag;
  const cell = event.target.closest('[data-calendar-slot]');
  if (!drag || !cell || cell.disabled || String(cell.dataset.doctorId) !== String(drag.doctorId)) return;
  const minute = Number(cell.dataset.minute);
  const min = Math.min(drag.startMinute, minute);
  const max = Math.max(drag.startMinute, minute);
  const selectedCells = [...document.querySelectorAll(`#staffDashboardCalendar [data-calendar-slot][data-doctor-id="${CSS.escape(String(drag.doctorId))}"]`)]
    .filter(item => {
      const value = Number(item.dataset.minute);
      return value >= min && value <= max;
    });
  if (selectedCells.some(item => item.disabled)) return;
  drag.endMinute = minute;
  paintStaffCalendarDrag();
});

window.addEventListener('pointerup', async event => {
  const drag = staffDashboardCalendarState.drag;
  if (!drag || drag.pointerId !== event.pointerId) return;
  const startMinute = Math.min(drag.startMinute, drag.endMinute);
  const endMinute = Math.max(drag.startMinute, drag.endMinute) + 30;
  const context = {
    doctorId:drag.doctorId,
    date:drag.date,
    time:timeFromMinutes(startMinute),
    endTime:timeFromMinutes(endMinute),
    durationMinutes:endMinute - startMinute
  };
  clearStaffCalendarDrag();
  await openStaffWorkflow('create-appointment', context);
});

$('#staffAttentionList')?.addEventListener('click', event => {
  const item = event.target.closest('[data-go]');
  if (item) showView(item.dataset.go);
});

function showPatientDetails(patient) {
  openModal(
      `${patient.lastName} ${patient.firstName}`,
      `
        <div class="detail-grid">
          ${detailItem('Patient ID', patient.id, true)}
          ${detailItem('User ID', patient.userId, true)}
          ${detailItem('Телефон', patient.phone)}
          ${detailItem('Email', patient.email)}
          ${detailItem('Дата рождения', patient.birthDate)}
          ${detailItem('Аллергии', patient.allergies, true)}
          ${detailItem('Хронические заболевания', patient.chronicDiseases, true)}
          ${detailItem('Примечания', patient.notes, true)}
        </div>
      `,
      `
        <button class="btn btn-success patient-create-appointment" data-id="${patient.id}">+ Создать запись</button>
        <button class="btn btn-primary patient-edit-modal" data-id="${patient.id}">Редактировать</button>
      `
  );
}

function openPatientEdit(patient) {
  openModal(
      'Редактировать пациента',
      `
        <form id="modalPatientEditForm" class="form-grid">
          <label>Имя<input name="firstName" required value="${escapeHtml(patient.firstName || '')}"></label>
          <label>Фамилия<input name="lastName" required value="${escapeHtml(patient.lastName || '')}"></label>
          <label>Отчество<input name="middleName" value="${escapeHtml(patient.middleName || '')}"></label>
          <label>Телефон<input name="phone" required value="${escapeHtml(patient.phone || '')}"></label>
          <label>Email<input name="email" type="email" value="${escapeHtml(patient.email || '')}"></label>
          <label>Дата рождения<input name="birthDate" type="date" value="${escapeHtml(patient.birthDate || '')}"></label>
          <label class="full">Аллергии<textarea name="allergies">${escapeHtml(patient.allergies || '')}</textarea></label>
          <label class="full">Хронические заболевания<textarea name="chronicDiseases">${escapeHtml(patient.chronicDiseases || '')}</textarea></label>
          <label class="full">Примечания<textarea name="notes">${escapeHtml(patient.notes || '')}</textarea></label>
        </form>
      `,
      `
        <button class="btn btn-primary patient-save-modal" data-id="${patient.id}">
          Сохранить
        </button>
      `
  );
}

async function loadStaffPatients({ silent = false } = {}) {
  try {
    const patients = await api('/api/patients', {
      method: 'GET'
    });

    caches.patients = new Map(
        patients.map(patient => [patient.id, patient])
    );

    renderTable(
        $('#staffPatients'),
        patients,
        [
          {
            label: 'ФИО',
            render: patient => `
              <button class="link-button patient-details" data-id="${patient.id}">
                ${escapeHtml(patient.lastName)} ${escapeHtml(patient.firstName)}
              </button>
            `
          },
          { label: 'Телефон', key: 'phone' },
          { label: 'Email', key: 'email' },
          { label: 'Дата рождения', key: 'birthDate' }
        ],
        patient => `
          <div class="actions"><button class="btn btn-success btn-sm patient-book" data-id="${patient.id}">Записать</button><button class="btn btn-secondary btn-sm patient-edit" data-id="${patient.id}">Изменить</button></div>
        `
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadStaffPatients').addEventListener('click', () => {
  loadStaffPatients();
});

$('#createPatientForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;

  try {
    await api('/api/patients', {
      method: 'POST',
      body: formToObject(form)
    });

    toast('Пациент создан', 'success');
    form.reset();
    closeStaffWorkflow();
    await loadStaffPatients();
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#staffPatients').addEventListener('click', event => {
  const details = event.target.closest('.patient-details');
  const edit = event.target.closest('.patient-edit');
  const book = event.target.closest('.patient-book');
  const target = details || edit || book;

  if (!target) {
    return;
  }

  const patient = caches.patients.get(target.dataset.id);

  if (!patient) {
    return;
  }

  if (book) {
    void openStaffWorkflow('create-appointment', { patientId: patient.id });
  } else if (edit) {
    openPatientEdit(patient);
  } else {
    showPatientDetails(patient);
  }
});

const WORKING_DAY_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
];

function groupWorkingHours(hours = []) {
  return hours.reduce((groups, interval) => {
    const day = interval.dayOfWeek;
    (groups[day] ??= []).push(interval);
    return groups;
  }, {});
}

function renderWorkingHours(hours = [], { editable = false, doctorId = '' } = {}) {
  if (!hours.length) {
    return '<p class="muted">Расписание пока не указано.</p>';
  }

  const grouped = groupWorkingHours(hours);

  return `<div class="working-hours-list">${WORKING_DAY_ORDER
    .filter(day => grouped[day]?.length)
    .map(day => {
      const intervals = grouped[day]
        .slice()
        .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
        .map(interval => `
          <div class="working-hours-interval-row">
            <span class="working-hours-interval">
              ${escapeHtml(formatTime24(interval.startTime))}–${escapeHtml(formatTime24(interval.endTime))}
            </span>
            ${editable ? `
              <button class="btn btn-primary btn-sm schedule-edit" type="button"
                data-doctor-id="${escapeHtml(doctorId)}"
                data-schedule-id="${escapeHtml(interval.id)}"
                data-start-time="${escapeHtml(formatTime24(interval.startTime))}"
                data-end-time="${escapeHtml(formatTime24(interval.endTime))}">
                Изменить
              </button>
              <button class="btn btn-danger btn-sm schedule-delete" type="button"
                data-doctor-id="${escapeHtml(doctorId)}"
                data-schedule-id="${escapeHtml(interval.id)}">
                Удалить
              </button>
            ` : ''}
          </div>
        `).join('');

      return `
        <div class="working-hours-day-row">
          <div class="working-hours-day-name">${escapeHtml(formatDayOfWeek(day))}</div>
          <div class="working-hours-intervals">${intervals}</div>
        </div>
      `;
    }).join('')}</div>`;
}


function renderCompactWorkingHours(hours = []) {
  if (!Array.isArray(hours) || !hours.length) {
    return '<span class="muted">Расписание пока не указано</span>';
  }

  const grouped = groupWorkingHours(hours);
  const shortDays = {
    MONDAY: 'пн',
    TUESDAY: 'вт',
    WEDNESDAY: 'ср',
    THURSDAY: 'чт',
    FRIDAY: 'пт',
    SATURDAY: 'сб',
    SUNDAY: 'вс'
  };

  return WORKING_DAY_ORDER
    .filter(day => grouped[day]?.length)
    .map(day => {
      const intervals = grouped[day]
        .slice()
        .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
        .map(interval => `${escapeHtml(formatTime24(interval.startTime))}–${escapeHtml(formatTime24(interval.endTime))}`)
        .join(', ');

      return `<span class="doctor-card-schedule-day"><strong>${shortDays[day]}</strong> ${intervals}</span>`;
    })
    .join('');
}

let doctorScheduleRequestVersion = 0;
const doctorScheduleCache = new Map();

function renderCachedDoctorSchedule(doctorId) {
  const target = $('#doctorScheduleList');
  if (!target || !doctorId) return;

  const hours = doctorScheduleCache.get(String(doctorId)) || [];
  target.innerHTML = renderWorkingHours(hours, {
    editable: true,
    doctorId
  });
}

async function loadDoctorSchedule(
    doctorId,
    { silent = false, force = false } = {}
) {
  const target = $('#doctorScheduleList');
  const requestVersion = ++doctorScheduleRequestVersion;

  if (!target) {
    return;
  }

  if (!doctorId) {
    target.innerHTML = '<p class="muted">Выберите врача, чтобы увидеть его расписание.</p>';
    return;
  }

  target.innerHTML = '<p class="muted">Загрузка расписания…</p>';

  try {
    const cacheBuster = force ? `?refresh=${Date.now()}` : '';
    const hours = await api(
        `/api/public/doctors/${doctorId}/working-hours${cacheBuster}`,
        {
          method: 'GET',
          cache: 'no-store'
        }
    );

    // Старый запрос не должен перерисовывать список поверх более свежего.
    if (requestVersion !== doctorScheduleRequestVersion) {
      return;
    }

    doctorScheduleCache.set(String(doctorId), Array.isArray(hours) ? hours : []);
    renderCachedDoctorSchedule(doctorId);
  } catch (error) {
    if (requestVersion !== doctorScheduleRequestVersion) {
      return;
    }

    target.innerHTML = '<p class="muted">Не удалось загрузить расписание.</p>';
    if (!silent) toast(error.message, 'error');
  }
}

function openScheduleEdit(button) {
  openModal(
      'Изменить рабочий интервал',
      `
        <form id="modalScheduleEditForm" class="form-grid">
          <label>Начало<input name="startTime" required type="time" value="${escapeHtml(button.dataset.startTime)}"></label>
          <label>Окончание<input name="endTime" required type="time" value="${escapeHtml(button.dataset.endTime)}"></label>
        </form>
      `,
      `
        <button class="btn btn-primary schedule-save-modal"
          data-doctor-id="${escapeHtml(button.dataset.doctorId)}"
          data-schedule-id="${escapeHtml(button.dataset.scheduleId)}">
          Сохранить
        </button>
      `
  );
}

function doctorDetailsHtml(doctor, hours = []) {
  return `
    <div class="doctor-profile-head">
      ${doctorAvatarHtml(doctor, 'doctor-avatar doctor-avatar-large')}
      <div>
        <h3>${escapeHtml([doctor.lastName, doctor.firstName, doctor.middleName].filter(Boolean).join(' '))}</h3>
        <p class="muted">${escapeHtml(doctor.specialty || 'Специальность не указана')}</p>
      </div>
    </div>
    <div class="detail-grid">
      ${detailItem('Doctor ID', doctor.id, true)}
      ${detailItem('User ID', doctor.userId, true)}
      ${detailItem('Специальность', doctor.specialty)}
      ${detailItem('Статус', doctor.active ? 'Активен' : 'Отключён')}
      ${detailItem('Описание', doctor.description, true)}
      <div class="detail-item full">
        <span class="detail-label">Рабочие часы</span>
        ${renderWorkingHours(hours)}
      </div>
    </div>
  `;
}

async function getStaffDoctorById(doctorId) {
  const doctor = await api(`/api/doctors/${doctorId}`, {
    method: 'GET'
  });

  let hours = [];

  try {
    hours = await api(
        `/api/public/doctors/${doctorId}/working-hours`,
        {
          method: 'GET'
        }
    );
  } catch {
    // Профиль неактивного врача всё равно должен открываться для staff.
  }

  caches.doctors.set(doctor.id, doctor);

  openModal(
      `${doctor.lastName} ${doctor.firstName}`,
      doctorDetailsHtml(doctor, hours),
      `
        <button class="btn btn-success doctor-create-appointment" data-id="${doctor.id}">+ Создать запись</button>
        <button class="btn btn-secondary doctor-schedule-modal" data-id="${doctor.id}">Расписание</button>
        <button class="btn btn-secondary doctor-services-modal" data-id="${doctor.id}">Услуги</button>
        <button class="btn btn-primary doctor-edit-modal" data-id="${doctor.id}">Редактировать</button>
      `
  );
}

function openDoctorEdit(doctor) {
  openModal(
      'Редактировать врача',
      `
        <form id="modalDoctorEditForm" class="form-grid">
          <label>Имя<input name="firstName" required value="${escapeHtml(doctor.firstName || '')}"></label>
          <label>Фамилия<input name="lastName" required value="${escapeHtml(doctor.lastName || '')}"></label>
          <label>Отчество<input name="middleName" value="${escapeHtml(doctor.middleName || '')}"></label>
          <label>Специальность<input name="specialty" required value="${escapeHtml(doctor.specialty || '')}"></label>
          <div class="full avatar-picker">
            <span>Аватар</span>
            <div class="avatar-picker-row">
              ${doctorAvatarHtml(doctor, 'doctor-avatar doctor-avatar-preview')}
              <label class="avatar-upload-control">Выбрать изображение
                <input name="avatarFile" type="file" accept="image/png,image/jpeg,image/webp">
              </label>
            </div>
            <input name="avatarUrl" type="hidden" value="${escapeHtml(doctor.avatarUrl || '')}">
            <small class="muted">PNG, JPG или WEBP, до 5 МБ</small>
          </div>
          <label class="full">Описание<textarea name="description">${escapeHtml(doctor.description || '')}</textarea></label>
        </form>
      `,
      `
        <button class="btn btn-primary doctor-save-modal" data-id="${doctor.id}" disabled>
          Сохранить
        </button>
      `
  );

  setupDoctorAvatarForm(
      $('#modalDoctorEditForm'),
      $('#detailModalActions .doctor-save-modal')
  );
}

async function loadDoctorServiceManagementOptions({ silent = false } = {}) {
  try {
    const [doctors, services] = await Promise.all([
      api('/api/doctors', { method: 'GET' }),
      api('/api/services', { method: 'GET' })
    ]);

    caches.doctors = new Map(
        doctors.map(doctor => [doctor.id, doctor])
    );

    caches.services = new Map(
        services.map(service => [service.id, service])
    );

    caches.patients = new Map(
        patients.map(patient => [patient.id, patient])
    );

    populateSelect(
        $('#doctorServiceDoctorSelect'),
        doctors,
        {
          placeholder: doctors.length
              ? 'Выберите врача'
              : 'Врачей пока нет',
          getValue: doctor => doctor.id,
          getLabel: doctor => {
            const fullName = [
              doctor.lastName,
              doctor.firstName,
              doctor.middleName
            ]
            .filter(Boolean)
            .join(' ');

            return `${fullName} — ${doctor.specialty ?? 'специализация не указана'}`;
          }
        }
    );

    populateSelect(
        $('#doctorServiceClinicServiceSelect'),
        services,
        {
          placeholder: services.length
              ? 'Выберите услугу'
              : 'Услуг клиники пока нет',
          getValue: service => service.id,
          getLabel: service => {
            const title = service.title ?? `Услуга ${service.id}`;
            return `${title} — ${service.price} ₽, ${service.durationMinutes} мин`;
          }
        }
    );

    const selectedDoctorId = $('#doctorServiceDoctorSelect').value;

    if (selectedDoctorId) {
      await loadAssignedDoctorServices(selectedDoctorId, { silent: true });
    } else {
      $('#assignedDoctorServices').innerHTML = `
        <div class="empty">Выберите врача, чтобы увидеть его услуги</div>
      `;
    }
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

async function loadAssignedDoctorServices(
    doctorId,
    { silent = false } = {}
) {
  const container = $('#assignedDoctorServices');

  if (!doctorId) {
    container.innerHTML = `
      <div class="empty">Выберите врача, чтобы увидеть его услуги</div>
    `;
    return;
  }

  container.innerHTML = `
    <div class="empty">Загрузка услуг врача…</div>
  `;

  try {
    const relations = await api(
        `/api/public/doctors/${doctorId}/services`,
        { method: 'GET' }
    );

    if (!relations.length) {
      container.innerHTML = `
        <div class="empty">К врачу пока не привязаны услуги</div>
      `;
      return;
    }

    container.innerHTML = relations
    .map(relation => `
      <div class="card" style="margin-top:10px">
        <div class="section-head">
          <div>
            <h3>${escapeHtml(relation.title ?? 'Услуга')}</h3>
            <p class="muted">
              ${escapeHtml(String(relation.price ?? '—'))} ₽ ·
              ${escapeHtml(String(relation.durationMinutes ?? '—'))} мин
            </p>
          </div>
          <button
            class="btn btn-danger btn-sm assigned-service-remove"
            data-doctor-id="${escapeHtml(doctorId)}"
            data-service-id="${escapeHtml(relation.clinicServiceId)}"
            type="button"
          >
            Отвязать
          </button>
        </div>
      </div>
    `)
    .join('');
  } catch (error) {
    container.innerHTML = `
      <div class="empty">Не удалось загрузить услуги врача</div>
    `;

    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#doctorServiceDoctorSelect').addEventListener('change', event => {
  loadAssignedDoctorServices(event.currentTarget.value);
});

$('#assignDoctorService').addEventListener('click', async () => {
  const doctorId = $('#doctorServiceDoctorSelect').value;
  const clinicServiceId = $('#doctorServiceClinicServiceSelect').value;

  if (!doctorId || !clinicServiceId) {
    toast('Сначала выберите врача и услугу', 'error');
    return;
  }

  try {
    await api(
        `/api/doctors/${doctorId}/services/${clinicServiceId}`,
        { method: 'POST' }
    );

    toast('Услуга привязана к врачу', 'success');
    await loadAssignedDoctorServices(doctorId);
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#removeDoctorService').addEventListener('click', async () => {
  const doctorId = $('#doctorServiceDoctorSelect').value;
  const clinicServiceId = $('#doctorServiceClinicServiceSelect').value;

  if (!doctorId || !clinicServiceId) {
    toast('Сначала выберите врача и услугу', 'error');
    return;
  }

  try {
    await api(
        `/api/doctors/${doctorId}/services/${clinicServiceId}`,
        { method: 'DELETE' }
    );

    toast('Услуга отвязана от врача', 'success');
    await loadAssignedDoctorServices(doctorId);
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#assignedDoctorServices').addEventListener('click', async event => {
  const button = event.target.closest('.assigned-service-remove');

  if (!button) {
    return;
  }

  try {
    await api(
        `/api/doctors/${button.dataset.doctorId}/services/${button.dataset.serviceId}`,
        { method: 'DELETE' }
    );

    toast('Услуга отвязана от врача', 'success');
    await loadAssignedDoctorServices(button.dataset.doctorId);
  } catch (error) {
    toast(error.message, 'error');
  }
});

async function loadStaffDoctors({ silent = false } = {}) {
  try {
    const doctors = await api('/api/doctors', {
      method: 'GET'
    });

    caches.doctors = new Map(
        doctors.map(doctor => [doctor.id, doctor])
    );

    const scheduleDoctorSelect = $('#doctorScheduleDoctorSelect');
    const selectedScheduleDoctorId = scheduleDoctorSelect?.value ?? '';

    if (scheduleDoctorSelect) {
      scheduleDoctorSelect.innerHTML = `
        <option value="">Выберите врача</option>
        ${doctors.map(doctor => {
          const fullName = [doctor.lastName, doctor.firstName, doctor.middleName]
            .filter(Boolean)
            .join(' ');
          return `<option value="${escapeHtml(doctor.id)}">${escapeHtml(fullName)} — ${escapeHtml(doctor.specialty ?? 'без специализации')}</option>`;
        }).join('')}
      `;

      if (doctors.some(doctor => String(doctor.id) === String(selectedScheduleDoctorId))) {
        scheduleDoctorSelect.value = selectedScheduleDoctorId;
      }
    }

    renderTable(
        $('#staffDoctors'),
        doctors,
        [
          {
            label: 'ФИО',
            render: doctor => `
              <div class="doctor-table-person">
                ${doctorAvatarHtml(doctor, 'doctor-avatar doctor-avatar-small')}
                <button class="link-button doctor-details" data-id="${doctor.id}">
                  ${escapeHtml(doctor.lastName)} ${escapeHtml(doctor.firstName)}
                </button>
              </div>
            `
          },
          { label: 'Специальность', key: 'specialty' },
          {
            label: 'Статус',
            render: doctor => `
              <span class="badge ${doctor.active ? 'success' : 'danger'}">
                ${doctor.active ? 'Активен' : 'Отключён'}
              </span>
            `
          }
        ],
        doctor => `
          <div class="actions">
            <button class="btn btn-success btn-sm doctor-book" data-id="${doctor.id}">Записать</button>
            <button class="btn btn-secondary btn-sm doctor-schedule-action" data-id="${doctor.id}">Расписание</button>
            <button class="btn btn-primary btn-sm doctor-edit" data-id="${doctor.id}">Редактировать</button>
            <button class="btn btn-secondary btn-sm doctor-toggle" data-id="${doctor.id}" data-active="${doctor.active}">
              ${doctor.active ? 'Отключить' : 'Активировать'}
            </button>
            ${doctor.userId ? `
              <button class="btn btn-ghost btn-sm doctor-unlink" data-id="${doctor.id}">
                Отвязать user
              </button>
            ` : `
              <button class="btn btn-ghost btn-sm doctor-link" type="button" data-id="${doctor.id}">
                Привязать user
              </button>
            `}
          </div>
        `
    );

    await loadDoctorServiceManagementOptions({ silent: true });

    if ($('#doctorScheduleDoctorSelect')?.value) {
      await loadDoctorSchedule($('#doctorScheduleDoctorSelect').value, { silent: true });
    }
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadStaffDoctors').addEventListener('click', () => {
  loadStaffDoctors();
});

$('#createDoctorForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;

  try {
    const body = formToObject(form);
    delete body.avatarFile;
    const avatarFile = form.querySelector('[name="avatarFile"]')?.files?.[0];
    if (avatarFile) body.avatarUrl = await uploadDoctorAvatar(avatarFile);

    await api('/api/doctors', {
      method: 'POST',
      body
    });

    toast('Врач создан', 'success');
    form.reset();
    closeStaffWorkflow();
    await loadStaffDoctors();
  } catch (error) {
    toast(error.message, 'error');
  }
});

setupDoctorAvatarForm(
    $('#createDoctorForm'),
    $('#createDoctorForm button[type="submit"], #createDoctorForm button:not([type])')
);

$('#doctorScheduleForm').addEventListener('submit', async event => {
  event.preventDefault();

  const form = event.currentTarget;
  const body = formToObject(form);
  const doctorId = body.doctorId;
  delete body.doctorId;

  try {
    const createdInterval = await api(`/api/doctors/${doctorId}/working-hours`, {
      method: 'POST',
      body
    });

    if (createdInterval?.id) {
      const current = doctorScheduleCache.get(String(doctorId)) || [];
      doctorScheduleCache.set(String(doctorId), [
        ...current.filter(item => String(item.id) !== String(createdInterval.id)),
        createdInterval
      ]);
      renderCachedDoctorSchedule(doctorId);
    }

    toast('Рабочие часы добавлены', 'success');
    form.querySelector('[name="dayOfWeek"]').value = body.dayOfWeek;
    form.querySelector('[name="doctorId"]').value = doctorId;
    form.querySelector('[name="startTime"]').value = '';
    form.querySelector('[name="endTime"]').value = '';
    if (!createdInterval?.id) {
      await loadDoctorSchedule(doctorId, { silent: true, force: true });
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#doctorScheduleDoctorSelect')?.addEventListener('change', event => {
  loadDoctorSchedule(event.currentTarget.value);
});

$('#doctorScheduleList')?.addEventListener('click', async event => {
  const editButton = event.target.closest('.schedule-edit');
  const deleteButton = event.target.closest('.schedule-delete');

  if (editButton) {
    openScheduleEdit(editButton);
    return;
  }

  if (deleteButton) {
    if (!window.confirm('Удалить этот рабочий интервал?')) {
      return;
    }

    try {
      await api(
          `/api/doctors/${deleteButton.dataset.doctorId}/working-hours/${deleteButton.dataset.scheduleId}`,
          { method: 'DELETE' }
      );
      const doctorId = deleteButton.dataset.doctorId;
      const current = doctorScheduleCache.get(String(doctorId)) || [];
      doctorScheduleCache.set(String(doctorId), current.filter(item =>
        String(item.id) !== String(deleteButton.dataset.scheduleId)
      ));
      renderCachedDoctorSchedule(doctorId);
      toast('Рабочий интервал удалён', 'error');
    } catch (error) {
      toast(error.message, 'error');
    }
  }
});


async function openDoctorUserLinkModal(doctorId) {
  openModal(
      'Привязать пользователя к врачу',
      `
        <div class="doctor-user-link-panel">
          <label class="full">
            Поиск по email или телефону
            <input id="doctorUserSearch" type="search" placeholder="Например, doctor@mail.ru или 79991234567" autocomplete="off">
          </label>
          <div id="doctorUserSearchResults" class="doctor-user-results">
            <p class="muted">Загрузка пользователей…</p>
          </div>
        </div>
      `
  );

  try {
    const users = await api('/api/users', { method: 'GET', cache: 'no-store' });
    const availableUsers = (Array.isArray(users) ? users : [])
      .filter(user => user.enabled !== false);

    const results = $('#doctorUserSearchResults');
    const search = $('#doctorUserSearch');

    const renderUsers = query => {
      const normalized = String(query || '').trim().toLowerCase();
      const digits = normalized.replace(/\D/g, '');

      const filtered = availableUsers.filter(user => {
        const email = String(user.email || '').toLowerCase();
        const phone = String(user.phone || '').replace(/\D/g, '');
        return !normalized
          || email.includes(normalized)
          || (digits && phone.includes(digits));
      });

      results.innerHTML = filtered.length
        ? filtered.map(user => {
            const fullName = [user.lastName, user.firstName, user.middleName]
              .filter(Boolean)
              .join(' ') || 'Без имени';
            return `
              <button class="doctor-user-result doctor-user-link-confirm" type="button"
                data-doctor-id="${escapeHtml(doctorId)}"
                data-user-email="${escapeHtml(user.email || '')}"
                data-user-phone="${escapeHtml(user.phone || user.phoneNumber || '')}">
                <span class="doctor-user-result-main">
                  <strong>${escapeHtml(fullName)}</strong>
                  <span>${escapeHtml(user.email || 'Email не указан')}</span>
                </span>
                <span class="doctor-user-result-phone">${escapeHtml(user.phone || 'Телефон не указан')}</span>
              </button>
            `;
          }).join('')
        : '<p class="muted">Пользователи не найдены.</p>';
    };

    renderUsers('');
    search?.addEventListener('input', event => renderUsers(event.currentTarget.value));
    search?.focus();
  } catch (error) {
    $('#doctorUserSearchResults').innerHTML = '<p class="muted">Не удалось загрузить пользователей клиники.</p>';
    toast(error.message, 'error');
  }
}

async function linkDoctorToUser(doctorId, email, phone) {
  const normalizedEmail = String(email || '').trim();
  const normalizedPhone = String(phone || '').replace(/\D/g, '');

  if (!normalizedEmail && !normalizedPhone) {
    throw new Error('У выбранного пользователя не указан email или телефон');
  }

  // Backend принимает LinkUserToDoctorRequestDto: { email, phone }.
  // Передаём одно надёжное уникальное поле: сначала email, иначе телефон.
  const body = normalizedEmail
    ? { email: normalizedEmail }
    : { phone: normalizedPhone };

  return api(`/api/doctors/${doctorId}/link-user`, {
    method: 'PATCH',
    body
  });
}

$('#staffDoctors').addEventListener('click', async event => {
  const details = event.target.closest('.doctor-details');
  const book = event.target.closest('.doctor-book');
  const scheduleAction = event.target.closest('.doctor-schedule-action');
  const servicesButton = event.target.closest('.doctor-services');
  const edit = event.target.closest('.doctor-edit');
  const toggle = event.target.closest('.doctor-toggle');
  const unlink = event.target.closest('.doctor-unlink');
  const link = event.target.closest('.doctor-link');

  try {
    if (book) { await openStaffWorkflow('create-appointment', { doctorId:book.dataset.id }); return; }
    if (scheduleAction) { await openStaffWorkflow('doctor-schedule', { doctorId:scheduleAction.dataset.id }); return; }
    if (details) {
      await getStaffDoctorById(details.dataset.id);
    }

    if (servicesButton) {
      $('#doctorServiceDoctorSelect').value = servicesButton.dataset.id;
      await loadAssignedDoctorServices(servicesButton.dataset.id);
      $('#doctorServiceManagement').scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }

    if (edit) {
      const doctor = caches.doctors.get(edit.dataset.id);
      if (doctor) {
        openDoctorEdit(doctor);
      }
    }

    if (toggle) {
      const action = toggle.dataset.active === 'true'
          ? 'deactivation'
          : 'activation';

      await api(`/api/doctors/${toggle.dataset.id}/${action}`, {
        method: 'PATCH'
      });

      await loadStaffDoctors();
    }

    if (link) {
      await openDoctorUserLinkModal(link.dataset.id);
    }

    if (unlink) {
      await api(`/api/doctors/${unlink.dataset.id}/unlink-user`, {
        method: 'PATCH'
      });

      await loadStaffDoctors();
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

function getCatalogServiceForClinicService(service) {
  return caches.dentalCatalog.get(String(service.dentalServiceId)) ?? null;
}

function getClinicServiceTitle(service) {
  const catalogService = getCatalogServiceForClinicService(service);
  return service.title ?? catalogService?.title ?? 'Без названия';
}

function getClinicServiceDescription(service) {
  const catalogService = getCatalogServiceForClinicService(service);
  return service.description ?? catalogService?.description ?? '—';
}

function showClinicServiceDetails(service) {
  openModal(
      getClinicServiceTitle(service),
      `
        <div class="detail-grid">
          ${detailItem('Название', getClinicServiceTitle(service), true)}
          ${detailItem('Описание', getClinicServiceDescription(service), true)}
          ${detailItem('Категория', getCatalogServiceForClinicService(service)?.category ?? service.category ?? '—')}
          ${detailItem('Цена', `${service.price} ₽`)}
          ${detailItem('Длительность', `${service.durationMinutes} мин`)}
          ${detailItem('Статус', service.active === false ? 'Отключена' : 'Активна')}
        </div>
      `,
      `
        <button class="btn btn-success service-create-appointment" data-id="${service.id}">+ Создать запись</button>
        <button class="btn btn-primary service-edit-modal" data-id="${service.id}">Редактировать</button>
      `
  );
}

function openClinicServiceEdit(service) {
  openModal(
      'Редактировать услугу клиники',
      `
        <form id="modalServiceEditForm" class="form-grid">
          <label>Цена<input name="price" required type="number" min="0" value="${service.price}"></label>
          <label>Длительность<input name="durationMinutes" required type="number" min="5" value="${service.durationMinutes}"></label>
        </form>
      `,
      `
        <button class="btn btn-primary service-save-modal" data-id="${service.id}">
          Сохранить
        </button>
      `
  );
}

async function loadDentalCatalogForClinicServices({ silent = false } = {}) {
  try {
    const services = await api('/api/catalog/dental-services', {
      method: 'GET'
    });

    caches.dentalCatalog = new Map(
        services.map(service => [String(service.id), service])
    );

    renderClinicServiceCatalogControls();
    renderDentalCatalogTable(services);
    return services;
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
    return [];
  }
}

function renderClinicServiceCatalogControls() {
  const services = [...caches.dentalCatalog.values()];
  const categorySelect = $('#clinicServiceCategoryFilter');
  const serviceSelect = $('#clinicServiceDentalServiceSelect');

  if (!categorySelect || !serviceSelect) {
    return;
  }

  const selectedCategory = categorySelect.value;
  const selectedServiceId = serviceSelect.value;
  const categories = [...new Set(services.map(service => service.category).filter(Boolean))].sort();

  categorySelect.innerHTML = `
    <option value="">Все категории</option>
    ${categories.map(category => `
      <option value="${escapeHtml(category)}">${escapeHtml(category)}</option>
    `).join('')}
  `;

  if (categories.includes(selectedCategory)) {
    categorySelect.value = selectedCategory;
  }

  const filteredServices = services
  .filter(service => !categorySelect.value || service.category === categorySelect.value)
  .sort((left, right) => String(left.title ?? '').localeCompare(String(right.title ?? ''), 'ru'));

  populateSelect(serviceSelect, filteredServices, {
    placeholder: filteredServices.length ? 'Выберите услугу' : 'Услуг в этой категории нет',
    getValue: service => service.id,
    getLabel: service => `${service.title}${service.description ? ` — ${service.description}` : ''}`
  });

  if (filteredServices.some(service => String(service.id) === selectedServiceId)) {
    serviceSelect.value = selectedServiceId;
  }

  updateSelectedClinicServiceDescription();
}

function updateSelectedClinicServiceDescription() {
  const serviceId = $('#clinicServiceDentalServiceSelect')?.value;
  const description = $('#clinicServiceSelectedDescription');
  const service = caches.dentalCatalog.get(String(serviceId));

  if (!description) {
    return;
  }

  description.textContent = service
      ? `${service.title}${service.category ? ` · ${service.category}` : ''}${service.description ? ` — ${service.description}` : ''}`
      : 'Выберите услугу, чтобы увидеть описание.';
}

function renderDentalCatalogTable(services = [...caches.dentalCatalog.values()]) {
  renderTable(
      $('#dentalCatalog'),
      services,
      [
        { label: 'Название', key: 'title' },
        { label: 'Категория', key: 'category' },
        { label: 'Описание', key: 'description' },
        {
          label: 'ID',
          render: service => `<span class="code">${service.id}</span>`
        }
      ]
  );
}

function renderServiceStatusGroups(container, services, renderGroupContent) {
  if (!container) {
    return;
  }

  const previouslyOpened = new Set(
      [...container.querySelectorAll('.service-status-group[open]')]
      .map(group => group.dataset.status)
  );

  const groups = [
    {
      status: 'active',
      title: 'Активированные услуги',
      items: services.filter(service => service.active !== false),
      openByDefault: true
    },
    {
      status: 'inactive',
      title: 'Отключённые услуги',
      items: services.filter(service => service.active === false),
      openByDefault: false
    }
  ];

  container.innerHTML = '';
  let rendered = false;

  groups.forEach(group => {
    if (!group.items.length) {
      return;
    }

    rendered = true;
    const details = document.createElement('details');
    details.className = 'appointment-group service-status-group';
    details.dataset.status = group.status;
    details.open = previouslyOpened.has(group.status)
        || (!previouslyOpened.size && group.openByDefault);

    const summary = document.createElement('summary');
    summary.innerHTML = `
      <span>${escapeHtml(group.title)}</span>
      <span class="badge ${group.status === 'active' ? 'success' : 'danger'}">${group.items.length}</span>
    `;

    const content = document.createElement('div');
    content.className = 'appointment-group-content';
    details.append(summary, content);
    container.append(details);
    renderGroupContent(content, group.items, group.status);
  });

  if (!rendered) {
    container.innerHTML = '<div class="card empty">Услуг пока нет</div>';
  }
}

async function loadStaffServices({ silent = false } = {}) {
  try {
    const [services] = await Promise.all([
      api('/api/services', { method: 'GET' }),
      loadDentalCatalogForClinicServices({ silent: true })
    ]);

    caches.services = new Map(
        services.map(service => [service.id, service])
    );

    renderServiceStatusGroups(
        $('#staffServices'),
        services,
        (container, groupServices) => renderTable(
            container,
            groupServices,
            [
              {
                label: 'Услуга',
                render: service => `
                  <button class="link-button service-details" data-id="${service.id}">
                    ${escapeHtml(getClinicServiceTitle(service))}
                  </button>
                `
              },
              {
                label: 'Описание',
                render: service => escapeHtml(getClinicServiceDescription(service))
              },
              {
                label: 'Категория',
                render: service => escapeHtml(getCatalogServiceForClinicService(service)?.category ?? service.category ?? '—')
              },
              {
                label: 'Цена',
                render: service => `${service.price} ₽`
              },
              {
                label: 'Длительность',
                render: service => `${service.durationMinutes} мин`
              },
              {
                label: 'Статус',
                render: service => `
                  <span class="badge ${service.active === false ? 'danger' : 'success'}">
                    ${service.active === false ? 'Отключена' : 'Активна'}
                  </span>
                `
              }
            ],
            service => `
              <div class="actions">
                <button class="btn btn-primary btn-sm service-edit" data-id="${service.id}">
                  Редактировать
                </button>
                ${service.active === false ? `
                  <button class="btn btn-success btn-sm service-activate" data-id="${service.id}">
                    Активировать
                  </button>
                ` : `
                  <button class="btn btn-danger btn-sm service-deactivate" data-id="${service.id}">
                    Отключить
                  </button>
                `}
              </div>
            `
        )
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadStaffServices').addEventListener('click', () => {
  loadStaffServices();
});

$('#addClinicServiceForm').addEventListener('submit', async event => {
  event.preventDefault();

  const form = event.currentTarget;
  const body = formToObject(form);
  const dentalServiceId = body.dentalServiceId;
  delete body.dentalServiceId;
  body.price = Number(body.price);
  body.durationMinutes = Number(body.durationMinutes);

  try {
    await api(`/api/services/${dentalServiceId}`, {
      method: 'POST',
      body
    });

    toast('Услуга подключена', 'success');
    form.reset();
    closeStaffWorkflow();
    await loadStaffServices();
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#loadDentalCatalog').addEventListener('click', () => {
  loadDentalCatalogForClinicServices();
});

$('#clinicServiceCategoryFilter').addEventListener('change', () => {
  renderClinicServiceCatalogControls();
});

$('#clinicServiceDentalServiceSelect').addEventListener('change', () => {
  updateSelectedClinicServiceDescription();
});

$('#staffServices').addEventListener('click', async event => {
  const details = event.target.closest('.service-details');
  const edit = event.target.closest('.service-edit');
  const deactivate = event.target.closest('.service-deactivate');
  const activate = event.target.closest('.service-activate');

  try {
    if (details) {
      const service = caches.services.get(details.dataset.id);
      if (service) {
        showClinicServiceDetails(service);
      }
    }

    if (edit) {
      const service = caches.services.get(edit.dataset.id);
      if (service) {
        openClinicServiceEdit(service);
      }
    }

    if (deactivate) {
      const serviceId = deactivate.dataset.id;
      const updated = await api(`/api/services/${serviceId}/deactivation`, {
        method: 'PATCH'
      });

      const cached = caches.services.get(serviceId);
      if (cached) {
        Object.assign(cached, updated && typeof updated === 'object' ? updated : {}, { active: false });
      }

      toast('Услуга отключена', 'error');
      await loadStaffServices();
    }

    if (activate) {
      const serviceId = activate.dataset.id;
      const updated = await api(`/api/services/${serviceId}/activation`, {
        method: 'PATCH'
      });

      const cached = caches.services.get(serviceId);
      if (cached) {
        Object.assign(cached, updated && typeof updated === 'object' ? updated : {}, { active: true });
      }

      toast('Услуга активирована', 'success');
      await loadStaffServices();
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

function staffAppointmentStatusSelectHtml(appointment, className = 'modal-appointment-status') {
  const current = String(appointment?.status || '').toUpperCase();
  return `
    <label class="appointment-status-control">Статус
      <select class="${className}" data-id="${escapeHtml(appointment.id)}">
        <option value="CREATED" ${current === 'CREATED' ? 'selected' : ''}>Создана</option>
        <option value="CONFIRMED" ${current === 'CONFIRMED' ? 'selected' : ''}>Подтверждена</option>
        <option value="COMPLETED" ${current === 'COMPLETED' ? 'selected' : ''}>Завершена</option>
        <option value="CANCELLED" ${current === 'CANCELLED' ? 'selected' : ''}>Отменена</option>
      </select>
    </label>
  `;
}

async function getStaffAppointmentById(appointmentId) {
  const appointment = await api(`/api/appointments/${appointmentId}`, {
    method: 'GET'
  });

  caches.appointments.set(appointment.id, appointment);
  openModal(
      'Запись клиники',
      appointmentDetailsHtml(appointment),
      `
        <button class="btn btn-secondary appointment-open-patient" data-patient-id="${escapeHtml(appointment.patientId || '')}">Пациент</button>
        <button class="btn btn-secondary appointment-open-doctor" data-doctor-id="${escapeHtml(appointment.doctorId || '')}">Врач</button>
        ${staffAppointmentStatusSelectHtml(appointment)}
        <button class="btn btn-primary appointment-modal-change" data-id="${escapeHtml(appointment.id)}">Сохранить статус</button>
        ${String(appointment.status || '').toUpperCase() === 'CANCELLED' ? '' : `<button class="btn btn-danger appointment-modal-cancel" data-id="${escapeHtml(appointment.id)}">Отменить запись</button>`}
      `
  );
}

function renderStaffAppointmentList(appointments) {
  const pendingStatuses = new Map(
      $$('.appointment-status[data-user-dirty="true"]').map(select => [String(select.dataset.id), select.value])
  );

  caches.appointments = new Map(
      appointments.map(appointment => [appointment.id, appointment])
  );

  renderAppointmentGroups(
      $('#staffAppointments'),
      appointments,
      [
        {
          label: 'Дата',
          render: appointment => `
            <button class="link-button staff-appointment-link" data-id="${appointment.id}">
              ${escapeHtml(formatDate(appointment.scheduledAt))}
            </button>
          `
        },
        {
          label: 'Пациент',
          render: appointment => { const patient = caches.patients.get(String(appointment.patientId)) || caches.patients.get(appointment.patientId); return escapeHtml(patient ? [patient.lastName, patient.firstName].filter(Boolean).join(' ') : appointment.patientId); }
        },
        {
          label: 'Врач',
          render: appointment => escapeHtml(getDoctorDisplayName(appointment))
        },
        {
          label: 'Цена',
          render: appointment => `${appointment.totalPrice} ₽`
        },
        {
          label: 'Статус',
          render: appointment => `<span class="badge">${escapeHtml(appointmentStatusLabel(appointment.status))}</span>`
        }
      ],
      appointment => `
        <div class="actions">
          <select class="appointment-status" data-id="${appointment.id}">
            <option value="CREATED" ${appointment.status === 'CREATED' ? 'selected' : ''}>Создана</option>
            <option value="CONFIRMED" ${appointment.status === 'CONFIRMED' ? 'selected' : ''}>Подтверждена</option>
            <option value="COMPLETED" ${appointment.status === 'COMPLETED' ? 'selected' : ''}>Завершена</option>
            <option value="CANCELLED" ${appointment.status === 'CANCELLED' ? 'selected' : ''}>Отменена</option>
          </select>
          <button class="btn btn-primary btn-sm appointment-change" data-id="${appointment.id}">Изменить</button>
          ${String(appointment.status || '').toUpperCase() === 'CANCELLED' ? '' : `
            <button class="btn btn-danger btn-sm staff-cancel" data-id="${appointment.id}">Отменить</button>
          `}
        </div>
      `
  );

  pendingStatuses.forEach((value, id) => {
    const select = document.querySelector(`.appointment-status[data-id="${CSS.escape(id)}"]`);
    if (!select) return;
    select.value = value;
    select.dataset.userDirty = 'true';
  });
}

async function refreshStaffAppointmentListOnly({ silent = true } = {}) {
  try {
    const appointments = await api('/api/appointments', { method: 'GET' });
    renderStaffAppointmentList(appointments);
  } catch (error) {
    if (!silent) toast(error.message, 'error');
    else console.warn('Не удалось обновить список записей:', error);
  }
}

async function loadStaffAppointments({ silent = false } = {}) {
  try {
    const [appointments, doctors, services, patients] = await Promise.all([
      api('/api/appointments', { method: 'GET' }),
      api('/api/doctors', { method: 'GET' }),
      api('/api/services', { method: 'GET' }),
      api('/api/patients', { method: 'GET' })
    ]);

    const activeDoctors = doctors.filter(doctor => doctor.active !== false);
    const activeServices = services.filter(service => service.active !== false);

    caches.doctors = new Map(
        doctors.map(doctor => [doctor.id, doctor])
    );

    caches.services = new Map(
        services.map(service => [service.id, service])
    );

    populateStaffAppointmentSelects(
        activeDoctors,
        activeServices
    );

    renderStaffAppointmentList(appointments);
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadStaffAppointments').addEventListener('click', () => {
  loadStaffAppointments();
});


$('#staffPatientSearch')?.addEventListener('input', event => {
  const query = String(event.currentTarget.value || '').trim().toLowerCase();
  const target = $('#staffPatientSuggestions');
  $('#staffPatientId').value = '';
  if (!target || query.length < 2) { target?.classList.add('hidden'); return; }
  const matches = [...caches.patients.values()].filter(patient => {
    const hay = [patient.lastName, patient.firstName, patient.middleName, patient.phone, patient.email].filter(Boolean).join(' ').toLowerCase();
    return hay.includes(query);
  }).slice(0, 8);
  target.innerHTML = matches.length ? matches.map(patient => `<button type="button" data-patient-pick="${escapeHtml(patient.id)}"><strong>${escapeHtml([patient.lastName, patient.firstName, patient.middleName].filter(Boolean).join(' '))}</strong><small>${escapeHtml(patient.phone || patient.email || '')}</small></button>`).join('') : '<div class="staff-patient-no-result">Ничего не найдено</div>';
  target.classList.remove('hidden');
});

$('#staffPatientSuggestions')?.addEventListener('click', event => {
  const button = event.target.closest('[data-patient-pick]');
  if (!button) return;
  const patient = caches.patients.get(String(button.dataset.patientPick)) || caches.patients.get(button.dataset.patientPick);
  setStaffPatientSelection(patient || null);
});

$('#staffAppointmentForm').addEventListener('submit', async event => {
  event.preventDefault();

  const form = event.currentTarget;
  const values = formToObject(form);
  const patientId = values.patientId;
  const scheduledAt = combineDateAndTime(
      values.appointmentDate,
      values.appointmentTime
  );

  if (!scheduledAt) {
    toast('Выберите дату и время записи', 'error');
    return;
  }

  try {
    await api(`/api/appointments/patients/${patientId}`, {
      method: 'POST',
      body: {
        doctorId: values.doctorId,
        scheduledAt,
        services: [
          {
            clinicServiceId: values.clinicServiceId,
            quantity: Number(values.quantity || 1)
          }
        ],
        comment: values.comment || null
      }
    });

    toast('Запись создана', 'success');
    form.reset();
    resetAvailabilityControls('staff');
    closeStaffWorkflow();
    await loadStaffAppointments();
    if (getCurrentViewName() === 'staff-dashboard') await refreshStaffDashboard({ silent:true });
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#staffAppointments').addEventListener('change', event => {
  const select = event.target.closest('.appointment-status');
  if (select) select.dataset.userDirty = 'true';
});

$('#staffAppointments').addEventListener('click', async event => {
  const link = event.target.closest('.staff-appointment-link');
  const change = event.target.closest('.appointment-change');
  const cancel = event.target.closest('.staff-cancel');

  try {
    if (link) {
      await getStaffAppointmentById(link.dataset.id);
    }

    if (change) {
      const select = $(`.appointment-status[data-id="${change.dataset.id}"]`);

      await api(`/api/appointments/${change.dataset.id}/change`, {
        method: 'PATCH',
        body: {
          status: select.value
        }
      });

      await loadStaffAppointments();
    }

    if (cancel) {
      await api(`/api/appointments/${cancel.dataset.id}/cancel`, {
        method: 'PATCH'
      });

      await loadStaffAppointments();
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

async function getUserById(userId) {
  const user = await api(`/api/users/${userId}`, {
    method: 'GET'
  });

  openModal(
      `${user.lastName} ${user.firstName}`,
      `
        <div class="detail-grid">
          ${detailItem('Email', user.email)}
          ${detailItem('Телефон', user.phone)}
          ${detailItem('Роли', (user.roles || []).join(', '), true)}
          ${detailItem('Статус', user.enabled ? 'Активен' : 'Отключён')}
        </div>
      `,
      `<button class="btn btn-success user-create-appointment" data-user-id="${user.id}">+ Создать запись</button>`
  );
}

async function loadStaffUsers({ silent = false } = {}) {
  try {
    const [users, patients] = await Promise.all([
      api('/api/users', { method: 'GET' }),
      api('/api/patients', { method: 'GET' })
    ]);

    caches.patients = new Map(patients.map(patient => [patient.id, patient]));

    caches.users = new Map(
        users.map(user => [user.id, user])
    );

    renderTable(
        $('#staffUsers'),
        users,
        [
          {
            label: 'ФИО',
            render: user => `
              <button class="link-button user-details" data-id="${user.id}">
                ${escapeHtml(user.lastName)} ${escapeHtml(user.firstName)}
              </button>
            `
          },
          { label: 'Email', key: 'email' },
          {
            label: 'Роли',
            render: user => escapeHtml((user.roles || []).join(', '))
          },
          {
            label: 'Статус',
            render: user => `
              <span class="badge ${user.enabled ? 'success' : 'danger'}">
                ${user.enabled ? 'Активен' : 'Отключён'}
              </span>
            `
          }
        ],
        user => `
          <button class="btn btn-secondary btn-sm user-toggle" data-id="${user.id}" data-enabled="${user.enabled}">
            ${user.enabled ? 'Отключить' : 'Активировать'}
          </button>
        `
    );
  } catch (error) {
    if (!silent) {
      toast(error.message, 'error');
    }
  }
}

$('#loadStaffUsers').addEventListener('click', () => {
  loadStaffUsers();
});

$('#staffUsers').addEventListener('click', async event => {
  const details = event.target.closest('.user-details');
  const toggle = event.target.closest('.user-toggle');

  try {
    if (details) {
      await getUserById(details.dataset.id);
    }

    if (toggle) {
      const action = toggle.dataset.enabled === 'true'
          ? 'deactivation'
          : 'activation';

      await api(`/api/users/${toggle.dataset.id}/${action}`, {
        method: 'PATCH'
      });

      await loadStaffUsers();
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});

$('#detailModalActions').addEventListener('click', async event => {
  try {
    const appointmentChange = event.target.closest('.appointment-modal-change');
    const appointmentCancel = event.target.closest('.appointment-modal-cancel');
    const appointmentPatient = event.target.closest('.appointment-open-patient');
    const appointmentDoctor = event.target.closest('.appointment-open-doctor');

    if (appointmentPatient?.dataset.patientId) {
      let patient = caches.patients.get(String(appointmentPatient.dataset.patientId)) || caches.patients.get(appointmentPatient.dataset.patientId);
      if (!patient) patient = await api(`/api/patients/${appointmentPatient.dataset.patientId}`, { method:'GET' });
      if (patient) { caches.patients.set(patient.id, patient); showPatientDetails(patient); }
      return;
    }
    if (appointmentDoctor?.dataset.doctorId) { await getStaffDoctorById(appointmentDoctor.dataset.doctorId); return; }

    if (appointmentChange) {
      const select = $(`.modal-appointment-status[data-id="${appointmentChange.dataset.id}"]`);
      if (!select) throw new Error('Не удалось определить новый статус записи');
      await api(`/api/appointments/${appointmentChange.dataset.id}/change`, {
        method: 'PATCH',
        body: { status: select.value }
      });
      closeModal();
      toast('Статус записи обновлён', 'success');
      if (getCurrentViewName() === 'staff-dashboard') await refreshStaffDashboard({ silent: true });
      else await refreshStaffAppointmentListOnly({ silent: true });
      return;
    }

    if (appointmentCancel) {
      await api(`/api/appointments/${appointmentCancel.dataset.id}/cancel`, { method: 'PATCH' });
      closeModal();
      toast('Запись отменена', 'success');
      if (getCurrentViewName() === 'staff-dashboard') await refreshStaffDashboard({ silent: true });
      else await refreshStaffAppointmentListOnly({ silent: true });
      return;
    }

    const patientBook = event.target.closest('.patient-create-appointment');
    const doctorBook = event.target.closest('.doctor-create-appointment');
    const doctorSchedule = event.target.closest('.doctor-schedule-modal');
    const doctorServices = event.target.closest('.doctor-services-modal');
    const serviceBook = event.target.closest('.service-create-appointment');
    const userBook = event.target.closest('.user-create-appointment');

    if (patientBook) { closeModal(); await openStaffWorkflow('create-appointment', { patientId:patientBook.dataset.id }); return; }
    if (doctorBook) { closeModal(); await openStaffWorkflow('create-appointment', { doctorId:doctorBook.dataset.id }); return; }
    if (doctorSchedule) { closeModal(); await openStaffWorkflow('doctor-schedule', { doctorId:doctorSchedule.dataset.id }); return; }
    if (doctorServices) { closeModal(); await openStaffWorkflow('doctor-services', { doctorId:doctorServices.dataset.id }); return; }
    if (serviceBook) { closeModal(); await openStaffWorkflow('create-appointment', { serviceId:serviceBook.dataset.id }); return; }
    if (userBook) {
      let patient = [...caches.patients.values()].find(item => String(item.userId) === String(userBook.dataset.userId));
      if (!patient) {
        const patients = await api('/api/patients', { method:'GET' });
        caches.patients = new Map(patients.map(item => [item.id, item]));
        patient = patients.find(item => String(item.userId) === String(userBook.dataset.userId));
      }
      if (!patient) { toast('У пользователя пока нет карточки пациента', 'error'); return; }
      closeModal(); await openStaffWorkflow('create-appointment', { patientId:patient.id }); return;
    }

    const patientEdit = event.target.closest('.patient-edit-modal');
    const patientSave = event.target.closest('.patient-save-modal');
    const doctorEdit = event.target.closest('.doctor-edit-modal');
    const doctorSave = event.target.closest('.doctor-save-modal');
    const serviceEdit = event.target.closest('.service-edit-modal');
    const serviceSave = event.target.closest('.service-save-modal');

    if (patientEdit) {
      const patient = caches.patients.get(patientEdit.dataset.id);
      if (patient) {
        openPatientEdit(patient);
      }
    }

    if (patientSave) {
      await api(`/api/patients/${patientSave.dataset.id}`, {
        method: 'PATCH',
        body: formToObject($('#modalPatientEditForm'))
      });

      closeModal();
      toast('Пациент обновлён', 'success');
      await loadStaffPatients();
    }

    const scheduleSave = event.target.closest('.schedule-save-modal');

    if (scheduleSave) {
      const body = formToObject($('#modalScheduleEditForm'));

      const updatedInterval = await api(
          `/api/doctors/${scheduleSave.dataset.doctorId}/working-hours/${scheduleSave.dataset.scheduleId}`,
          { method: 'PATCH', body }
      );

      if (updatedInterval?.id) {
        const doctorId = scheduleSave.dataset.doctorId;
        const current = doctorScheduleCache.get(String(doctorId)) || [];
        doctorScheduleCache.set(String(doctorId), current.map(item =>
          String(item.id) === String(updatedInterval.id) ? updatedInterval : item
        ));
        renderCachedDoctorSchedule(doctorId);
      }

      closeModal();
      toast('Рабочий интервал обновлён', 'success');
      if (!updatedInterval?.id) {
        await loadDoctorSchedule(scheduleSave.dataset.doctorId, { silent: true, force: true });
      }
      return;
    }

    if (doctorEdit) {
      const doctor = caches.doctors.get(doctorEdit.dataset.id);
      if (doctor) {
        openDoctorEdit(doctor);
      }
    }

    if (doctorSave) {
      const form = $('#modalDoctorEditForm');
      const body = formToObject(form);
      delete body.avatarFile;
      const avatarFile = form.querySelector('[name="avatarFile"]')?.files?.[0];
      if (avatarFile) body.avatarUrl = await uploadDoctorAvatar(avatarFile);

      await api(`/api/doctors/${doctorSave.dataset.id}`, {
        method: 'PATCH',
        body
      });

      closeModal();
      toast('Врач обновлён', 'success');
      await loadStaffDoctors();
    }

    if (serviceEdit) {
      const service = caches.services.get(serviceEdit.dataset.id);
      if (service) {
        openClinicServiceEdit(service);
      }
    }

    if (serviceSave) {
      const body = formToObject($('#modalServiceEditForm'));
      body.price = Number(body.price);
      body.durationMinutes = Number(body.durationMinutes);

      await api(`/api/services/${serviceSave.dataset.id}`, {
        method: 'PATCH',
        body
      });

      closeModal();
      toast('Услуга обновлена', 'success');
      await loadStaffServices();
    }
  } catch (error) {
    toast(error.message, 'error');
  }
});


const staffListObserver = new MutationObserver(mutations => {
  for (const mutation of mutations) {
    const host = mutation.target.closest?.('#staffPatients,#staffDoctors,#staffServices,#staffAppointments,#staffUsers') || (mutation.target.matches?.('#staffPatients,#staffDoctors,#staffServices,#staffAppointments,#staffUsers') ? mutation.target : null);
    if (host?.id) applyStaffTableFilter(host.id);
  }
});
['staffPatients','staffDoctors','staffServices','staffAppointments','staffUsers'].forEach(id => { const el=document.getElementById(id); if (el) staffListObserver.observe(el,{childList:true,subtree:true}); });

const viewLoaders = {
  'public-doctors': loadPublicDoctors,
  'public-services': loadPublicServices,
  'patient-card': loadPatientCard,
  'patient-appointments': async options => {
    await Promise.all([
      loadPublicDoctors({ silent: true }),
      loadPublicServices({ silent: true }),
      loadPatientAppointments(options)
    ]);
  },
  'patient-notifications': loadNotifications,
  'profile': loadProfile,
  'staff-dashboard': refreshStaffDashboard,
  'staff-patients': loadStaffPatients,
  'staff-doctors': loadStaffDoctors,
  'staff-services': loadStaffServices,
  'staff-appointments': loadStaffAppointments,
  'staff-users': loadStaffUsers
};

function getCurrentViewName() {
  const activeView = $('.view.active');

  return activeView
      ? activeView.id.replace('view-', '')
      : null;
}

function isUserEditingCurrentView() {
  const activeView = $('.view.active');
  const activeElement = document.activeElement;
  const modalIsOpen = Boolean(
      document.querySelector('.modal:not(.hidden), [role="dialog"]:not(.hidden)')
  );
  const focusedControlIsInsideView = Boolean(
      activeView
      && activeElement
      && activeView.contains(activeElement)
      && activeElement.matches('input, select, textarea, button, [contenteditable="true"]')
  );
  const dirtyFormExists = Boolean(
      activeView?.querySelector('form[data-user-dirty="true"]')
  );

  return modalIsOpen || focusedControlIsInsideView || dirtyFormExists;
}

function bindDirtyFormTracking() {
  document.addEventListener('input', event => {
    const form = event.target.closest?.('form');
    if (form) form.dataset.userDirty = 'true';
  });

  document.addEventListener('change', event => {
    const form = event.target.closest?.('form');
    if (form) form.dataset.userDirty = 'true';
  });

  document.addEventListener('reset', event => {
    if (event.target instanceof HTMLFormElement) {
      delete event.target.dataset.userDirty;
    }
  });

  document.addEventListener('submit', event => {
    if (event.target instanceof HTMLFormElement) {
      delete event.target.dataset.userDirty;
    }
  });
}

const viewLoadPromises = new Map();

async function loadView(viewName, { silent = true } = {}) {
  if (!viewName) {
    return;
  }

  const loader = viewLoaders[viewName];

  if (!loader) {
    return;
  }

  const view = $(`#view-${viewName}`);

  if (view?.dataset.access && !hasAccess(view.dataset.access)) {
    return;
  }

  // Не запускаем два одинаковых запроса параллельно. При этом загрузки
  // разных разделов друг друга не блокируют, поэтому быстрый переход между
  // страницами больше не оставляет новый раздел пустым.
  const existingPromise = viewLoadPromises.get(viewName);
  if (existingPromise) {
    return existingPromise;
  }

  const loadPromise = (async () => {
    try {
      await loader({ silent });
    } catch (error) {
      console.error(
          silent
              ? 'Ошибка автоматического обновления:'
              : 'Ошибка загрузки раздела:',
          error
      );
    } finally {
      viewLoadPromises.delete(viewName);
    }
  })();

  viewLoadPromises.set(viewName, loadPromise);
  return loadPromise;
}

async function refreshCurrentView() {
  if (refreshInProgress || document.hidden || isUserEditingCurrentView()) {
    return;
  }

  const viewName = getCurrentViewName();

  if (!AUTO_REFRESH_VIEWS.has(viewName)) {
    return;
  }

  refreshInProgress = true;

  try {
    await loadView(viewName, { silent: true });
  } finally {
    refreshInProgress = false;
  }
}

function startAutoRefresh() {
  stopAutoRefresh();

  autoRefreshTimer = window.setInterval(
      refreshCurrentView,
      AUTO_REFRESH_INTERVAL_MS
  );

  refreshCurrentView();
}

function stopAutoRefresh() {
  if (autoRefreshTimer) {
    window.clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }
}

document.addEventListener('visibilitychange', () => {
  if (!document.hidden) {
    refreshCurrentView();
  }
});

async function initialize() {
  // Ошибка декоративной разметки обязательных полей не должна останавливать
  // восстановление JWT-сессии и инициализацию всего приложения.
  try {
    initPatientBookingStepper();
setupRequiredFieldMarkers();
  } catch (error) {
    console.warn('Required-field markers were not initialized:', error);
  }
  ensureSidebarContextControls();
  syncSidebarContext(viewFromPathname());
  bindDirtyFormTracking();

bindAppointmentAvailability('patient');
bindAppointmentAvailability('staff');

  const ssrCatalog = hydratePublicCatalogFromSsr();

  // Сначала восстанавливаем сохранённую сессию и права, затем рисуем UI.
  // JWT хранится в localStorage и должен переживать обычное обновление страницы.
  await checkConnection();
  await resolveAccessProfile();
  updateAuthState();

const initialView = viewFromPathname();

  if (initialView && $(`#view-${initialView}`)) {
    if ((initialView === 'login' || initialView === 'register') && getToken()) {
      if (accessProfile.patient) {
        showView('profile', { updateUrl:true });
      } else {
        showView('home', { updateUrl:true });
      }
    } else {
      const initialCatalogWasRendered =
          (initialView === 'public-doctors' && ssrCatalog.doctors)
          || (initialView === 'public-services' && ssrCatalog.services);
      showView(initialView, {
        updateUrl: false,
        load: !initialCatalogWasRendered
      });
    }
  }

  if (getToken()) {
    startAutoRefresh();
  }
}


// --- Near real-time appointment updates ------------------------------------
// The current Spring API has no native event stream yet. The Next.js server
// keeps a single SSE connection open to this browser and checks appointments
// server-side. When the snapshot changes, only an event is pushed to the UI.
// Native Spring SSE/RabbitMQ events can replace this endpoint later without
// changing the UI contract.
let appointmentLiveController = null;
let appointmentLiveReconnectTimer = null;
let appointmentLiveFallbackTimer = null;
let appointmentLiveGeneration = 0;
const APPOINTMENT_LIVE_FALLBACK_MS = 2000;

function ensureAppointmentLiveBadge() {
  let badge = document.getElementById('appointmentsLiveStatus');
  if (badge) return badge;

  const head = document.querySelector('#view-staff-appointments .section-head');
  if (!head) return null;

  badge = document.createElement('span');
  badge.id = 'appointmentsLiveStatus';
  badge.className = 'badge live-status live-status-offline';
  badge.textContent = 'LIVE: отключено';

  const refreshButton = head.querySelector('#loadStaffAppointments');
  if (refreshButton) {
    const actions = document.createElement('div');
    actions.className = 'actions';
    refreshButton.replaceWith(actions);
    actions.append(badge, refreshButton);
  } else {
    head.appendChild(badge);
  }

  return badge;
}

function setAppointmentLiveState(state, text) {
  const badge = ensureAppointmentLiveBadge();
  if (!badge) return;
  badge.classList.remove('live-status-online', 'live-status-connecting', 'live-status-offline');
  badge.classList.add(`live-status-${state}`);
  badge.textContent = text;
}

function stopAppointmentLiveUpdates() {
  appointmentLiveGeneration += 1;
  if (appointmentLiveController) {
    appointmentLiveController.abort();
    appointmentLiveController = null;
  }
  if (appointmentLiveReconnectTimer) {
    window.clearTimeout(appointmentLiveReconnectTimer);
    appointmentLiveReconnectTimer = null;
  }
  if (appointmentLiveFallbackTimer) {
    window.clearInterval(appointmentLiveFallbackTimer);
    appointmentLiveFallbackTimer = null;
  }
  setAppointmentLiveState('offline', 'LIVE: отключено');
}

async function handleAppointmentLiveChange() {
  if (document.hidden) return;

  const currentView = getCurrentViewName();
  if (currentView === 'staff-appointments') {
    // Refresh only the list: never overwrite a form that the employee is filling in.
    await refreshStaffAppointmentListOnly({ silent: true });
  } else if (currentView === 'staff-dashboard') {
    await refreshStaffDashboard({ silent: true });
  }
}

function scheduleAppointmentLiveReconnect(generation) {
  if (generation !== appointmentLiveGeneration || !getToken()) return;
  appointmentLiveReconnectTimer = window.setTimeout(() => {
    startAppointmentLiveUpdates();
  }, 2500);
}

async function startAppointmentLiveUpdates() {
  const token = getToken();
  if (!token || !(accessProfile['staff-core'] || accessProfile['staff-appointments'])) {
    stopAppointmentLiveUpdates();
    return;
  }

  if (appointmentLiveController) appointmentLiveController.abort();
  if (appointmentLiveReconnectTimer) window.clearTimeout(appointmentLiveReconnectTimer);

  const generation = ++appointmentLiveGeneration;
  const controller = new AbortController();
  appointmentLiveController = controller;
  setAppointmentLiveState('connecting', 'LIVE: подключение…');

  if (appointmentLiveFallbackTimer) window.clearInterval(appointmentLiveFallbackTimer);
  appointmentLiveFallbackTimer = window.setInterval(() => {
    if (document.hidden) return;
    const currentView = getCurrentViewName();
    if (currentView === 'staff-appointments') {
      void refreshStaffAppointmentListOnly({ silent: true });
    } else if (currentView === 'staff-dashboard') {
      void refreshStaffDashboard({ silent: true });
    }
  }, APPOINTMENT_LIVE_FALLBACK_MS);

  try {
    const response = await fetch('/live/appointments', {
      method: 'GET',
      headers: {
        'Accept': 'text/event-stream',
        'Authorization': `Bearer ${token}`
      },
      cache: 'no-store',
      signal: controller.signal
    });

    if (!response.ok || !response.body) {
      throw new Error(`Live stream HTTP ${response.status}`);
    }

    setAppointmentLiveState('online', 'LIVE: записи обновляются');

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (generation === appointmentLiveGeneration) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let separatorIndex;
      while ((separatorIndex = buffer.indexOf('\n\n')) !== -1) {
        const block = buffer.slice(0, separatorIndex);
        buffer = buffer.slice(separatorIndex + 2);
        const eventLine = block.split('\n').find(line => line.startsWith('event:'));
        const eventName = eventLine?.slice(6).trim();

        if (eventName === 'appointments-changed') {
          await handleAppointmentLiveChange();
        } else if (eventName === 'auth-error') {
          throw new Error('Live stream authorization failed');
        }
      }
    }
  } catch (error) {
    if (controller.signal.aborted || generation !== appointmentLiveGeneration) return;
    console.warn('Appointment live stream disconnected:', error);
    setAppointmentLiveState('offline', 'LIVE: переподключение…');
    scheduleAppointmentLiveReconnect(generation);
  }
}


initialize().then(() => {
  startAppointmentLiveUpdates();
});

import Script from 'next/script';
import { notFound } from 'next/navigation';
import { demoShell } from '../../lib/shells';
import { withThemeToggle } from '../../lib/theme-toggle';
import {
  loadInitialPublicCatalog,
  renderDoctorsHtml,
  renderServicesHtml,
  serializeCatalogSeed
} from '../../lib/public-catalog';

export const dynamic = 'force-dynamic';

const SITE = 'https://demo.dent.bublapi.ru';
const SIDEBAR_CONTEXT_CONTROLS = `
<div class="nav-group sidebar-context-entry hidden" data-access="staff-core,staff-appointments">
  <div class="nav-title">Рабочая зона</div>
  <button class="nav-button context-button context-button-staff" id="sidebarEnterStaff" type="button">
    <span>Панель сотрудника</span>
    <small>Пациенты, врачи и записи</small>
  </button>
</div>
<div class="nav-group sidebar-context-exit hidden">
  <button class="nav-button context-button context-button-patient" id="sidebarExitStaff" type="button">
    <span>К кабинету пациента</span>
    <small>Личные записи и профиль</small>
  </button>
</div>
`;
const AUTH_SECTIONS = `
<section class="view auth-view" id="view-login">
  <div class="auth-shell">
    <div class="auth-story">
      <div class="auth-story-copy">
        <a class="auth-back-link" data-go="home" href="/">На главную</a>
        <h1>Ваши записи и лечение в одном месте</h1>
        <p>Войдите, чтобы управлять приёмами, получать уведомления и видеть данные своего профиля.</p>
      </div>
      <div class="auth-story-note"><span aria-hidden="true" class="auth-story-mark"><img alt="" src="/favicon.svg"/></span><p>Личный кабинет BublAPI Dent</p></div>
    </div>
    <div class="auth-form-pane">
      <form class="auth-form" id="loginForm">
        <div class="auth-form-heading"><h2>С возвращением</h2><p>Введите данные, указанные при регистрации.</p></div>
        <div class="auth-fields">
          <label>Email<input autocomplete="email" inputmode="email" name="email" placeholder="name@example.ru" required type="email"/></label>
          <label>Пароль<span class="password-field"><input autocomplete="current-password" name="password" required type="password"/><button aria-label="Показать пароль" class="password-toggle" data-password-toggle type="button">Показать</button></span></label>
        </div>
        <p aria-live="polite" class="auth-status hidden" id="loginStatus" role="status"></p>
        <button class="btn btn-primary auth-submit" type="submit">Войти</button>
        <p class="auth-switch">Впервые здесь? <a data-go="register" href="/register">Создать аккаунт пациента</a></p>
      </form>
    </div>
  </div>
</section>
<section class="view auth-view" id="view-register">
  <div class="auth-shell auth-shell-register">
    <div class="auth-story">
      <div class="auth-story-copy">
        <a class="auth-back-link" data-go="home" href="/">На главную</a>
        <h1>Записаться к врачу станет проще</h1>
        <p>Создайте кабинет пациента один раз. Дальше в нём будут храниться ваши записи и уведомления клиники.</p>
      </div>
      <div class="auth-story-note"><span aria-hidden="true" class="auth-story-mark"><img alt="" src="/favicon.svg"/></span><p>Регистрация доступна пациентам клиники</p></div>
    </div>
    <div class="auth-form-pane">
      <form class="auth-form auth-form-register" id="registerForm">
        <div class="auth-form-heading"><h2>Создайте аккаунт</h2><p>Поля со звёздочкой обязательны.</p></div>
        <div class="auth-fields auth-fields-grid">
          <label>Имя<input autocomplete="given-name" name="firstName"/></label>
          <label>Фамилия<input autocomplete="family-name" name="lastName"/></label>
          <label>Отчество<input autocomplete="additional-name" name="middleName"/></label>
          <label class="full">Телефон<input aria-describedby="registerPhoneHint" autocomplete="tel" inputmode="tel" name="phone" placeholder="(999) 123-45-67" required type="tel"/><small id="registerPhoneHint">Выберите код страны и введите номер</small></label>
          <label class="full">Email<input autocomplete="email" inputmode="email" name="email" placeholder="name@example.ru" required type="email"/></label>
          <label class="full">Пароль<span class="password-field"><input aria-describedby="registerPasswordHint" autocomplete="new-password" minlength="8" name="password" required type="password"/><button aria-label="Показать пароль" class="password-toggle" data-password-toggle type="button">Показать</button></span><small id="registerPasswordHint">Не менее 8 символов</small></label>
        </div>
        <p aria-live="polite" class="auth-status hidden" id="registerStatus" role="status"></p>
        <button class="btn btn-primary auth-submit" type="submit">Создать аккаунт</button>
        <p class="auth-switch">Уже есть аккаунт? <a data-go="login" href="/login">Войти</a></p>
      </form>
    </div>
  </div>
  <div class="hidden" id="authInfo"></div>
</section>
`;

const PATIENT_BOOKING_SECTION = `
<section class="view hidden" data-access="patient" id="view-patient-appointments">
  <div class="section-head booking-page-head">
    <div><div class="dashboard-kicker">Личный кабинет</div><h1>Мои записи</h1><p class="muted">Сначала — ваши ближайшие и прошлые приёмы. Новую запись можно оформить ниже.</p></div>
    <button class="btn btn-primary" id="loadPatientAppointments">Обновить записи</button>
  </div>

  <div class="patient-appointments-block patient-appointments-first">
    <div id="patientAppointments"></div>
  </div>

  <div class="section-head booking-create-head"><div><div class="dashboard-kicker">Онлайн-запись</div><h2>Новая запись</h2><p class="muted">Четыре коротких шага — без длинной формы и лишних полей.</p></div></div>

  <form class="booking-stepper" id="patientAppointmentForm">
    <div class="booking-progress" aria-label="Шаги записи">
      <button class="booking-progress-step active" data-booking-jump="1" type="button"><span>1</span><small>Услуга</small></button>
      <i></i>
      <button class="booking-progress-step" data-booking-jump="2" type="button"><span>2</span><small>Врач</small></button>
      <i></i>
      <button class="booking-progress-step" data-booking-jump="3" type="button"><span>3</span><small>Время</small></button>
      <i></i>
      <button class="booking-progress-step" data-booking-jump="4" type="button"><span>4</span><small>Подтверждение</small></button>
    </div>

    <div class="booking-step active" data-booking-step="1">
      <div class="booking-step-copy"><span>Шаг 1 из 4 · Услуга</span><h2>Что вас беспокоит?</h2><p>Выберите услугу — покажем только тех врачей, которые её оказывают.</p></div>
      <select class="booking-native-select" id="patientServiceSelect" name="clinicServiceId" required><option value="">Выберите услугу</option></select>
      <div class="booking-service-tools">
        <label class="booking-service-search"><span aria-hidden="true">⌕</span><input autocomplete="off" id="patientServiceSearch" placeholder="Найти услугу по названию" type="search"/></label>
        <small class="muted" id="patientServiceSearchMeta"></small>
      </div>
      <div class="booking-choice-grid" id="patientBookingServices"></div>
      <div class="booking-actions"><span></span><button class="btn btn-primary booking-next" data-booking-next="2" type="button">Продолжить</button></div>
    </div>

    <div class="booking-step" data-booking-step="2">
      <div class="booking-step-copy"><span>Шаг 2 из 4 · Специалист</span><h2>К кому хотите записаться?</h2><p>Можно вернуться назад и изменить услугу в любой момент.</p></div>
      <select class="booking-native-select" id="patientDoctorSelect" name="doctorId" required><option value="">Выберите врача</option></select>
      <div class="booking-choice-grid booking-doctor-grid" id="patientBookingDoctors"></div>
      <div class="booking-actions"><button class="btn btn-secondary" data-booking-prev="1" type="button">Назад</button><button class="btn btn-primary booking-next" data-booking-next="3" type="button">Продолжить</button></div>
    </div>

    <div class="booking-step" data-booking-step="3">
      <div class="booking-step-copy"><span>Шаг 3 из 4 · Время</span><h2>Когда вам удобно?</h2><p>Показываем только реально свободные слоты выбранного специалиста.</p></div>
      <div class="booking-summary-strip" id="patientBookingSummary"></div>
      <div class="booking-time-layout">
        <label class="booking-date-field">Дата<input disabled id="patientAppointmentDate" name="appointmentDate" required type="date"/></label>
        <label class="booking-time-field">Время<select disabled id="patientAppointmentTime" name="appointmentTime" required><option value="">Выберите время</option></select></label>
      </div>
      <div class="booking-date-quick-list" id="patientBookingDates" aria-label="Ближайшие доступные даты"></div>
      <div class="booking-slot-grid" id="patientBookingSlots"></div>
      <div class="booking-actions"><button class="btn btn-secondary" data-booking-prev="2" type="button">Назад</button><button class="btn btn-primary booking-next" data-booking-next="4" type="button">Продолжить</button></div>
    </div>

    <div class="booking-step" data-booking-step="4">
      <div class="booking-step-copy"><span>Шаг 4 из 4 · Подтверждение</span><h2>Проверьте запись</h2><p>Перед отправкой ещё раз сверим специалиста, услугу и время.</p></div>
      <div class="booking-confirm-card" id="patientBookingConfirm"></div>
      <div class="booking-extra-fields">
        <label>Количество<input min="1" name="quantity" required type="number" value="1"/></label>
        <label>Комментарий<textarea name="comment" placeholder="Необязательно"></textarea></label>
      </div>
      <div class="booking-actions"><button class="btn btn-secondary" data-booking-prev="3" type="button">Назад</button><button class="btn btn-success" type="submit">Подтвердить запись</button></div>
    </div>
  </form>

</section>`;

const STAFF_DASHBOARD_SECTION = `
<section class="view hidden" data-access="staff-core" id="view-staff-dashboard">
  <div class="section-head"><div><div class="dashboard-kicker">Рабочий стол</div><h1>Сегодня в клинике</h1><p class="muted">Расписание, загрузка и ситуации, которые требуют внимания.</p></div><button class="btn btn-primary" id="refreshStaffDashboard">Обновить данные</button></div>
  <div class="dashboard-metrics">
    <div class="card dashboard-metric-card"><div class="muted">Пациенты</div><div class="metric" id="metricPatients">—</div><div class="dashboard-delta" id="metricPatientsWeek">— за неделю</div></div>
    <div class="card dashboard-metric-card"><div class="muted">Записи сегодня</div><div class="metric" id="metricAppointmentsToday">—</div><div class="dashboard-delta">без отменённых</div></div>
    <div class="card dashboard-metric-card"><div class="muted">Врачи</div><div class="metric" id="metricDoctorsTotal">—</div><div class="dashboard-delta" id="metricDoctorsWorking">— работают сейчас</div></div>
  </div>
  <div class="staff-dashboard-layout">
    <div class="card staff-calendar-card">
      <div class="staff-calendar-head">
        <div><h2>Календарь</h2><p class="muted" id="staffCalendarDateLabel">Записи распределены по специалистам и времени.</p></div>
        <div class="staff-calendar-head-actions">
          <div class="staff-calendar-date-controls" aria-label="Выбор дня календаря">
            <button aria-label="Предыдущий день" class="btn btn-secondary btn-sm staff-calendar-arrow" id="staffCalendarPrev" type="button">←</button>
            <label class="staff-calendar-date-picker"><span class="sr-only">Дата</span><input id="staffCalendarDate" type="date"/></label>
            <button aria-label="Следующий день" class="btn btn-secondary btn-sm staff-calendar-arrow" id="staffCalendarNext" type="button">→</button>
            <button class="btn btn-secondary btn-sm" id="staffCalendarToday" type="button">Сегодня</button>
          </div>
          <div class="staff-calendar-controls"><button class="btn btn-secondary btn-sm active" type="button" data-calendar-mode="day">День</button><button class="btn btn-secondary btn-sm" type="button" data-calendar-mode="list">Список</button></div>
        </div>
      </div>
      <div class="staff-calendar" id="staffDashboardCalendar"></div>
    </div>
    <aside class="staff-attention" id="staffAttentionPanel">
      <div class="staff-attention-head"><h2>Требуют внимания</h2><span class="badge" id="staffAttentionCount">0</span></div>
      <div class="staff-attention-list" id="staffAttentionList"></div>
    </aside>
  </div>
  <div class="card dashboard-chart-card">
    <div class="dashboard-chart-head"><div><h2>Загрузка клиники</h2><p class="muted">Количество записей по дням</p></div><span class="muted">7 дней</span></div>
    <div aria-label="График загрузки клиники за неделю" class="dashboard-chart" id="dashboardOccupancyChart"></div>
    <div class="dashboard-chart-legend"><span><i class="dashboard-legend-dot"></i>Записи</span><strong id="metricAppointments">— всего</strong></div>
  </div>
</section>`;

const STAFF_PATIENTS_SECTION = `
<section class="view hidden" data-access="staff-core" id="view-staff-patients">
  <div class="section-head staff-entity-head">
    <div><div class="dashboard-kicker">Пациенты</div><h1>Пациенты клиники</h1><p class="muted">Найдите пациента, откройте карточку или начните запись прямо из его контекста.</p></div>
    <div class="staff-head-actions"><button class="btn btn-secondary btn-icon" id="loadStaffPatients" type="button" title="Обновить список" aria-label="Обновить список">↻</button><button class="btn btn-primary" data-staff-workflow="create-patient" type="button">+ Новый пациент</button></div>
  </div>
  <div class="staff-list-toolbar card compact-card"><label class="staff-list-search"><span>⌕</span><input type="search" placeholder="ФИО, телефон или email" data-filter-target="staffPatients"></label><span class="muted staff-toolbar-hint">Откройте пациента, чтобы увидеть действия и данные.</span></div>
  <div id="staffPatients"></div>
</section>`;

const STAFF_DOCTORS_SECTION = `
<section class="view hidden" data-access="staff-core" id="view-staff-doctors">
  <div class="section-head staff-entity-head">
    <div><div class="dashboard-kicker">Команда</div><h1>Врачи и расписания</h1><p class="muted">Сначала список специалистов. Расписание, услуги и редактирование доступны из карточки врача.</p></div>
    <div class="staff-head-actions"><button class="btn btn-secondary btn-icon" id="loadStaffDoctors" type="button" title="Обновить список" aria-label="Обновить список">↻</button><button class="btn btn-primary" data-staff-workflow="create-doctor" type="button">+ Добавить врача</button></div>
  </div>
  <div class="staff-list-toolbar card compact-card"><label class="staff-list-search"><span>⌕</span><input type="search" placeholder="ФИО или специальность" data-filter-target="staffDoctors"></label><span class="muted staff-toolbar-hint">Расписание и услуги настраиваются в контексте выбранного врача.</span></div>
  <div id="staffDoctors"></div>
</section>`;

const STAFF_SERVICES_SECTION = `
<section class="view hidden" data-access="staff-core" id="view-staff-services">
  <div class="section-head staff-entity-head">
    <div><div class="dashboard-kicker">Каталог клиники</div><h1>Услуги клиники</h1><p class="muted">Подключённые услуги, цены и длительность — без формы добавления перед списком.</p></div>
    <div class="staff-head-actions"><button class="btn btn-secondary btn-icon" id="loadStaffServices" type="button" title="Обновить список" aria-label="Обновить список">↻</button><button class="btn btn-primary" data-staff-workflow="add-service" type="button">+ Подключить услугу</button></div>
  </div>
  <div class="staff-list-toolbar card compact-card"><label class="staff-list-search"><span>⌕</span><input type="search" placeholder="Название, категория или цена" data-filter-target="staffServices"></label><span class="muted staff-toolbar-hint">Глобальный каталог открывается только при подключении новой услуги.</span></div>
  <div id="staffServices"></div>
</section>`;

const STAFF_APPOINTMENTS_SECTION = `
<section class="view hidden" data-access="staff-appointments" id="view-staff-appointments">
  <div class="section-head staff-entity-head">
    <div><div class="dashboard-kicker">Приёмы</div><h1>Записи клиники</h1><p class="muted">Просмотр, поиск и смена статуса. Новая запись запускается отдельным действием.</p></div>
    <div class="staff-head-actions"><button class="btn btn-secondary btn-icon" id="loadStaffAppointments" type="button" title="Обновить список" aria-label="Обновить список">↻</button><button class="btn btn-primary" data-staff-workflow="create-appointment" type="button">+ Новая запись</button></div>
  </div>
  <div class="staff-list-toolbar card compact-card staff-list-toolbar-wrap"><label class="staff-list-search"><span>⌕</span><input type="search" placeholder="Пациент, врач, дата или статус" data-filter-target="staffAppointments"></label><div class="staff-quick-filters" data-filter-table="staffAppointments"><button class="active" data-table-filter="" type="button">Все</button><button data-table-filter="создан" type="button">Ожидают</button><button data-table-filter="подтверж" type="button">Подтверждены</button><button data-table-filter="заверш" type="button">Завершены</button></div></div>
  <div id="staffAppointments"></div>
</section>`;

const STAFF_USERS_SECTION = `
<section class="view hidden" data-access="staff-core" id="view-staff-users">
  <div class="section-head staff-entity-head">
    <div><div class="dashboard-kicker">Доступ</div><h1>Пользователи</h1><p class="muted">Аккаунты, роли и быстрые действия в контексте пользователя.</p></div>
    <div class="staff-head-actions"><button class="btn btn-secondary btn-icon" id="loadStaffUsers" type="button" title="Обновить список" aria-label="Обновить список">↻</button></div>
  </div>
  <div class="staff-list-toolbar card compact-card"><label class="staff-list-search"><span>⌕</span><input type="search" placeholder="ФИО, email или роль" data-filter-target="staffUsers"></label><span class="muted staff-toolbar-hint">Если пользователь связан с карточкой пациента, запись можно создать прямо из его карточки.</span></div>
  <div id="staffUsers"></div>
</section>`;

const STAFF_WORKFLOW_MODAL = `
<div aria-hidden="true" class="staff-workflow-modal hidden" id="staffWorkflowModal">
  <div class="modal-backdrop" data-staff-workflow-close></div>
  <div class="staff-workflow-dialog" role="dialog" aria-modal="true" aria-labelledby="staffWorkflowTitle">
    <div class="staff-workflow-head"><div><div class="dashboard-kicker">Быстрое действие</div><h2 id="staffWorkflowTitle">Действие</h2><p class="muted" id="staffWorkflowSubtitle"></p></div><button class="btn btn-ghost btn-sm" data-staff-workflow-close type="button">Закрыть</button></div>
    <div class="staff-workflow-body">
      <section class="staff-workflow-panel hidden" data-workflow-panel="create-patient">
        <form class="card workflow-card" id="createPatientForm"><h3>Данные пациента</h3><div class="form-grid"><label>Имя<input name="firstName" required></label><label>Фамилия<input name="lastName" required></label><label>Отчество<input name="middleName"></label><label>Телефон<input name="phone"></label><label>Email<input name="email" type="email"></label><label>Дата рождения<input name="birthDate" type="date"></label><label class="full">Примечания<textarea name="notes"></textarea></label></div><div class="workflow-footer"><button class="btn btn-secondary" data-staff-workflow-close type="button">Отмена</button><button class="btn btn-success">Создать пациента</button></div></form>
      </section>
      <section class="staff-workflow-panel hidden" data-workflow-panel="create-doctor">
        <form class="card workflow-card" id="createDoctorForm"><h3>Новый врач</h3><div class="form-grid"><label>Имя<input name="firstName" required></label><label>Фамилия<input name="lastName" required></label><label>Отчество<input name="middleName"></label><label>Специальность<input name="specialty" required></label><div class="full avatar-picker"><span>Аватар</span><div class="avatar-picker-row"><img alt="Предпросмотр аватара" class="doctor-avatar doctor-avatar-preview" src="/default.png"><label class="avatar-upload-control">Выбрать изображение<input accept="image/png,image/jpeg,image/webp" name="avatarFile" type="file"></label></div><input name="avatarUrl" type="hidden"><small class="muted">PNG, JPG или WEBP, до 5 МБ.</small></div><label class="full">Описание<textarea name="description"></textarea></label></div><div class="workflow-footer"><button class="btn btn-secondary" data-staff-workflow-close type="button">Отмена</button><button class="btn btn-success" disabled id="createDoctorSubmit" type="submit">Создать врача</button></div></form>
      </section>
      <section class="staff-workflow-panel hidden" data-workflow-panel="doctor-schedule">
        <form class="card workflow-card" id="doctorScheduleForm"><h3>Рабочие часы</h3><div class="form-grid"><label class="full">Врач<select id="doctorScheduleDoctorSelect" name="doctorId" required><option value="">Выберите врача</option></select></label><label>День<select name="dayOfWeek" required><option value="MONDAY">Понедельник</option><option value="TUESDAY">Вторник</option><option value="WEDNESDAY">Среда</option><option value="THURSDAY">Четверг</option><option value="FRIDAY">Пятница</option><option value="SATURDAY">Суббота</option><option value="SUNDAY">Воскресенье</option></select></label><label>Начало<input lang="ru-RU" name="startTime" required type="time"></label><label>Окончание<input lang="ru-RU" name="endTime" required type="time"></label></div><div class="workflow-footer"><button class="btn btn-primary">Добавить интервал</button></div><div class="schedule-management-list" id="doctorScheduleList"><p class="muted">Выберите врача, чтобы увидеть его расписание.</p></div></form>
      </section>
      <section class="staff-workflow-panel hidden" data-workflow-panel="doctor-services">
        <div class="card workflow-card" id="doctorServiceManagement"><h3>Услуги врача</h3><p class="muted">Привязывайте только те услуги, которые специалист действительно оказывает.</p><div class="form-grid"><label>Врач<select id="doctorServiceDoctorSelect"><option value="">Выберите врача</option></select></label><label>Услуга клиники<select id="doctorServiceClinicServiceSelect"><option value="">Выберите услугу</option></select></label></div><div class="actions workflow-actions"><button class="btn btn-success" id="assignDoctorService" type="button">Привязать услугу</button><button class="btn btn-danger" id="removeDoctorService" type="button">Отвязать выбранную</button></div><div id="assignedDoctorServices"></div></div>
      </section>
      <section class="staff-workflow-panel hidden" data-workflow-panel="add-service">
        <div class="workflow-split"><form class="card workflow-card" id="addClinicServiceForm"><h3>Подключить услугу</h3><div class="form-grid"><label>Категория <span class="muted">(необязательно)</span><select id="clinicServiceCategoryFilter"><option value="">Все категории</option></select></label><label>Услуга из каталога<select id="clinicServiceDentalServiceSelect" name="dentalServiceId" required><option value="">Выберите услугу</option></select></label><label>Цена<input min="0" name="price" required type="number"></label><label>Длительность, мин<input min="5" name="durationMinutes" required type="number"></label><label class="full muted" id="clinicServiceSelectedDescription">Выберите услугу, чтобы увидеть описание.</label></div><div class="workflow-footer"><button class="btn btn-secondary" data-staff-workflow-close type="button">Отмена</button><button class="btn btn-success">Подключить</button></div></form><div class="card workflow-card"><div class="section-head workflow-card-head"><div><h3>Глобальный каталог</h3><p class="muted">Выберите готовую услугу BublAPI.</p></div><button class="btn btn-secondary btn-sm" id="loadDentalCatalog" type="button">Обновить</button></div><div id="dentalCatalog"></div></div></div>
      </section>
      <section class="staff-workflow-panel hidden" data-workflow-panel="create-appointment">
        <form class="card workflow-card appointment-workflow-card" id="staffAppointmentForm"><div class="workflow-context-strip hidden" id="staffAppointmentContext"></div><h3>Новая запись</h3><p class="muted">Известные данные подставятся автоматически. Остальное можно изменить.</p><div class="form-grid"><label class="full staff-patient-picker">Пациент<input type="search" id="staffPatientSearch" autocomplete="off" placeholder="ФИО, телефон или email"><input name="patientId" id="staffPatientId" type="hidden" required><div class="staff-patient-suggestions hidden" id="staffPatientSuggestions"></div></label><label>Врач<select id="staffDoctorSelect" name="doctorId" required><option value="">Выберите врача</option></select></label><label>Услуга<select id="staffServiceSelect" name="clinicServiceId" required><option value="">Выберите услугу</option></select></label><label>Дата<input disabled id="staffAppointmentDate" name="appointmentDate" required type="date"/></label><label>Время<select disabled id="staffAppointmentTime" name="appointmentTime" required><option value="">Выберите время</option></select></label><div class="full booking-date-quick-list compact" id="staffBookingDates" aria-label="Ближайшие доступные даты"></div><label>Количество<input min="1" name="quantity" required type="number" value="1"></label><label class="full">Комментарий<textarea name="comment" placeholder="Необязательно"></textarea></label></div><div class="workflow-footer"><button class="btn btn-secondary" data-staff-workflow-close type="button">Отмена</button><button class="btn btn-success">Создать запись</button></div></form>
      </section>
    </div>
  </div>
</div>`;


const PAGES = {
  '': { view:'home', title:'Стоматологическая клиника BublAPI Dent', description:'Онлайн-запись в стоматологическую клинику: врачи, услуги, актуальное расписание и личный кабинет пациента.', index:true, image:'/og-image.png' },
  'doctors': { view:'public-doctors', title:'Врачи стоматологической клиники', description:'Познакомьтесь со специалистами клиники, их специализациями и актуальным расписанием приёма.', index:true, image:'/og-image-doctors.png' },
  'services': { view:'public-services', title:'Услуги и цены стоматологии', description:'Стоматологические услуги клиники, стоимость и длительность приёма.', index:true, image:'/og-image-services.png' },
  'login': { view:'login', navView:'login', title:'Вход в личный кабинет', description:'Вход в личный кабинет пациента стоматологической клиники.', index:false, image:'/og-image.png' },
  'register': { view:'register', navView:'login', title:'Регистрация пациента', description:'Регистрация личного кабинета пациента стоматологической клиники.', index:false, image:'/og-image.png' },
  'profile': { view:'profile', title:'Личный кабинет', description:'Профиль пользователя клиники.', index:false, image:'/og-image.png' },
  'patient/card': { view:'patient-card', title:'Моя медицинская карточка', description:'Медицинская карточка пациента.', index:false, image:'/og-image.png' },
  'patient/appointments': { view:'patient-appointments', title:'Мои записи', description:'Записи пациента на приём.', index:false, image:'/og-image.png' },
  'patient/notifications': { view:'patient-notifications', title:'Мои уведомления', description:'Уведомления пациента клиники.', index:false, image:'/og-image.png' },
  'staff': { view:'staff-dashboard', title:'Рабочий стол клиники', description:'Рабочий стол сотрудника клиники.', index:false, image:'/og-image.png' },
  'staff/patients': { view:'staff-patients', title:'Пациенты клиники', description:'Рабочий раздел сотрудников клиники.', index:false, image:'/og-image.png' },
  'staff/doctors': { view:'staff-doctors', title:'Врачи и расписания', description:'Рабочий раздел сотрудников клиники.', index:false, image:'/og-image.png' },
  'staff/services': { view:'staff-services', title:'Услуги клиники', description:'Рабочий раздел сотрудников клиники.', index:false, image:'/og-image.png' },
  'staff/appointments': { view:'staff-appointments', title:'Записи клиники', description:'Рабочий раздел сотрудников клиники.', index:false, image:'/og-image.png' },
  'staff/users': { view:'staff-users', title:'Пользователи клиники', description:'Рабочий раздел сотрудников клиники.', index:false, image:'/og-image.png' }
};

async function resolvePage(params) {
  const resolved = await params;
  const key = (resolved?.slug || []).join('/');
  return { key, config: PAGES[key] };
}

export async function generateMetadata({ params }) {
  const { key, config } = await resolvePage(params);
  if (!config) return {};
  const path = key ? `/${key}` : '/';
  return {
    title: config.title,
    description: config.description,
    alternates: { canonical: path },
    robots: config.index ? { index:true, follow:true, googleBot:{ index:true, follow:true, 'max-image-preview':'large', 'max-snippet':-1 } } : { index:false, follow:false, nocache:true },
    openGraph: { type:'website', locale:'ru_RU', url:path, siteName:'BublAPI Dent', title:config.title, description:config.description, images:[{ url:config.image, width:1200, height:630, alt:config.title }] },
    twitter: { card:'summary_large_image', title:config.title, description:config.description, images:[config.image] }
  };
}

export default async function DemoRoute({ params }) {
  const { key, config } = await resolvePage(params);
  if (!config) notFound();

  const publicCatalog = await loadInitialPublicCatalog(config.view);

  const structuredData = config.index ? {
    '@context':'https://schema.org',
    '@type': key === 'doctors' ? 'MedicalBusiness' : 'Dentist',
    name:'BublAPI Dent',
    url:`${SITE}${key ? `/${key}` : '/'}`,
    description:config.description
  } : null;

  let initialShell = withThemeToggle(demoShell)
    .replace('Вход / регистрация', 'Войти')
    .replace('data-view="auth">Авторизация', 'data-view="login">Войти')
    .replace('data-go="auth">Войти в систему', 'data-go="login">Войти в систему')
    .replace(/<section class="view" id="view-auth">[\s\S]*?<\/section>/, AUTH_SECTIONS)
    .replace(/<section class="view hidden" data-access="patient" id="view-patient-appointments">[\s\S]*?<\/section>/, PATIENT_BOOKING_SECTION)
    .replace(/<section class="view hidden" data-access="staff-core" id="view-staff-dashboard">[\s\S]*?<\/section>/, STAFF_DASHBOARD_SECTION)
    .replace(/<section class="view hidden" data-access="staff-core" id="view-staff-patients">[\s\S]*?<\/section>/, STAFF_PATIENTS_SECTION)
    .replace(/<section class="view hidden" data-access="staff-core" id="view-staff-doctors">[\s\S]*?<\/section>/, STAFF_DOCTORS_SECTION)
    .replace(/<section class="view hidden" data-access="staff-core" id="view-staff-services">[\s\S]*?<\/section>/, STAFF_SERVICES_SECTION)
    .replace(/<section class="view hidden" data-access="staff-appointments" id="view-staff-appointments">[\s\S]*?<\/section>/, STAFF_APPOINTMENTS_SECTION)
    .replace(/<section class="view hidden" data-access="staff-core" id="view-staff-users">[\s\S]*?<\/section>/, STAFF_USERS_SECTION)
    .replace('</main>\n</div>\n<div class="toast-container"></div>', `</main>\n</div>\n${STAFF_WORKFLOW_MODAL}\n<div class="toast-container"></div>`)
    .replace(
      '<div class="nav-group hidden" data-access="staff-core,staff-appointments">',
      `${SIDEBAR_CONTEXT_CONTROLS}<div class="nav-group hidden" data-access="staff-core,staff-appointments">`
    )
    .replace('class=\"view active\" id=\"view-home\"', 'class=\"view\" id=\"view-home\"');
  if (['home','public-doctors','public-services','login','register'].includes(config.view)) {
    initialShell = initialShell.replace(`class=\"view\" id=\"view-${config.view}\"`, `class=\"view active\" id=\"view-${config.view}\"`);
    initialShell = initialShell.replace(/class=\"nav-button active\" data-view=\"home\"/, 'class=\"nav-button\" data-view=\"home\"');
    initialShell = initialShell.replace(`class=\"nav-button\" data-view=\"${config.navView || config.view}\"`, `class=\"nav-button active\" data-view=\"${config.navView || config.view}\"`);
  } else {
    initialShell = initialShell.replace('class=\"view\" id=\"view-home\"', 'class=\"view active\" id=\"view-home\"');
  }

  if (publicCatalog.doctors?.loaded) {
    initialShell = initialShell.replace(
      '<div class="grid grid-3" id="publicDoctors"></div>',
      `<div class="grid grid-3" id="publicDoctors">${renderDoctorsHtml(publicCatalog.doctors.items)}</div>`
    );
  } else if (publicCatalog.doctors) {
    initialShell = initialShell.replace(
      '<div class="grid grid-3" id="publicDoctors"></div>',
      '<div class="grid grid-3" id="publicDoctors"><div class="card empty">Не удалось загрузить врачей. Попробуйте обновить страницу.</div></div>'
    );
  }

  if (publicCatalog.services?.loaded) {
    initialShell = initialShell.replace(
      '<div class="grid grid-3" id="publicServices"></div>',
      `<div class="grid grid-3" id="publicServices">${renderServicesHtml(publicCatalog.services.items)}</div>`
    );
  } else if (publicCatalog.services) {
    initialShell = initialShell.replace(
      '<div class="grid grid-3" id="publicServices"></div>',
      '<div class="grid grid-3" id="publicServices"><div class="card empty">Не удалось загрузить услуги. Попробуйте обновить страницу.</div></div>'
    );
  }

  return <>
    {structuredData && <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }} />}
    <div data-initial-view={config.view} dangerouslySetInnerHTML={{ __html: initialShell }} />
    {(publicCatalog.doctors || publicCatalog.services) && (
      <script
        id="publicCatalogSsrData"
        type="application/json"
        dangerouslySetInnerHTML={{ __html: serializeCatalogSeed(publicCatalog) }}
      />
    )}
    <noscript><div style={{padding:'24px',maxWidth:'900px',margin:'0 auto'}}>Для работы личного кабинета требуется JavaScript.</div></noscript>
    <Script src="/legacy/app.js" type="module" strategy="afterInteractive" />
  </>;
}

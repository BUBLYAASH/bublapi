package org.bublapi.dent.appointment.service;

import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.appointment.dto.ChangeAppointmentStatusRequestDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment.mapper.AppointmentMapper;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.bublapi.dent.appointment_service.repository.AppointmentServiceRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;
import org.bublapi.dent.doctor_schedule_exception.repository.DoctorScheduleExceptionRepository;
import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.bublapi.dent.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import org.bublapi.dent.logging.UserAuditService;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.EmailTemplateData;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.publisher.NotificationPublisher;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
   private static final DateTimeFormatter APPOINTMENT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
           "dd.MM.yyyy HH:mm");

   private final AppointmentRepository appointmentRepository;
   private final AppointmentServiceRepository appointmentServiceRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final PatientRepository patientRepository;
   private final DoctorRepository doctorRepository;
   private final DoctorWorkingHoursRepository doctorWorkingHoursRepository;
   private final DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;
   private final AppointmentMapper appointmentMapper;
   private final NotificationPublisher notificationPublisher;
   private final UserAuditService userAuditService;

   public AppointmentService(AppointmentRepository appointmentRepository,
                             AppointmentServiceRepository appointmentServiceRepository,
                             ClinicServiceRepository clinicServiceRepository, PatientRepository patientRepository,
                             DoctorRepository doctorRepository,
                             DoctorWorkingHoursRepository doctorWorkingHoursRepository,
                             DoctorScheduleExceptionRepository doctorScheduleExceptionRepository,
                             AppointmentMapper appointmentMapper, NotificationPublisher notificationPublisher,
                             UserAuditService userAuditService) {
      this.appointmentRepository = appointmentRepository;
      this.appointmentServiceRepository = appointmentServiceRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.patientRepository = patientRepository;
      this.doctorRepository = doctorRepository;
      this.doctorWorkingHoursRepository = doctorWorkingHoursRepository;
      this.doctorScheduleExceptionRepository = doctorScheduleExceptionRepository;
      this.appointmentMapper = appointmentMapper;
      this.notificationPublisher = notificationPublisher;
      this.userAuditService = userAuditService;
   }

   private void validateInsideRegularWorkingHours(UUID clinicId, Doctor doctor, LocalDateTime scheduledAt,
                                                  LocalDateTime endAt) {
      DayOfWeek dayOfWeek = DayOfWeek.valueOf(scheduledAt.getDayOfWeek().name());

      LocalTime appointmentStart = scheduledAt.toLocalTime();
      LocalTime appointmentEnd = endAt.toLocalTime();

      List<DoctorWorkingHours> workingHours = doctorWorkingHoursRepository.findAllByDoctor_Clinic_IdAndDoctor_IdAndDayOfWeek(
              clinicId, doctor.getId(), dayOfWeek);

      boolean fitsWorkingHours = workingHours.stream()
                                             .anyMatch(hours -> !appointmentStart.isBefore(
                                                     hours.getStartTime()) && !appointmentEnd.isAfter(
                                                     hours.getEndTime()));

      if (!fitsWorkingHours) {
         throw new BadRequestException("Selected time is outside the doctor's working hours");
      }
   }

   private void validateInsideCustomWorkingHours(List<DoctorScheduleException> exceptions, LocalDateTime scheduledAt,
                                                 LocalDateTime endAt) {
      LocalTime appointmentStart = scheduledAt.toLocalTime();
      LocalTime appointmentEnd = endAt.toLocalTime();

      boolean fitsCustomWorkingHours = exceptions.stream()
                                                 .filter(type -> type.getType() == ScheduleExceptionType.CUSTOM_WORKING_HOURS)
                                                 .anyMatch(exception -> !appointmentStart.isBefore(
                                                         exception.getStartTime()) && !appointmentEnd.isAfter(
                                                         exception.getEndTime()));

      if (!fitsCustomWorkingHours) {
         throw new BadRequestException("Selected time is outside the doctor's custom working hours");
      }
   }

   private void validateNoAppointmentOverlap(UUID clinicId, Doctor doctor, LocalDateTime scheduledAt,
                                             LocalDateTime endAt) {

      boolean hasOverlap = appointmentRepository.existsOverlappingAppointment(clinicId, doctor.getId(), scheduledAt,
                                                                              endAt, AppointmentStatus.CANCELLED);

      if (hasOverlap) {
         throw new BadRequestException("Doctor already has an appointment during the selected time");
      }
   }

   private void validateDoctorAvailability(UUID clinicId, Doctor doctor, LocalDateTime scheduledAt,
                                           LocalDateTime endAt) {
      if (!scheduledAt.isAfter(LocalDateTime.now())) {
         throw new BadRequestException("Appointment time must be in the future");
      }

      if (!scheduledAt.toLocalDate().equals(endAt.toLocalDate())) {
         throw new BadRequestException("Appointment cannot continue into the next day");
      }

      LocalDate appointmentDate = scheduledAt.toLocalDate();

      List<DoctorScheduleException> scheduleExceptions = doctorScheduleExceptionRepository.findAllByDoctor_Clinic_IdAndDoctor_IdAndDate(
              clinicId, doctor.getId(), appointmentDate);

      boolean hasDayOff = scheduleExceptions.stream().anyMatch(type -> type.getType() == ScheduleExceptionType.DAY_OFF);

      if (hasDayOff) {
         throw new BadRequestException("Doctor is unavailable on the selected date");
      }

      boolean hasCustomWorkingHours = scheduleExceptions.stream()
                                                        .anyMatch(
                                                                type -> type.getType() == ScheduleExceptionType.CUSTOM_WORKING_HOURS);

      if (hasCustomWorkingHours) {
         validateInsideCustomWorkingHours(scheduleExceptions, scheduledAt, endAt);
         return;
      }
      validateInsideRegularWorkingHours(clinicId, doctor, scheduledAt, endAt);
   }

   private void validateTransition(AppointmentStatus from, AppointmentStatus to) {
      if (from == to) {
         throw new BadRequestException("Appointment already has this status");
      }

      if (!from.canTransitionTo(to)) {
         throw new BadRequestException("Cannot change appointment status from " + from + " to " + to);
      }
   }

   private void publishAppointmentNotifications(Appointment appointment, NotificationType type, String title,
                                                String message, EmailTemplateData emailData) {
      UUID patientUserId = appointment.getPatient().getUser() != null ? appointment.getPatient()
                                                                                   .getUser()
                                                                                   .getId() : null;

      String patientEmail = appointment.getPatient().getEmail();

      notificationPublisher.publishAfterCommit(
              new CreateNotificationCommand(appointment.getClinic().getId(), patientUserId, appointment.getId(), type,
                                            NotificationChannel.IN_APP, patientEmail, title, message, null, null));

      if (patientEmail != null && !patientEmail.isBlank()) {
         notificationPublisher.publishAfterCommit(
                 new CreateNotificationCommand(appointment.getClinic().getId(), patientUserId, appointment.getId(),
                                               type, NotificationChannel.EMAIL, patientEmail, title, message, null,
                                               emailData));
      }
   }

   private EmailTemplateData createEmailTemplateData(Appointment appointment) {
      return new EmailTemplateData(appointment.getClinic().getTitle(), appointment.getPatient().getFirstName(),
                                   appointment.getScheduledAt().format(APPOINTMENT_DATE_TIME_FORMATTER),
                                   appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                                   appointment.getServices().stream().map(AppointmentServiceItem::getTitle).toList());
   }

   @Transactional
   public AppointmentResponseDto create(UUID patientId, CreateAppointmentRequestDto request) {
      Clinic clinic = ClinicContext.get();

      Patient patient = patientRepository.findByClinic_IdAndId(clinic.getId(), patientId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      Doctor doctor = doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinic.getId(), request.doctorId())
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      List<ResolvedAppointmentService> resolvedServices = new ArrayList<>();

      int totalPrice = 0;
      int totalDuration = 0;

      for (AppointmentServiceRequestDto serviceRequest : request.services()) {
         ClinicService clinicService = clinicServiceRepository.findByClinic_IdAndIdAndActiveTrue(clinic.getId(),
                                                                                                 serviceRequest.clinicServiceId())
                                                              .orElseThrow(() -> new ResourceNotFoundException(
                                                                      "Clinic service not found"));

         resolvedServices.add(new ResolvedAppointmentService(clinicService, serviceRequest.quantity()));
         totalPrice += clinicService.getPrice() * serviceRequest.quantity();
         totalDuration += clinicService.getDurationMinutes() * serviceRequest.quantity();
      }

      LocalDateTime scheduledAt = request.scheduledAt();
      LocalDateTime endAt = scheduledAt.plusMinutes(totalDuration);

      validateDoctorAvailability(clinic.getId(), doctor, scheduledAt, endAt);
      validateNoAppointmentOverlap(clinic.getId(), doctor, scheduledAt, endAt);

      Appointment appointment = appointmentMapper.toEntity(request);

      appointment.setClinic(clinic);
      appointment.setPatient(patient);
      appointment.setDoctor(doctor);
      appointment.setEndAt(endAt);
      appointment.setTotalPrice(totalPrice);
      appointment.setStatus(AppointmentStatus.CREATED);

      Appointment saved = appointmentRepository.save(appointment);

      int position = 1;

      for (ResolvedAppointmentService resolvedService : resolvedServices) {
         ClinicService clinicService = resolvedService.clinicService();

         AppointmentServiceItem item = new AppointmentServiceItem();

         item.setAppointment(saved);
         saved.getServices().add(item);
         item.setClinicService(clinicService);
         item.setTitle(clinicService.getDentalService().getTitle());
         item.setPrice(clinicService.getPrice());
         item.setDurationMinutes(clinicService.getDurationMinutes());
         item.setQuantity(resolvedService.quantity());
         item.setPosition(position++);

         appointmentServiceRepository.save(item);
      }

      publishAppointmentNotifications(saved, NotificationType.APPOINTMENT_CREATED, "Вы успешно записались",
                                      "Ваша запись к врачу " + saved.getDoctor().getLastName() + " " + saved.getDoctor()
                                                                                                            .getFirstName() + " успешно создана на " + saved.getScheduledAt()
                                                                                                                                                            .format(APPOINTMENT_DATE_TIME_FORMATTER),
                                      createEmailTemplateData(saved));

      userAuditService.appointmentCreated(saved.getId());

      return appointmentMapper.toResponse(saved);
   }

   @Transactional
   public AppointmentResponseDto cancel(UUID patientId, UUID appointmentId) {
      UUID clinicId = ClinicContext.getClinicId();

      Appointment appointment = appointmentRepository.findByClinic_IdAndIdAndPatient_Id(clinicId, appointmentId,
                                                                                        patientId)
                                                     .orElseThrow(() -> new ResourceNotFoundException(
                                                             "Appointment not found"));

      if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
         throw new BadRequestException("Appointment is already cancelled");
      }

      if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
         throw new BadRequestException("Completed appointment cannot be cancelled");
      }

      appointment.setStatus(AppointmentStatus.CANCELLED);

      publishAppointmentNotifications(appointment, NotificationType.APPOINTMENT_CANCELLED, "Ваша запись отменена",
                                      "Ваша запись на " + appointment.getScheduledAt()
                                                                     .format(APPOINTMENT_DATE_TIME_FORMATTER) + " отменена",
                                      createEmailTemplateData(appointment));

      userAuditService.appointmentCancelled(appointment.getId());

      return appointmentMapper.toResponse(appointment);
   }

   @Transactional
   public AppointmentResponseDto createForPatient(UUID userId, CreateAppointmentRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndUser_Id(clinicId, userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return create(patient.getId(), request);
   }

   @Transactional
   public AppointmentResponseDto cancelByPatient(UUID userId, UUID appointmentId) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndUser_Id(clinicId, userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return cancel(patient.getId(), appointmentId);
   }

   @Transactional
   public AppointmentResponseDto createForStaff(UUID patientId, CreateAppointmentRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      patientRepository.findByClinic_IdAndId(clinicId, patientId)
                       .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return create(patientId, request);
   }

   @Transactional
   public AppointmentResponseDto cancelByStaff(UUID appointmentId) {
      UUID clinicId = ClinicContext.getClinicId();

      Appointment appointment = appointmentRepository.findByClinic_IdAndId(clinicId, appointmentId)
                                                     .orElseThrow(() -> new ResourceNotFoundException(
                                                             "Appointment not found"));

      return cancel(appointment.getPatient().getId(), appointmentId);
   }

   @Transactional
   public AppointmentResponseDto changeStatusByStaff(UUID appointmentId, ChangeAppointmentStatusRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      Appointment appointment = appointmentRepository.findByClinic_IdAndId(clinicId, appointmentId)
                                                     .orElseThrow(() -> new ResourceNotFoundException(
                                                             "Appointment not found"));

      AppointmentStatus oldStatus = appointment.getStatus();
      AppointmentStatus newStatus = request.status();

      validateTransition(oldStatus, newStatus);

      appointment.setStatus(request.status());

      publishAppointmentNotifications(appointment, NotificationType.APPOINTMENT_STATUS_CHANGED,
                                      "Статус Вашей записи изменен",
                                      "Статус вашей записи изменен: " + oldStatus + " -> " + newStatus,
                                      createEmailTemplateData(appointment));

      userAuditService.appointmentStatusChanged(appointment.getId());

      return appointmentMapper.toResponse(appointment);
   }

   public AppointmentResponseDto findByIdForPatient(UUID userId, UUID appointmentId) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndUser_Id(clinicId, userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      Appointment appointment = appointmentRepository.findByClinic_IdAndIdAndPatient_Id(clinicId, appointmentId,
                                                                                        patient.getId())
                                                     .orElseThrow(() -> new ResourceNotFoundException(
                                                             "Appointment not found"));

      return appointmentMapper.toResponse(appointment);
   }

   public AppointmentResponseDto findByIdForStaff(UUID appointmentId) {
      UUID clinicId = ClinicContext.getClinicId();

      Appointment appointment = appointmentRepository.findByClinic_IdAndId(clinicId, appointmentId)
                                                     .orElseThrow(() -> new ResourceNotFoundException(
                                                             "Appointment not found"));

      return appointmentMapper.toResponse(appointment);
   }

   public List<AppointmentResponseDto> findAllForPatient(UUID userId) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndUser_Id(clinicId, userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return appointmentRepository.findAllByClinic_IdAndPatient_IdOrderByScheduledAtDesc(clinicId, patient.getId())
                                  .stream()
                                  .map(appointmentMapper::toResponse)
                                  .toList();
   }

   public List<AppointmentResponseDto> findAllByPatientForStaff(UUID patientId) {
      UUID clinicId = ClinicContext.getClinicId();

      if (!patientRepository.existsByClinic_IdAndId(clinicId, patientId)) {
         throw new ResourceNotFoundException("Patient not found");
      }

      return appointmentRepository.findAllByClinic_IdAndPatient_IdOrderByScheduledAtDesc(clinicId, patientId)
                                  .stream()
                                  .map(appointmentMapper::toResponse)
                                  .toList();
   }

   public List<AppointmentResponseDto> findAllForStaff() {
      UUID clinicId = ClinicContext.getClinicId();

      return appointmentRepository.findAllByClinic_IdOrderByScheduledAtDesc(clinicId)
                                  .stream()
                                  .map(appointmentMapper::toResponse)
                                  .toList();
   }

   public List<AppointmentResponseDto> findAllForDoctor(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

      return appointmentRepository.findAllByClinic_IdAndDoctor_IdOrderByScheduledAtAsc(clinicId, doctor.getId())
                                  .stream()
                                  .map(appointmentMapper::toResponse)
                                  .toList();
   }

   private record ResolvedAppointmentService(
           ClinicService clinicService, int quantity) {
   }

   // TODO:
   //  - reschedule appointment
   //  - update appointment services
   //  - add rate limiting for appointment creation per user
}

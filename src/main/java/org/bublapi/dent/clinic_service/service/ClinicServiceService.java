package org.bublapi.dent.clinic_service.service;

import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment_service.repository.AppointmentServiceRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.dto.AddClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.dto.ClinicServiceResponseDto;
import org.bublapi.dent.clinic_service.dto.UpdateClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.mapper.ClinicServiceMapper;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.dental_service.repository.DentalServiceRepository;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.publisher.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClinicServiceService {
   private final ClinicServiceRepository clinicServiceRepository;
   private final DentalServiceRepository dentalServiceRepository;
   private final ClinicServiceMapper clinicServiceMapper;
   private final AppointmentServiceRepository appointmentServiceRepository;
   private final NotificationPublisher notificationPublisher;

   public ClinicServiceService(ClinicServiceRepository clinicServiceRepository, DentalServiceRepository dentalServiceRepository, ClinicServiceMapper clinicServiceMapper, AppointmentServiceRepository appointmentServiceRepository, NotificationPublisher notificationPublisher) {
      this.clinicServiceRepository = clinicServiceRepository;
      this.dentalServiceRepository = dentalServiceRepository;
      this.clinicServiceMapper = clinicServiceMapper;
      this.appointmentServiceRepository = appointmentServiceRepository;
      this.notificationPublisher = notificationPublisher;
   }

   private void publishClinicServiceNotifications(Appointment appointment, NotificationType type, String title, String message) {
      UUID patientUserId = appointment.getPatient().getUser() != null ? appointment.getPatient()
                                                                                   .getUser()
                                                                                   .getId() : null;

      String patientEmail = appointment.getPatient().getEmail();

      notificationPublisher.publishAfterCommit(
              new CreateNotificationCommand(appointment.getClinic().getId(), patientUserId, appointment.getId(), type,
                                            NotificationChannel.IN_APP, patientEmail, title, message, null));

      if (patientEmail != null && !patientEmail.isBlank()) {
         notificationPublisher.publishAfterCommit(
                 new CreateNotificationCommand(appointment.getClinic().getId(), patientUserId, appointment.getId(),
                                               type, NotificationChannel.EMAIL, patientEmail, title, message, null));
      }
   }

   public ClinicServiceResponseDto add(UUID dentalServiceId, AddClinicServiceRequestDto request) {
      Clinic clinic = ClinicContext.get();

      if (clinicServiceRepository.existsByDentalService_Id(dentalServiceId)) {
         throw new BadRequestException("Dental Service is already in this clinic");
      }

      DentalService dentalService = dentalServiceRepository.findByIdAndActiveTrue(dentalServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Dental Service not found or unavailable"));

      ClinicService clinicService = clinicServiceMapper.toEntity(request);

      clinicService.setClinic(clinic);
      clinicService.setDentalService(dentalService);

      ClinicService saved = clinicServiceRepository.save(clinicService);

      return clinicServiceMapper.toResponse(saved);
   }

   @Transactional
   public ClinicServiceResponseDto update(UUID clinicServiceId, UpdateClinicServiceRequestDto request) {
      ClinicService clinicService = clinicServiceRepository.findById(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Clinic Service is not found"));

      clinicServiceMapper.updateEntity(request, clinicService);

      return clinicServiceMapper.toResponse(clinicService);
   }

   @Transactional
   public ClinicServiceResponseDto deactivate(UUID clinicServiceId) {
      ClinicService clinicService = clinicServiceRepository.findById(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Clinic Service not found or unavailable"));

      if (!clinicService.getActive()) {
         throw new BadRequestException("Clinic service is already deactivated");
      }

      clinicService.setActive(false);

      appointmentServiceRepository.findAllAffectedByServiceDeactivation(clinicServiceId, LocalDateTime.now(),
                                                                        List.of(AppointmentStatus.CANCELLED,
                                                                                AppointmentStatus.COMPLETED))
                                  .forEach(p -> publishClinicServiceNotifications(p.getAppointment(),
                                                                                  NotificationType.CLINIC_SERVICE_DEACTIVATED,
                                                                                  "Услуга приостановлена",
                                                                                  "Услуга «" + clinicService.getDentalService()
                                                                                                            .getTitle() + "» больше не предоставляется в клинике «" + clinicService.getClinic()
                                                                                                                                                                                   .getTitle() + "». Пожалуйста, свяжитесь с клиникой для изменения записи."));

      return clinicServiceMapper.toResponse(clinicService);
   }

   @Transactional
   public ClinicServiceResponseDto activate(UUID clinicServiceId) {
      ClinicService clinicService = clinicServiceRepository.findById(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Clinic Service not found"));

      clinicService.setActive(true);

      return clinicServiceMapper.toResponse(clinicService);
   }

   public List<ClinicServiceResponseDto> findAllActiveForPublic() {
      return clinicServiceRepository.findAllByActiveTrue().stream().map(clinicServiceMapper::toResponse).toList();
   }

   public ClinicServiceResponseDto findById(UUID clinicServiceId) {
      ClinicService clinicService = clinicServiceRepository.findByIdAndActiveTrue(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Clinic service not found"));

      return clinicServiceMapper.toResponse(clinicService);
   }

   public List<ClinicServiceResponseDto> findAllForStaff() {
      return clinicServiceRepository.findAll().stream().map(clinicServiceMapper::toResponse).toList();
   }
}

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
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
   private final AppointmentRepository appointmentRepository;
   private final AppointmentServiceRepository appointmentServiceRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final PatientRepository patientRepository;
   private final DoctorRepository doctorRepository;
   private final DoctorWorkingHoursRepository doctorWorkingHoursRepository;
   private final DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;
   private final AppointmentMapper appointmentMapper;

   public AppointmentService(AppointmentRepository appointmentRepository, AppointmentServiceRepository appointmentServiceRepository, ClinicServiceRepository clinicServiceRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, DoctorWorkingHoursRepository doctorWorkingHoursRepository, DoctorScheduleExceptionRepository doctorScheduleExceptionRepository, AppointmentMapper appointmentMapper) {
      this.appointmentRepository = appointmentRepository;
      this.appointmentServiceRepository = appointmentServiceRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.patientRepository = patientRepository;
      this.doctorRepository = doctorRepository;
      this.doctorWorkingHoursRepository = doctorWorkingHoursRepository;
      this.doctorScheduleExceptionRepository = doctorScheduleExceptionRepository;
      this.appointmentMapper = appointmentMapper;
   }

   private record ResolvedAppointmentService(
           ClinicService clinicService, int quantity) {
   }

   private void validateInsideRegularWorkingHours(Doctor doctor, LocalDateTime scheduledAt, LocalDateTime endAt) {
      DayOfWeek dayOfWeek = DayOfWeek.valueOf(scheduledAt.getDayOfWeek().name());

      LocalTime appointmentStart = scheduledAt.toLocalTime();
      LocalTime appointmentEnd = endAt.toLocalTime();

      List<DoctorWorkingHours> workingHours = doctorWorkingHoursRepository.findAllByDoctor_IdAndDayOfWeek(doctor.getId(), dayOfWeek);

      boolean fitsWorkingHours = workingHours.stream()
                                             .anyMatch(hours -> !appointmentStart.isBefore(hours.getStartTime()) && !appointmentEnd.isAfter(hours.getEndTime()));

      if (!fitsWorkingHours) {
         throw new BadRequestException("Selected time is outside the doctor's working hours");
      }
   }

   private void validateInsideCustomWorkingHours(List<DoctorScheduleException> exceptions, LocalDateTime scheduledAt, LocalDateTime endAt) {
      LocalTime appointmentStart = scheduledAt.toLocalTime();
      LocalTime appointmentEnd = endAt.toLocalTime();

      boolean fitsCustomWorkingHours = exceptions.stream()
                                                 .filter(type -> type.getType() == ScheduleExceptionType.CUSTOM_WORKING_HOURS)
                                                 .anyMatch(exception -> !appointmentStart.isBefore(exception.getStartTime()) && !appointmentEnd.isAfter(exception.getEndTime()));

      if (!fitsCustomWorkingHours) {
         throw new BadRequestException("Selected time is outside the doctor's custom working hours");
      }
   }

   private void validateNoAppointmentOverlap(Doctor doctor, LocalDateTime scheduledAt, LocalDateTime endAt) {

      boolean hasOverlap = appointmentRepository.existsOverlappingAppointment(doctor.getId(), scheduledAt, endAt, AppointmentStatus.CANCELLED);

      if (hasOverlap) {
         throw new BadRequestException("Doctor already has an appointment during the selected time");
      }
   }

   private void validateDoctorAvailability(Doctor doctor, LocalDateTime scheduledAt, LocalDateTime endAt) {
      if (!scheduledAt.isAfter(LocalDateTime.now())) {
         throw new BadRequestException("Appointment time must be in the future");
      }

      if (!scheduledAt.toLocalDate().equals(endAt.toLocalDate())) {
         throw new BadRequestException("Appointment cannot continue into the next day");
      }

      LocalDate appointmentDate = scheduledAt.toLocalDate();

      List<DoctorScheduleException> scheduleExceptions = doctorScheduleExceptionRepository.findAllByDoctor_IdAndDate(doctor.getId(), appointmentDate);

      boolean hasDayOff = scheduleExceptions.stream().anyMatch(type -> type.getType() == ScheduleExceptionType.DAY_OFF);

      if (hasDayOff) {
         throw new BadRequestException("Doctor is unavailable on the selected date");
      }

      boolean hasCustomWorkingHours = scheduleExceptions.stream()
                                                        .anyMatch(type -> type.getType() == ScheduleExceptionType.CUSTOM_WORKING_HOURS);

      if (hasCustomWorkingHours) {
         validateInsideCustomWorkingHours(scheduleExceptions, scheduledAt, endAt);
         return;
      }
      validateInsideRegularWorkingHours(doctor, scheduledAt, endAt);
   }

   @Transactional
   public AppointmentResponseDto create(UUID patientId, CreateAppointmentRequestDto request) {
      //TODO: notification after successful creation
      Clinic clinic = ClinicContext.get();

      Patient patient = patientRepository.findById(patientId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      Doctor doctor = doctorRepository.findByIdAndActiveTrue(request.doctorId())
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      List<ResolvedAppointmentService> resolvedServices = new ArrayList<>();

      int totalPrice = 0;
      int totalDuration = 0;

      for (AppointmentServiceRequestDto serviceRequest : request.services()) {
         ClinicService clinicService = clinicServiceRepository.findByIdAndActiveTrue(serviceRequest.clinicServiceId())
                                                              .orElseThrow(() -> new ResourceNotFoundException("Clinic service not found"));

         resolvedServices.add(new ResolvedAppointmentService(clinicService, serviceRequest.quantity()));
         totalPrice += clinicService.getPrice() * serviceRequest.quantity();
         totalDuration += clinicService.getDurationMinutes() * serviceRequest.quantity();
      }

      LocalDateTime scheduledAt = request.scheduledAt();
      LocalDateTime endAt = scheduledAt.plusMinutes(totalDuration);

      validateDoctorAvailability(doctor, scheduledAt, endAt);
      validateNoAppointmentOverlap(doctor, scheduledAt, endAt);

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
         item.setPrice(clinicService.getPrice());
         item.setDurationMinutes(clinicService.getDurationMinutes());
         item.setQuantity(resolvedService.quantity());
         item.setPosition(position++);
         
         appointmentServiceRepository.save(item);
      }

      return appointmentMapper.toResponse(saved);
   }

   @Transactional
   public AppointmentResponseDto cancel(UUID patientId, UUID appointmentId) {
      Appointment appointment = appointmentRepository.findByIdAndPatient_Id(appointmentId, patientId)
                                                     .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

      if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
         throw new BadRequestException("Appointment is already cancelled");
      }

      if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
         throw new BadRequestException("Completed appointment cannot be cancelled");
      }

      appointment.setStatus(AppointmentStatus.CANCELLED);

      return appointmentMapper.toResponse(appointment);
   }

   @Transactional
   public AppointmentResponseDto createForPatient(UUID userId, CreateAppointmentRequestDto request) {
      Patient patient = patientRepository.findByUser_Id(userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return create(patient.getId(), request);
   }

   @Transactional
   public AppointmentResponseDto cancelByPatient(UUID userId, UUID appointmentId) {
      Patient patient = patientRepository.findByUser_Id(userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return cancel(patient.getId(), appointmentId);
   }

   //TODO: for staff methods add user notifications if found by patient id
   @Transactional
   public AppointmentResponseDto createForStaff(UUID patientId, CreateAppointmentRequestDto request) {
      patientRepository.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return create(patientId, request);
   }

   @Transactional
   public AppointmentResponseDto cancelByStaff(UUID patientId, UUID appointmentId) {
      patientRepository.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return cancel(patientId, appointmentId);
   }

   @Transactional
   public AppointmentResponseDto changeStatusByStaff(UUID patientId, UUID appointmentId, ChangeAppointmentStatusRequestDto request) {
      Appointment appointment = appointmentRepository.findByIdAndPatient_Id(appointmentId, patientId)
                                                     .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

      validateTransition(appointment.getStatus(), request.status());

      appointment.setStatus(request.status());

      return appointmentMapper.toResponse(appointment);
   }

   private void validateTransition(AppointmentStatus from, AppointmentStatus to) {
      if (from == to) {
         throw new BadRequestException("Appointment already has this status");
      }

      if (!from.canTransitionTo(to)) {
         throw new BadRequestException("Cannot change appointment status from " + from + " to " + to);
      }
   }

   //TODO: Complete CRUD
}

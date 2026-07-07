package org.bublapi.dent.appointment.service;

import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
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
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   private final AppointmentMapper appointmentMapper;

   public AppointmentService(AppointmentRepository appointmentRepository, AppointmentServiceRepository appointmentServiceRepository, ClinicServiceRepository clinicServiceRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, AppointmentMapper appointmentMapper) {
      this.appointmentRepository = appointmentRepository;
      this.appointmentServiceRepository = appointmentServiceRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.patientRepository = patientRepository;
      this.doctorRepository = doctorRepository;
      this.appointmentMapper = appointmentMapper;
   }

   public AppointmentResponseDto create(UUID patientId, CreateAppointmentRequestDto request) {
      //TODO: Compare to Doctor's working hours and exceptions and check appointments overlaps
      //TODO: notification after successful creation
      Clinic clinic = ClinicContext.get();

      Patient patient = patientRepository.findById(patientId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      Doctor doctor = doctorRepository.findByIdAndActiveTrue(request.doctorId())
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      List<ClinicService> clinicServices = new ArrayList<>();

      int totalPrice = 0;
      int totalDuration = 0;

      for (UUID serviceId : request.serviceIds()) {
         ClinicService clinicService = clinicServiceRepository.findByIdAndActiveTrue(serviceId)
                                                              .orElseThrow(() -> new ResourceNotFoundException("Clinic service not found"));

         clinicServices.add(clinicService);
         totalPrice += clinicService.getPrice();
         totalDuration += clinicService.getDurationMinutes();
      }

      Appointment appointment = appointmentMapper.toEntity(request);

      appointment.setClinic(clinic);
      appointment.setPatient(patient);
      appointment.setDoctor(doctor);
      appointment.setEndAt(appointment.getScheduledAt().plusMinutes(totalDuration));
      appointment.setTotalPrice(totalPrice);
      appointment.setStatus(AppointmentStatus.CREATED);

      Appointment saved = appointmentRepository.save(appointment);

      int position = 1;

      for (ClinicService clinicService : clinicServices) {
         AppointmentServiceItem item = new AppointmentServiceItem();

         item.setAppointment(saved);
         saved.getServices().add(item);
         item.setClinicService(clinicService);
         item.setPrice(clinicService.getPrice());
         item.setDurationMinutes(clinicService.getDurationMinutes());
         item.setQuantity(1);
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

package org.bublapi.dent.appointment.service;

import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment.mapper.AppointmentMapper;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.bublapi.dent.appointment_service.repository.AppointmentServiceRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
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
   private final ClinicRepository clinicRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final PatientRepository patientRepository;
   private final DoctorRepository doctorRepository;
   private final AppointmentMapper appointmentMapper;

   public AppointmentService(AppointmentRepository appointmentRepository, AppointmentServiceRepository appointmentServiceRepository, ClinicRepository clinicRepository, ClinicServiceRepository clinicServiceRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, AppointmentMapper appointmentMapper) {
      this.appointmentRepository = appointmentRepository;
      this.appointmentServiceRepository = appointmentServiceRepository;
      this.clinicRepository = clinicRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.patientRepository = patientRepository;
      this.doctorRepository = doctorRepository;
      this.appointmentMapper = appointmentMapper;
   }

   @Transactional
   public AppointmentResponseDto create(UUID clinicId, UUID patientId, CreateAppointmentRequestDto request) {
      Clinic clinic = clinicRepository.findByIdAndActiveTrue(clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      Patient patient = patientRepository.findByIdAndClinic_Id(patientId, clinicId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      Doctor doctor = doctorRepository.findAvailableDoctorInClinic(clinicId, request.doctorId())
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      List<ClinicService> clinicServices = new ArrayList<>();

      int totalPrice = 0;
      int totalDuration = 0;

      for (UUID serviceId : request.serviceIds()) {
         ClinicService clinicService = clinicServiceRepository.findByIdAndClinic_IdAndActiveTrue(serviceId, clinicId)
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
         item.setClinicService(clinicService);
         item.setPrice(clinicService.getPrice());
         item.setDurationMinutes(clinicService.getDurationMinutes());
         item.setQuantity(1);
         item.setPosition(position++);

         appointmentServiceRepository.save(item);
      }

      return appointmentMapper.toResponse(saved);
   }
}

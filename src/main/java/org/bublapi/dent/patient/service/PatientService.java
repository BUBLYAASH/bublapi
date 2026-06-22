package org.bublapi.dent.patient.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.patient.dto.CreatePatientFromProfileRequestDto;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.mapper.PatientMapper;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

   private final PatientRepository patientRepository;
   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;
   private final PatientMapper patientMapper;

   public PatientService(PatientRepository patientRepository, ClinicRepository clinicRepository, UserRepository userRepository, PatientMapper patientMapper) {
      this.patientRepository = patientRepository;
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.patientMapper = patientMapper;
   }

   public PatientResponseDto create(UUID clinicId, CreatePatientRequestDto request) {
      Clinic clinic = clinicRepository.findById(clinicId).orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      Patient patient = patientMapper.toEntity(request);

      if ((request.email() != null && !request.email().isBlank()) || (request.phone() != null && !request.phone().isBlank())) {
         userRepository.findByEmailOrPhoneInClinic(clinicId, request.email(), request.phone()).ifPresent(u -> {
            if (patientRepository.findByUser_Id(u.getId()).isEmpty()) {
               patient.setUser(u);
            }
         });
      }

      patient.setClinic(clinic);

      Patient saved = patientRepository.save(patient);

      return patientMapper.toResponse(saved);
   }

   @Transactional
   public PatientResponseDto createFromProfile(UUID clinicId, UUID userId, CreatePatientFromProfileRequestDto request) {
      Clinic clinic = clinicRepository.findByIdAndActiveTrue(clinicId).orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      User user = userRepository.findByIdAndClinic_Id(userId, clinicId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      patientRepository.findByUser_Id(user.getId()).ifPresent(t -> {
         throw new BadRequestException("User already has patient card");
      });

      Patient patient = patientMapper.toEntity(request);

      patient.setFirstName(user.getFirstName());
      patient.setLastName(user.getLastName());
      patient.setMiddleName(user.getMiddleName());
      patient.setEmail(user.getEmail());
      patient.setPhone(user.getPhone());
      patient.setUser(user);
      patient.setClinic(clinic);

      Patient saved = patientRepository.save(patient);

      return patientMapper.toResponse(saved);
   }

   @Transactional
   public PatientResponseDto update(UUID clinicId, UUID patientId, UpdatePatientRequestDto request) {
      Patient patient = patientRepository.findByIdAndClinic_Id(patientId, clinicId).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      patientMapper.updateEntity(request, patient);

      return patientMapper.toResponse(patient);
   }

   public PatientResponseDto getByUserId(UUID clinicId, UUID userId) {
      clinicRepository.findByIdAndActiveTrue(clinicId).orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      Patient patient = patientRepository.findByUser_IdAndClinic_Id(userId, clinicId).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return patientMapper.toResponse(patient);
   }

   public List<PatientResponseDto> findAll(UUID clinicId) {
      clinicRepository.findByIdAndActiveTrue(clinicId).orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      return patientRepository.findAllByClinic_Id(clinicId).stream().map(patientMapper::toResponse).toList();
   }
}

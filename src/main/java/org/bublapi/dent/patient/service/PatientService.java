package org.bublapi.dent.patient.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.patient.dto.CreatePatientFromProfileRequestDto;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientByPhoneRequestDto;
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
   private final UserRepository userRepository;
   private final PatientMapper patientMapper;

   public PatientService(PatientRepository patientRepository, UserRepository userRepository, PatientMapper patientMapper) {
      this.patientRepository = patientRepository;
      this.userRepository = userRepository;
      this.patientMapper = patientMapper;
   }

   public PatientResponseDto create(CreatePatientRequestDto request) {
      Clinic clinic = ClinicContext.get();

      Patient patient = patientMapper.toEntity(request);

      if ((request.email() != null && !request.email().isBlank()) || (request.phone() != null && !request.phone()
                                                                                                         .isBlank())) {
         userRepository.findByEmailOrPhone(request.email(), request.phone()).ifPresent(u -> {
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
   public PatientResponseDto createFromProfile(UUID userId, CreatePatientFromProfileRequestDto request) {
      Clinic clinic = ClinicContext.get();

      User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      if (patientRepository.existsByUser_Id(user.getId())) {
         throw new BadRequestException("User already has patient card");
      }

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
   public PatientResponseDto update(UUID patientId, UpdatePatientRequestDto request) {
      Patient patient = patientRepository.findById(patientId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      patientMapper.updateEntity(request, patient);

      return patientMapper.toResponse(patient);
   }

   @Transactional
   public PatientResponseDto updateByUserId(UUID userId, UpdatePatientRequestDto request) {
      Patient patient = patientRepository.findByUser_Id(userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      patientMapper.updateEntity(request, patient);

      return patientMapper.toResponse(patient);
   }

   public PatientResponseDto getByUserId(UUID userId) {
      Patient patient = patientRepository.findByUser_Id(userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return patientMapper.toResponse(patient);
   }

   public PatientResponseDto getByPhone(PatientByPhoneRequestDto request) {
      Patient patient = patientRepository.findByPhone(request.phone())
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return patientMapper.toResponse(patient);
   }

   public List<PatientResponseDto> findAll() {
      return patientRepository.findAll().stream().map(patientMapper::toResponse).toList();
   }
}

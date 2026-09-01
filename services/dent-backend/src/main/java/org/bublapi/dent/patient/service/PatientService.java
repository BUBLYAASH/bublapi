package org.bublapi.dent.patient.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.logging.UserAuditService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class PatientService {

   private final PatientRepository patientRepository;
   private final UserRepository userRepository;
   private final PatientMapper patientMapper;
   private final UserAuditService userAuditService;

   public PatientService(PatientRepository patientRepository, UserRepository userRepository,
                         PatientMapper patientMapper, UserAuditService userAuditService) {
      this.patientRepository = patientRepository;
      this.userRepository = userRepository;
      this.patientMapper = patientMapper;
      this.userAuditService = userAuditService;
   }

   private static List<String> getChangedFields(Patient patient, UpdatePatientRequestDto request) {
      List<String> changedFields = new ArrayList<>();

      if (request.firstName() != null && !Objects.equals(patient.getFirstName(), request.firstName())) {
         changedFields.add("firstName");
      }

      if (request.lastName() != null && !Objects.equals(patient.getLastName(), request.lastName())) {
         changedFields.add("lastName");
      }

      if (request.middleName() != null && !Objects.equals(patient.getMiddleName(), request.middleName())) {
         changedFields.add("middleName");
      }

      if (request.phone() != null) {
         String normalizedPhone = request.phone().trim();

         if (!Objects.equals(patient.getPhone(), normalizedPhone)) {
            changedFields.add("phone");
         }
      }

      if (request.email() != null) {
         String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

         if (!Objects.equals(patient.getEmail(), normalizedEmail)) {
            changedFields.add("email");
         }
      }

      if (request.birthDate() != null && !Objects.equals(patient.getBirthDate(), request.birthDate())) {
         changedFields.add("birthDate");
      }

      if (request.notes() != null && !Objects.equals(patient.getNotes(), request.notes())) {
         changedFields.add("notes");
      }

      if (request.allergies() != null && !Objects.equals(patient.getAllergies(), request.allergies())) {
         changedFields.add("allergies");
      }

      if (request.chronicDiseases() != null && !Objects.equals(patient.getChronicDiseases(),
                                                               request.chronicDiseases())) {
         changedFields.add("chronicDiseases");
      }

      return changedFields;
   }

   @Transactional
   public PatientResponseDto create(CreatePatientRequestDto request) {
      Clinic clinic = ClinicContext.get();
      String email = request.email().trim().toLowerCase(Locale.ROOT);
      String phone = request.phone().trim();

      Patient patient = patientMapper.toEntity(request);

      if (!email.isBlank() || !phone.isBlank()) {
         userRepository.findByEmailOrPhoneInClinic(email, phone, clinic.getId()).ifPresent(u -> {
            if (patientRepository.findByClinic_IdAndUser_Id(clinic.getId(), u.getId()).isEmpty()) {
               patient.setUser(u);
            }
         });
      }

      patient.setClinic(clinic);

      Patient saved = patientRepository.save(patient);

      userAuditService.patientCreated(saved.getId());

      return patientMapper.toResponse(saved);
   }

   @Transactional
   public PatientResponseDto createFromProfile(UUID userId, CreatePatientFromProfileRequestDto request) {
      Clinic clinic = ClinicContext.get();

      User user = userRepository.findByIdAndClinic_Id(userId, clinic.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      if (patientRepository.existsByClinic_IdAndUser_Id(clinic.getId(), user.getId())) {
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

      userAuditService.patientCreated(saved.getId());

      return patientMapper.toResponse(saved);
   }

   @Transactional
   public PatientResponseDto update(UUID patientId, UpdatePatientRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndId(clinicId, patientId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      List<String> changedFields = getChangedFields(patient, request);

      String email = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
      String phone = request.phone() == null ? null : request.phone().trim();

      patientMapper.updateEntity(request, patient);

      if (email != null && !email.isBlank()) {
         patient.setEmail(email);
      }

      if (phone != null && !phone.isBlank()) {
         patient.setPhone(phone);
      }

      userAuditService.patientUpdated(patientId, changedFields);

      return patientMapper.toResponse(patient);
   }

   @Transactional
   public PatientResponseDto updateByUserId(UUID userId, UpdatePatientRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndUser_Id(clinicId, userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      List<String> changedFields = getChangedFields(patient, request);

      patientMapper.updateEntity(request, patient);

      userAuditService.patientUpdated(patient.getId(), changedFields);

      return patientMapper.toResponse(patient);
   }

   public PatientResponseDto getByUserId(UUID userId) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndUser_Id(clinicId, userId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return patientMapper.toResponse(patient);
   }

   public PatientResponseDto getByPhone(PatientByPhoneRequestDto request) {
      String phone = request.phone().trim();

      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndPhone(clinicId, phone)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return patientMapper.toResponse(patient);
   }

   public PatientResponseDto findByPatientId(UUID patientId) {
      UUID clinicId = ClinicContext.getClinicId();

      Patient patient = patientRepository.findByClinic_IdAndId(clinicId, patientId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

      return patientMapper.toResponse(patient);
   }

   public List<PatientResponseDto> findAll() {
      UUID clinicId = ClinicContext.getClinicId();

      return patientRepository.findAllByClinic_Id(clinicId).stream().map(patientMapper::toResponse).toList();
   }
}

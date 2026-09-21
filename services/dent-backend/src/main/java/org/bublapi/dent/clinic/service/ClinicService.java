package org.bublapi.dent.clinic.service;

import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.mapper.ClinicMapper;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.logging.AdministrativeAuditService;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ClinicService {

   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;
   private final DoctorRepository doctorRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final ClinicMapper clinicMapper;
   private final AdministrativeAuditService administrativeAuditService;

   public ClinicService(ClinicRepository clinicRepository, UserRepository userRepository,
                        DoctorRepository doctorRepository, ClinicServiceRepository clinicServiceRepository,
                        ClinicMapper clinicMapper, AdministrativeAuditService administrativeAuditService) {
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.doctorRepository = doctorRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.clinicMapper = clinicMapper;
      this.administrativeAuditService = administrativeAuditService;
   }

   @Transactional
   public ClinicResponseDto create(CreateClinicRequestDto request) {
      Clinic clinic = clinicMapper.toEntity(request);

      Clinic saved = clinicRepository.save(clinic);

      administrativeAuditService.clinicCreated(saved.getId());

      return clinicMapper.toResponse(saved);
   }

   @Transactional
   public ClinicResponseDto update(UUID id, UpdateClinicRequestDto request) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      List<String> changedFields = getChangedFields(clinic, request);

      clinicMapper.updateEntity(request, clinic);

      administrativeAuditService.clinicUpdated(clinic.getId(), changedFields);

      return clinicMapper.toResponse(clinic);
   }

   @Transactional
   public ClinicResponseDto deactivate(UUID id) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      userRepository.disableAllByClinicId(clinic.getId());
      doctorRepository.disableAllByClinicId(clinic.getId());
      clinicServiceRepository.disableAllByClinicId(clinic.getId());

      clinic.setActive(false);

      administrativeAuditService.clinicDeactivated(clinic.getId());

      return clinicMapper.toResponse(clinic);
   }

   @Transactional
   public ClinicResponseDto activate(UUID id) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      clinic.setActive(true);

      userRepository.enableAllDisabledByClinic(clinic.getId());
      doctorRepository.enableAllDisabledByClinic(clinic.getId());
      clinicServiceRepository.enableAllDisabledByClinic(clinic.getId());

      administrativeAuditService.clinicActivated(clinic.getId());

      return clinicMapper.toResponse(clinic);
   }

   public List<ClinicResponseDto> findAll() {
      return clinicRepository.findAll().stream().map(clinicMapper::toResponse).toList();
   }

   private List<String> getChangedFields(Clinic clinic, UpdateClinicRequestDto request) {
      List<String> changedFields = new ArrayList<>();

      if (request.title() != null && !Objects.equals(request.title(), clinic.getTitle())) {
         changedFields.add("title");
      }

      if (request.description() != null && !Objects.equals(request.description(), clinic.getDescription())) {
         changedFields.add("description");
      }

      if (request.address() != null && !Objects.equals(request.address(), clinic.getAddress())) {
         changedFields.add("address");
      }

      if (request.phone() != null && !Objects.equals(request.phone(), clinic.getPhone())) {
         changedFields.add("phone");
      }

      if (request.email() != null && !Objects.equals(request.email(), clinic.getEmail())) {
         changedFields.add("email");
      }

      if (request.website() != null && !Objects.equals(request.website(), clinic.getWebsite())) {
         changedFields.add("website");
      }

      if (request.timezone() != null && !Objects.equals(request.timezone(), clinic.getTimezone())) {
         changedFields.add("timezone");
      }

      return changedFields;
   }
}

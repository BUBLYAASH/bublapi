package org.bublapi.dent.clinic.service;

import jakarta.persistence.EntityManager;
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

import java.util.List;
import java.util.UUID;

@Service
public class ClinicService {

   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;
   private final DoctorRepository doctorRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final ClinicMapper clinicMapper;
   private final EntityManager entityManager;
   private final AdministrativeAuditService administrativeAuditService;

   public ClinicService(ClinicRepository clinicRepository, UserRepository userRepository,
                        DoctorRepository doctorRepository, ClinicServiceRepository clinicServiceRepository,
                        ClinicMapper clinicMapper, EntityManager entityManager,
                        AdministrativeAuditService administrativeAuditService) {
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.doctorRepository = doctorRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.clinicMapper = clinicMapper;
      this.entityManager = entityManager;
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

      clinicMapper.updateEntity(request, clinic);

      administrativeAuditService.clinicUpdated(clinic.getId());

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
}

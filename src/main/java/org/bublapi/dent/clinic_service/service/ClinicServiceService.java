package org.bublapi.dent.clinic_service.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.clinic_service.dto.AddClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.dto.ClinicServiceResponseDto;
import org.bublapi.dent.clinic_service.dto.UpdateClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.mapper.ClinicServiceMapper;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.dental_service.repository.DentalServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClinicServiceService {
   private final ClinicServiceRepository clinicServiceRepository;
   private final DentalServiceRepository dentalServiceRepository;
   private final ClinicRepository clinicRepository;
   private final ClinicServiceMapper clinicServiceMapper;

   public ClinicServiceService(ClinicServiceRepository clinicServiceRepository, DentalServiceRepository dentalServiceRepository, ClinicRepository clinicRepository, ClinicServiceMapper clinicServiceMapper) {
      this.clinicServiceRepository = clinicServiceRepository;
      this.dentalServiceRepository = dentalServiceRepository;
      this.clinicRepository = clinicRepository;
      this.clinicServiceMapper = clinicServiceMapper;
   }

   public ClinicServiceResponseDto add(UUID clinicId, UUID dentalServiceId, AddClinicServiceRequestDto request) {
      Clinic clinic = clinicRepository.findByIdAndActiveTrue(clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      if (clinicServiceRepository.existsByClinic_IdAndDentalService_Id(clinicId, dentalServiceId)) {
         throw new BadRequestException("Dental Service is already in this clinic");
      }

      DentalService dentalService = dentalServiceRepository.findByIdAndActiveTrue(dentalServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Dental Service not found or unavailable"));

      ClinicService clinicService = clinicServiceMapper.toEntity(request);

      clinicService.setClinic(clinic);
      clinicService.setDentalService(dentalService);

      ClinicService saved = clinicServiceRepository.save(clinicService);

      return clinicServiceMapper.toResponse(saved);
   }

   @Transactional
   public ClinicServiceResponseDto update(UUID clinicId, UUID clinicServiceId, UpdateClinicServiceRequestDto request) {
      clinicRepository.findByIdAndActiveTrue(clinicId)
                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      ClinicService clinicService = clinicServiceRepository.findByIdAndClinic_Id(clinicServiceId, clinicId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic Service is not found"));

      clinicServiceMapper.updateEntity(request, clinicService);

      return clinicServiceMapper.toResponse(clinicService);
   }

   @Transactional
   public ClinicServiceResponseDto deactivate(UUID clinicId, UUID clinicServiceId) {
      //TODO: after adding notifications - notify user that service that them chosen - currently unavailable
      ClinicService clinicService = clinicServiceRepository.findByIdAndClinic_Id(clinicServiceId, clinicId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic Service not found or unavailable"));

      clinicService.setActive(false);

      return clinicServiceMapper.toResponse(clinicService);
   }

   @Transactional
   public ClinicServiceResponseDto activate(UUID clinicId, UUID clinicServiceId) {
      ClinicService clinicService = clinicServiceRepository.findByIdAndClinic_Id(clinicServiceId, clinicId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic Service not found"));

      clinicService.setActive(true);

      return clinicServiceMapper.toResponse(clinicService);
   }

   public List<ClinicServiceResponseDto> findAllActiveForPublic(UUID clinicId) {
      clinicRepository.findByIdAndActiveTrue(clinicId)
                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      return clinicServiceRepository.findAllByClinic_IdAndActiveTrue(clinicId)
                                    .stream()
                                    .map(clinicServiceMapper::toResponse)
                                    .toList();
   }

   public ClinicServiceResponseDto findById(UUID clinicId, UUID clinicServiceId) {
      clinicRepository.findByIdAndActiveTrue(clinicId)
                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      ClinicService clinicService = clinicServiceRepository.findByIdAndClinic_IdAndActiveTrue(clinicServiceId, clinicId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic service not found"));

      return clinicServiceMapper.toResponse(clinicService);
   }

   public List<ClinicServiceResponseDto> findAllForStaff(UUID clinicId) {
      return clinicServiceRepository.findAllByClinic_Id(clinicId)
                                    .stream()
                                    .map(clinicServiceMapper::toResponse)
                                    .toList();
   }
}

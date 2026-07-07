package org.bublapi.dent.clinic_service.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClinicServiceService {
   private final ClinicServiceRepository clinicServiceRepository;
   private final DentalServiceRepository dentalServiceRepository;
   private final ClinicServiceMapper clinicServiceMapper;

   public ClinicServiceService(ClinicServiceRepository clinicServiceRepository, DentalServiceRepository dentalServiceRepository, ClinicServiceMapper clinicServiceMapper) {
      this.clinicServiceRepository = clinicServiceRepository;
      this.dentalServiceRepository = dentalServiceRepository;
      this.clinicServiceMapper = clinicServiceMapper;
   }

   public ClinicServiceResponseDto add(UUID dentalServiceId, AddClinicServiceRequestDto request) {
      Clinic clinic = ClinicContext.get();

      if (clinicServiceRepository.existsByDentalService_Id(dentalServiceId)) {
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
   public ClinicServiceResponseDto update(UUID clinicServiceId, UpdateClinicServiceRequestDto request) {
      ClinicService clinicService = clinicServiceRepository.findById(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic Service is not found"));

      clinicServiceMapper.updateEntity(request, clinicService);

      return clinicServiceMapper.toResponse(clinicService);
   }

   @Transactional
   public ClinicServiceResponseDto deactivate(UUID clinicServiceId) {
      //TODO: after adding notifications - notify user that service that them chosen - currently unavailable
      ClinicService clinicService = clinicServiceRepository.findById(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic Service not found or unavailable"));

      clinicService.setActive(false);

      return clinicServiceMapper.toResponse(clinicService);
   }

   @Transactional
   public ClinicServiceResponseDto activate(UUID clinicServiceId) {
      ClinicService clinicService = clinicServiceRepository.findById(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic Service not found"));

      clinicService.setActive(true);

      return clinicServiceMapper.toResponse(clinicService);
   }

   public List<ClinicServiceResponseDto> findAllActiveForPublic() {
      return clinicServiceRepository.findAllByActiveTrue().stream().map(clinicServiceMapper::toResponse).toList();
   }

   public ClinicServiceResponseDto findById(UUID clinicServiceId) {
      ClinicService clinicService = clinicServiceRepository.findByIdAndActiveTrue(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException("Clinic service not found"));

      return clinicServiceMapper.toResponse(clinicService);
   }

   public List<ClinicServiceResponseDto> findAllForStaff() {
      return clinicServiceRepository.findAll().stream().map(clinicServiceMapper::toResponse).toList();
   }
}

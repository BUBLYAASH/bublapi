package org.bublapi.dent.dental_service.service;

import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.dental_service.dto.CreateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.dto.DentalServiceResponseDto;
import org.bublapi.dent.dental_service.dto.UpdateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.dental_service.mapper.DentalServiceMapper;
import org.bublapi.dent.dental_service.repository.DentalServiceRepository;
import org.bublapi.dent.logging.AdministrativeAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DentalServiceService {
   private final DentalServiceRepository dentalServiceRepository;
   private final DentalServiceMapper dentalServiceMapper;
   private final AdministrativeAuditService administrativeAuditService;

   public DentalServiceService(DentalServiceRepository dentalServiceRepository, DentalServiceMapper dentalServiceMapper,
                               AdministrativeAuditService administrativeAuditService) {
      this.dentalServiceRepository = dentalServiceRepository;
      this.dentalServiceMapper = dentalServiceMapper;
      this.administrativeAuditService = administrativeAuditService;
   }

   public DentalServiceResponseDto create(CreateDentalServiceRequestDto request) {
      if (dentalServiceRepository.existsByTitle(request.title())) {
         throw new BadRequestException("Service with this title already exists");
      }

      DentalService dentalService = dentalServiceMapper.toEntity(request);

      DentalService saved = dentalServiceRepository.save(dentalService);

      administrativeAuditService.dentalServiceCreated(saved.getId());

      return dentalServiceMapper.toResponse(saved);
   }

   @Transactional
   public DentalServiceResponseDto update(UUID dentalServiceId, UpdateDentalServiceRequestDto request) {
      DentalService dentalService = dentalServiceRepository.findByIdAndActiveTrue(dentalServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Dental Service not found or unavailable"));

      if (request.title() != null && dentalServiceRepository.existsByTitle(request.title()) && !request.title()
                                                                                                       .equals(dentalService.getTitle())) {
         throw new BadRequestException("Service with this title already exists");
      }

      dentalServiceMapper.updateEntity(request, dentalService);

      administrativeAuditService.dentalServiceUpdated(dentalService.getId());

      return dentalServiceMapper.toResponse(dentalService);
   }

   public List<DentalServiceResponseDto> findAll() {
      return dentalServiceRepository.findAll().stream().map(dentalServiceMapper::toResponse).toList();
   }

   public DentalServiceResponseDto findById(UUID dentalServiceId) {
      DentalService dentalService = dentalServiceRepository.findByIdAndActiveTrue(dentalServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Dental Service not found or unavailable"));

      return dentalServiceMapper.toResponse(dentalService);
   }

   public List<DentalServiceResponseDto> findAllActive() {
      return dentalServiceRepository.findAllByActiveTrue().stream().map(dentalServiceMapper::toResponse).toList();
   }

   @Transactional
   public DentalServiceResponseDto deactivate(UUID dentalServiceId) {
      DentalService dentalService = dentalServiceRepository.findByIdAndActiveTrue(dentalServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Dental Service not found or unavailable"));
      dentalService.setActive(false);

      administrativeAuditService.dentalServiceDeactivated(dentalService.getId());

      return dentalServiceMapper.toResponse(dentalService);
   }

   @Transactional
   public DentalServiceResponseDto activate(UUID dentalServiceId) {
      DentalService dentalService = dentalServiceRepository.findById(dentalServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Dental Service not found"));
      dentalService.setActive(true);

      administrativeAuditService.dentalServiceActivated(dentalService.getId());

      return dentalServiceMapper.toResponse(dentalService);
   }
}

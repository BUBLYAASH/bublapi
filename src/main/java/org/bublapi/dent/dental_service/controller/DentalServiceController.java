package org.bublapi.dent.dental_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.dental_service.dto.CreateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.dto.DentalServiceResponseDto;
import org.bublapi.dent.dental_service.dto.UpdateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.service.DentalServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dental Services")
@RestController
@RequestMapping("/api/catalog/dental-services")
public class DentalServiceController {
   private final DentalServiceService dentalServiceService;

   public DentalServiceController(DentalServiceService dentalServiceService) {
      this.dentalServiceService = dentalServiceService;
   }

   @Operation(summary = "Create new service", description = "Create a new service in global catalog")
   @PostMapping
   public DentalServiceResponseDto create(@Valid @RequestBody CreateDentalServiceRequestDto request) {
      return dentalServiceService.create(request);
   }

   @Operation(summary = "Update a service", description = "Update a service by provided information")
   @PatchMapping("/{dentalServiceId}")
   public DentalServiceResponseDto update(@PathVariable UUID dentalServiceId, @Valid @RequestBody UpdateDentalServiceRequestDto request) {
      return dentalServiceService.update(dentalServiceId, request);
   }

   @Operation(summary = "Get all services", description = "Get all services from global catalog")
   @GetMapping
   public List<DentalServiceResponseDto> findAll() {
      return dentalServiceService.findAll();
   }

   @Operation(summary = "Get information about one service", description = "Get all information about one service by ID")
   @GetMapping("/{dentalServiceId}")
   public DentalServiceResponseDto findById(@PathVariable UUID dentalServiceId) {
      return dentalServiceService.findById(dentalServiceId);
   }

   @Operation(summary = "Deactivate a service", description = "Deactivates a service by ID")
   @PatchMapping("/{dentalServiceId}/deactivation")
   public DentalServiceResponseDto deactivate(@PathVariable UUID dentalServiceId) {
      return dentalServiceService.deactivate(dentalServiceId);
   }

   @Operation(summary = "Activate a service", description = "Activates a service by ID")
   @PatchMapping("/{dentalServiceId}/activation")
   public DentalServiceResponseDto activate(@PathVariable UUID dentalServiceId) {
      return dentalServiceService.activate(dentalServiceId);
   }
}

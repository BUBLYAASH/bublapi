package org.bublapi.dent.clinic_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.clinic_service.dto.AddClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.dto.ClinicServiceResponseDto;
import org.bublapi.dent.clinic_service.dto.UpdateClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.service.ClinicServiceService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Clinic Services")
@RestController
@RequestMapping("/api/clinics/{clinicId}/services")
public class ClinicServiceController {
   private final ClinicServiceService clinicServiceService;

   public ClinicServiceController(ClinicServiceService clinicServiceService) {
      this.clinicServiceService = clinicServiceService;
   }

   @Operation(summary = "Add new clinic service", description = "Add new clinic service from global catalog")
   @PostMapping("/{dentalServiceId}")
   public ClinicServiceResponseDto addService(@PathVariable UUID clinicId, @PathVariable UUID dentalServiceId, @Valid @RequestBody AddClinicServiceRequestDto request) {
      return clinicServiceService.add(clinicId, dentalServiceId, request);
   }

   @Operation(summary = "Update a clinic service", description = "Update provided fields in a clinic service by ID")
   @PatchMapping("/{clinicServiceId}")
   public ClinicServiceResponseDto updateService(@PathVariable UUID clinicId, @PathVariable UUID clinicServiceId, @Valid @RequestBody UpdateClinicServiceRequestDto request) {
      return clinicServiceService.update(clinicId, clinicServiceId, request);
   }

   @Operation(summary = "Deactivate a clinic service", description = "Deactivates a clinic service by ID")
   @PatchMapping("/{clinicServiceId}/deactivation")
   public ClinicServiceResponseDto deactivateService(@PathVariable UUID clinicId, @PathVariable UUID clinicServiceId) {
      return clinicServiceService.deactivate(clinicId, clinicServiceId);
   }

   @Operation(summary = "Activate a clinic service", description = "Activates a clinic service by ID")
   @PatchMapping("/{clinicServiceId}/activation")
   public ClinicServiceResponseDto activateService(@PathVariable UUID clinicId, @PathVariable UUID clinicServiceId) {
      return clinicServiceService.activate(clinicId, clinicServiceId);
   }
}

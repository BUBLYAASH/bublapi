package org.bublapi.dent.clinic_service.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.clinic_service.dto.ClinicServiceResponseDto;
import org.bublapi.dent.clinic_service.service.ClinicServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public Clinic Services")
@RestController
@RequestMapping("/api/public/clinics/{clinicId}/services")
public class PublicClinicServiceController {
   private final ClinicServiceService clinicServiceService;

   public PublicClinicServiceController(ClinicServiceService clinicServiceService) {
      this.clinicServiceService = clinicServiceService;
   }

   @Operation(summary = "Get all active services in clinic", description = "Gets all active services in current clinic")
   @GetMapping
   public List<ClinicServiceResponseDto> findAll(@PathVariable UUID clinicId) {
      return clinicServiceService.findAllActiveForPublic(clinicId);
   }

   @Operation(summary = "Get information about one service in clinic", description = "Get information about one service in current clinic")
   @GetMapping("/{clinicServiceId}")
   public ClinicServiceResponseDto findById(@PathVariable UUID clinicId, @PathVariable UUID clinicServiceId) {
      return clinicServiceService.findById(clinicId, clinicServiceId);
   }
}

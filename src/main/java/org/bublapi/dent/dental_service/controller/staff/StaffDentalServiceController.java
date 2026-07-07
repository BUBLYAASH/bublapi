package org.bublapi.dent.dental_service.controller.staff;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.dental_service.dto.DentalServiceResponseDto;
import org.bublapi.dent.dental_service.service.DentalServiceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Global dental services catalog")
@RestController
@RequestMapping("/api/catalog/dental-services")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("""
        hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST')
        and @clinicSecurity.hasAccess(authentication)
        """)
public class StaffDentalServiceController {
   private final DentalServiceService dentalServiceService;

   public StaffDentalServiceController(DentalServiceService dentalServiceService) {
      this.dentalServiceService = dentalServiceService;
   }

   @Operation(summary = "Get all active services from catalog", description = "Gets all active services from global catalog")
   @GetMapping
   public List<DentalServiceResponseDto> findAllActive() {
      return dentalServiceService.findAllActive();
   }

}

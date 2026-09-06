package org.bublapi.dent.clinic_service.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.clinic_service.dto.ClinicServiceResponseDto;
import org.bublapi.dent.clinic_service.service.ClinicServiceService;
import org.bublapi.dent.doctor_clinic_service.dto.DoctorClinicServiceResponseDto;
import org.bublapi.dent.doctor_clinic_service.service.DoctorClinicServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public Clinic Services")
@RestController
@RequestMapping("/api/public/services")
@SecurityRequirement(name = "apiKey")
public class PublicClinicServiceController {
   private final ClinicServiceService clinicServiceService;
   private final DoctorClinicServiceService doctorClinicServiceService;

   public PublicClinicServiceController(ClinicServiceService clinicServiceService,
                                        DoctorClinicServiceService doctorClinicServiceService) {
      this.clinicServiceService = clinicServiceService;
      this.doctorClinicServiceService = doctorClinicServiceService;
   }

   @Operation(summary = "Get all active services in clinic", description = "Gets all active services in current clinic")
   @GetMapping
   public List<ClinicServiceResponseDto> findAll() {
      return clinicServiceService.findAllActiveForPublic();
   }

   @Operation(summary = "Get information about one service in clinic", description = "Get information about one service in current clinic")
   @GetMapping("/{clinicServiceId}")
   public ClinicServiceResponseDto findById(@PathVariable UUID clinicServiceId) {
      return clinicServiceService.findById(clinicServiceId);
   }

   @Operation(summary = "Get all doctors provided a service", description = "Get all doctors provided a service")
   @GetMapping("/{clinicServiceId}/doctors")
   public List<DoctorClinicServiceResponseDto> getDoctors(@PathVariable UUID clinicServiceId) {
      return doctorClinicServiceService.findAllByClinicServiceId(clinicServiceId);
   }
}

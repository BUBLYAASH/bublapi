package org.bublapi.dent.patient.controller.patient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.auth.security.CustomUserDetails;
import org.bublapi.dent.patient.dto.CreatePatientFromProfileRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.service.PatientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Patient card from user profile")
@RestController
@RequestMapping("/api/patient/clinics/{clinicId}/patient-card")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("""
        hasRole('PATIENT')
        and @clinicSecurity.hasAccess(authentication, #clinicId)
        """)
public class PatientPatientController {
   private final PatientService patientService;

   public PatientPatientController(PatientService patientService) {
      this.patientService = patientService;
   }

   @Operation(summary = "Create a patient card from profile", description = "Create a patient card from user's profile")
   @PostMapping
   public PatientResponseDto createPatientFromProfile(@PathVariable UUID clinicId, @AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody CreatePatientFromProfileRequestDto request) {
      return patientService.createFromProfile(clinicId, userDetails.getId(), request);
   }

   @Operation(summary = "Update a patient card from profile", description = "Update provided fields in patient card from profile")
   @PatchMapping
   public PatientResponseDto updatePatient(@PathVariable UUID clinicId, @AuthenticationPrincipal CustomUserDetails userDetails,
                                           @Valid @RequestBody UpdatePatientRequestDto request) {
      return patientService.updateByUserId(clinicId, userDetails.getId(), request);
   }

   @Operation(summary = "Get user's patient card", description = "Returns authenticated user's patient card")
   @GetMapping
   public PatientResponseDto getPatientByUserId(@PathVariable UUID clinicId, @AuthenticationPrincipal CustomUserDetails userDetails) {
      return patientService.getByUserId(clinicId, userDetails.getId());
   }
}

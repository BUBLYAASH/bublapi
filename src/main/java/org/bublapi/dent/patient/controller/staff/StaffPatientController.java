package org.bublapi.dent.patient.controller.staff;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientByPhoneRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.service.PatientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Patients management for staff")
@RestController
@RequestMapping("/api/patients")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiKey")
@PreAuthorize("""
        hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST')
        and @clinicSecurity.hasAccess(authentication)
        """)
public class StaffPatientController {
   private final PatientService patientService;

   public StaffPatientController(PatientService patientService) {
      this.patientService = patientService;
   }

   @Operation(summary = "Create a patient card from reception", description = "Create a patient card from reception")
   @PostMapping
   public PatientResponseDto createPatient(@Valid @RequestBody CreatePatientRequestDto request) {
      return patientService.create(request);
   }

   @Operation(summary = "Update a patient card by receptionist", description = "Update provided fields in patient card by receptionist")
   @PatchMapping("/{patientId}")
   public PatientResponseDto updatePatient(@PathVariable UUID patientId, @Valid @RequestBody UpdatePatientRequestDto request) {
      return patientService.update(patientId, request);
   }

   @Operation(summary = "Get user's patient card by user ID", description = "Get user's patient card by user ID")
   @GetMapping("/by-user/{userId}")
   public PatientResponseDto getPatientByUserId(@PathVariable UUID userId) {
      return patientService.getByUserId(userId);
   }

   @Operation(summary = "Get user's patient card by phone", description = "Get user's patient card by their phone")
   @PostMapping("/search/by-phone")
   public PatientResponseDto getPatientByPhone(@Valid @RequestBody PatientByPhoneRequestDto request) {
      return patientService.getByPhone(request);
   }

   @Operation(summary = "Get all patients in clinic", description = "Get all patients in this clinic")
   @GetMapping
   public List<PatientResponseDto> findAll() {
      return patientService.findAll();
   }
}

package org.bublapi.dent.patient.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.patient.dto.CreatePatientFromProfileRequestDto;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Patients")
@RestController
@RequestMapping("/api/clinics/{clinicId}/patients")
public class PatientController {

   private final PatientService patientService;

   public PatientController(PatientService patientService) {
      this.patientService = patientService;
   }

   @Operation(summary = "Create a patient card from reception", description = "Create a patient card from reception")
   @PostMapping
   public PatientResponseDto createPatient(@PathVariable UUID clinicId,
                                           @Valid @RequestBody CreatePatientRequestDto request) {
      return patientService.create(clinicId, request);
   }

   @Operation(summary = "Create a patient card from profile", description = "Create a patient card from user's profile")
   @PostMapping("/{userId}/patient-card") //TODO: Instead of userId -> get ID from JWT, not from URL
   public PatientResponseDto createPatientFromProfile(@PathVariable UUID clinicId, @PathVariable UUID userId, @Valid @RequestBody CreatePatientFromProfileRequestDto request) {
      return patientService.createFromProfile(clinicId, userId, request);
   }

   @Operation(summary = "Update a patient card", description = "Update provided fields in patient card")
   @PatchMapping("/{patientId}")
   public PatientResponseDto updatePatient(@PathVariable UUID clinicId, @PathVariable UUID patientId,
                                           @Valid @RequestBody UpdatePatientRequestDto request) {
      return patientService.update(clinicId, patientId, request);
   }

   @Operation(summary = "Get user's patient card", description = "Get user's patient card by ID")
   @GetMapping("/{userId}")
   public PatientResponseDto getPatientByUserId(@PathVariable UUID clinicId, @PathVariable UUID userId) {
      return patientService.getByUserId(clinicId, userId);
   }

   @Operation(summary = "Get all patients in clinic", description = "Get all patients in this clinic")
   @GetMapping
   public List<PatientResponseDto> findAll(@PathVariable UUID clinicId) {
      return patientService.findAll(clinicId);
   }
}

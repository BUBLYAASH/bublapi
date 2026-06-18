package org.bublapi.dent.patient.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.service.PatientService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patients")
@RestController
@RequestMapping("/api/clinics/{clinicId}/patients")
public class PatientController {

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @PostMapping
  @Operation(summary = "Create a patient card", description = "Create a patient card by provided data")
  public PatientResponseDto createPatient(@PathVariable UUID clinicId,
      @Valid @RequestBody CreatePatientRequestDto request) {
    return patientService.create(clinicId, request);
  }

  @PatchMapping("/{patientId}")
  @Operation(summary = "Update a patient card", description = "Update provided fields in patient card")
  public PatientResponseDto updatePatient(@PathVariable UUID clinicId, @PathVariable UUID patientId,
      @Valid @RequestBody UpdatePatientRequestDto request) {
    return patientService.update(clinicId, patientId, request);
  }
}

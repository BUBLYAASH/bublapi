package org.bublapi.dent.clinic.controller;

import io.swagger.v3.oas.annotations.OpenAPI31;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.service.ClinicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Clinics")
@RestController
@RequestMapping("/api/clinics")
public class ClinicController {

  private final ClinicService clinicService;

  public ClinicController(ClinicService clinicService) {
    this.clinicService = clinicService;
  }

  @Operation(summary = "Create a new clinic", description = "Add a new clinic to DB")
  @PostMapping
  public ClinicResponseDto createClinic(@Valid @RequestBody CreateClinicRequestDto request) {
    return clinicService.create(request);
  }

  @Operation(summary = "Update a clinic", description = "Update only provided fields in clinic")
  @PatchMapping("/{id}")
  public ClinicResponseDto updateClinic(@PathVariable UUID id,
      @Valid @RequestBody UpdateClinicRequestDto request) {
    return clinicService.update(id, request);
  }

  @Operation(summary = "Deactivate a clinic", description = "Deactivates clinic by ID")
  @PatchMapping("/{id}/deactivation")
  public ClinicResponseDto deactivateClinic(@PathVariable UUID id) {
    return clinicService.deactivate(id);
  }

  @Operation(summary = "Activate a clinic", description = "Activates clinic by ID")
  @PatchMapping("/{id}/activation")
  public ClinicResponseDto activateClinic(@PathVariable UUID id) {
    return clinicService.activate(id);
  }

  @Operation(summary = "Get all available clinics", description = "Get a list of all available clinics")
  @GetMapping
  public List<ClinicResponseDto> findAll() {
    return clinicService.findAll();
  }
}

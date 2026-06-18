package org.bublapi.dent.clinic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.service.ClinicService;
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
}

package org.bublapi.dent.clinic.controller;

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

@RestController
@RequestMapping("/api/clinics")
public class ClinicController {
  private final ClinicService clinicService;

  public ClinicController(ClinicService clinicService) {
    this.clinicService = clinicService;
  }

  @PostMapping
  public ClinicResponseDto create(@Valid @RequestBody CreateClinicRequestDto request){
    return clinicService.create(request);
  }

  @PatchMapping("/{id}")
  public ClinicResponseDto update(@PathVariable UUID id, @Valid @RequestBody UpdateClinicRequestDto request){
    return clinicService.update(id, request);
  }
}

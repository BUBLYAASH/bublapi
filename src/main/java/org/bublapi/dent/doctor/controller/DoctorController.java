package org.bublapi.dent.doctor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.service.DoctorService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Doctors")
@RestController
@RequestMapping("/api/clinics/{clinicId}/doctors")
public class DoctorController {

  private final DoctorService doctorService;

  public DoctorController(DoctorService doctorService) {
    this.doctorService = doctorService;
  }

  @PostMapping
  @Operation(summary = "Add a new doctor", description = "Add a new doctor to provided clinic")
  public DoctorResponseDto createDoctor(@PathVariable UUID clinicId,
      @Valid @RequestBody CreateDoctorRequestDto request) {
    return doctorService.create(clinicId, request);
  }

  @PatchMapping("/{doctorId}")
  @Operation(summary = "Update doctor's profile", description = "Update provided fields in doctor's profile")
  public DoctorResponseDto updateDoctor(@PathVariable UUID clinicId, @PathVariable UUID doctorId,
      @Valid @RequestBody UpdateDoctorRequestDto request) {
    return doctorService.update(clinicId, doctorId, request);
  }
}

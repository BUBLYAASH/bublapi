package org.bublapi.dent.doctor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.service.DoctorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Doctors")
@RestController
@RequestMapping("/api/clinics/{clinicId}/doctors")
public class DoctorController {

   private final DoctorService doctorService;

   public DoctorController(DoctorService doctorService) {
      this.doctorService = doctorService;
   }

   @Operation(summary = "Add a new doctor", description = "Add a new doctor to provided clinic")
   @PostMapping
   public DoctorResponseDto createDoctor(@PathVariable UUID clinicId,
                                         @Valid @RequestBody CreateDoctorRequestDto request) {
      return doctorService.create(clinicId, request);
   }

   @Operation(summary = "Update doctor's profile", description = "Update provided fields in doctor's profile")
   @PatchMapping("/{doctorId}")
   public DoctorResponseDto updateDoctor(@PathVariable UUID clinicId, @PathVariable UUID doctorId,
                                         @Valid @RequestBody UpdateDoctorRequestDto request) {
      return doctorService.update(clinicId, doctorId, request);
   }

   @Operation(summary = "Deactivate doctor", description = "Deactivates doctor by ID")
   @PatchMapping("/{doctorId}/deactivation")
   public DoctorResponseDto deactivateDoctor(@PathVariable UUID clinicId,
                                             @PathVariable UUID doctorId) {
      return doctorService.deactivate(clinicId, doctorId);
   }

   @Operation(summary = "Activate doctor", description = "Activates doctor by ID")
   @PatchMapping("/{doctorId}/activation")
   public DoctorResponseDto activateDoctor(@PathVariable UUID clinicId,
                                           @PathVariable UUID doctorId) {
      return doctorService.activate(clinicId, doctorId);
   }

   @Operation(summary = "Get all doctors in clinic", description = "Show all doctors in this clinic")
   @GetMapping
   public List<DoctorResponseDto> findAll(@PathVariable UUID clinicId) {
      return doctorService.findAll(clinicId);
   }
}

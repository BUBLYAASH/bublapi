package org.bublapi.dent.appointment.controller.patient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.service.AppointmentService;
import org.bublapi.dent.auth.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Appointments for patients")
@RestController
@RequestMapping("/api/patient/clinics/{clinicId}/appointments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATIENT')")
//TODO: after adding API Key for clinic, change endpoint by removing clinicId variable
public class PatientAppointmentController {
   private final AppointmentService appointmentService;

   public PatientAppointmentController(AppointmentService appointmentService) {
      this.appointmentService = appointmentService;
   }

   @Operation(summary = "Create new appointment", description = "Creates new appointment by patient card for the current authenticated patient")
   @PostMapping
   public AppointmentResponseDto create(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID clinicId, @Valid @RequestBody CreateAppointmentRequestDto request) {
      return appointmentService.createForPatient(userDetails.getId(), clinicId, request);
   }

   @Operation(summary = "Cancel an appointment", description = "Cancels an appointment")
   @PatchMapping("/{appointmentId}/cancel")
   public AppointmentResponseDto cancel(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID clinicId, @PathVariable UUID appointmentId) {
      return appointmentService.cancelByPatient(userDetails.getId(), clinicId, appointmentId);
   }
}

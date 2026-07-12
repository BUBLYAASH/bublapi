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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Appointments for patients")
@RestController
@RequestMapping("/api/patient/appointments")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiKey")
@PreAuthorize("""
        hasRole('PATIENT')
        and @clinicSecurity.hasAccess(authentication)
        """)
public class PatientAppointmentController {
   private final AppointmentService appointmentService;

   public PatientAppointmentController(AppointmentService appointmentService) {
      this.appointmentService = appointmentService;
   }

   @Operation(summary = "Create new appointment", description = "Creates new appointment by patient card for the current authenticated patient")
   @PostMapping
   public AppointmentResponseDto create(@AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody CreateAppointmentRequestDto request) {
      return appointmentService.createForPatient(userDetails.getId(), request);
   }

   @Operation(summary = "Cancel an appointment", description = "Cancels an appointment")
   @PatchMapping("/{appointmentId}/cancel")
   public AppointmentResponseDto cancel(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID appointmentId) {
      return appointmentService.cancelByPatient(userDetails.getId(), appointmentId);
   }

   @Operation(summary = "Get all appointments", description = "Shows all appointments for patient")
   @GetMapping
   public List<AppointmentResponseDto> findAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
      return appointmentService.findAllForPatient(userDetails.getId());
   }

   @Operation(summary = "Get information about one appointment", description = "Shows information about one appointment by ID")
   @GetMapping("/{appointmentId}")
   public AppointmentResponseDto findById(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID appointmentId) {
      return appointmentService.findByIdForPatient(userDetails.getId(), appointmentId);
   }
}

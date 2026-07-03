package org.bublapi.dent.appointment.controller.staff;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
import org.bublapi.dent.appointment.dto.ChangeAppointmentStatusRequestDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.service.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Appointments for staff")
@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/appointments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("""
        hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST', 'DOCTOR')
        and @clinicSecurity.hasAccess(authentication, #clinicId)
        """)
//TODO: after adding API Key for clinic, change endpoint by removing clinicId variable
public class StaffAppointmentController {
   private final AppointmentService appointmentService;

   public StaffAppointmentController(AppointmentService appointmentService) {
      this.appointmentService = appointmentService;
   }

   @Operation(summary = "Create new appointment", description = "Creates new appointment by staff for specified patient")
   @PostMapping
   public AppointmentResponseDto create(@PathVariable UUID patientId, @PathVariable UUID clinicId, @Valid @RequestBody CreateAppointmentRequestDto request) {
      return appointmentService.createForStaff(patientId, clinicId, request);
   }

   @Operation(summary = "Cancel an appointment", description = "Cancels an appointment by staff for specified patient")
   @PatchMapping("/{appointmentId}/cancel")
   public AppointmentResponseDto cancel(@PathVariable UUID patientId, @PathVariable UUID clinicId, @PathVariable UUID appointmentId) {
      return appointmentService.cancelByStaff(patientId, clinicId, appointmentId);
   }

   @Operation(summary = "Change appointment's status", description = "Changes appointment's status by staff")
   @PatchMapping("/{appointmentId}/change")
   public AppointmentResponseDto changeStatus(@PathVariable UUID patientId, @PathVariable UUID clinicId, @PathVariable UUID appointmentId, @Valid @RequestBody ChangeAppointmentStatusRequestDto request) {
      return appointmentService.changeStatusByStaff(clinicId, patientId, appointmentId, request);
   }
}

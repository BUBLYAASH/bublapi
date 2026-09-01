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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Appointments for staff")
@RestController
@RequestMapping("/api/appointments")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiKey")
@PreAuthorize("""
        hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST', 'DOCTOR')
        and @clinicSecurity.hasAccess(authentication)
        """)
public class StaffAppointmentController {
   private final AppointmentService appointmentService;

   public StaffAppointmentController(AppointmentService appointmentService) {
      this.appointmentService = appointmentService;
   }

   @Operation(summary = "Create new appointment", description = "Creates new appointment by staff for specified patient")
   @PostMapping("/patients/{patientId}")
   public AppointmentResponseDto create(@PathVariable UUID patientId,
                                        @Valid @RequestBody CreateAppointmentRequestDto request) {
      return appointmentService.createForStaff(patientId, request);
   }

   @Operation(summary = "Cancel an appointment", description = "Cancels an appointment by staff for specified patient")
   @PatchMapping("/{appointmentId}/cancel")
   public AppointmentResponseDto cancel(@PathVariable UUID appointmentId) {
      return appointmentService.cancelByStaff(appointmentId);
   }

   @Operation(summary = "Change appointment's status", description = "Changes appointment's status by staff")
   @PatchMapping("/{appointmentId}/change")
   public AppointmentResponseDto changeStatus(@PathVariable UUID appointmentId,
                                              @Valid @RequestBody ChangeAppointmentStatusRequestDto request) {
      return appointmentService.changeStatusByStaff(appointmentId, request);
   }

   @Operation(summary = "Get all patient's appointments", description = "Shows all appointments for provided patient")
   @GetMapping("/patients/{patientId}")
   public List<AppointmentResponseDto> findAllByPatient(@PathVariable UUID patientId) {
      return appointmentService.findAllByPatientForStaff(patientId);
   }

   @Operation(summary = "Get all appointments in clinic", description = "Shows all appointments in a clinic")
   @GetMapping
   public List<AppointmentResponseDto> findAll() {
      return appointmentService.findAllForStaff();
   }

   @Operation(summary = "Get information about one appointment", description = "Shows information about one appointment by its ID")
   @GetMapping("/{appointmentId}")
   public AppointmentResponseDto findById(@PathVariable UUID appointmentId) {
      return appointmentService.findByIdForStaff(appointmentId);
   }

   @Operation(summary = "Get all appointments to a doctor", description = "Shows all appointments to a doctor")
   @GetMapping("/doctors/{doctorId}")
   public List<AppointmentResponseDto> findAllByDoctor(@PathVariable UUID doctorId) {
      return appointmentService.findAllForDoctor(doctorId);
   }
}

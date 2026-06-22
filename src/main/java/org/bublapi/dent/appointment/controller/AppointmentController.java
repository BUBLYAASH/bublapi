package org.bublapi.dent.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.service.AppointmentService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Appointments")
@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/appointments")
public class AppointmentController {
   private final AppointmentService appointmentService;

   public AppointmentController(AppointmentService appointmentService) {
      this.appointmentService = appointmentService;
   }

   @Operation(summary = "Create new appointment", description = "Creates new appointment by patient card")
   @PostMapping
   public AppointmentResponseDto create(@PathVariable UUID clinicId, @PathVariable UUID patientId, @Valid @RequestBody CreateAppointmentRequestDto request) {
      return appointmentService.create(clinicId, patientId, request);
   }
}

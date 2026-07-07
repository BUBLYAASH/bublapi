package org.bublapi.dent.doctor.controller.staff;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.LinkUserToDoctorRequestDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.service.DoctorService;
import org.bublapi.dent.doctor_schedule_exception.dto.DoctorScheduleExceptionResponseDto;
import org.bublapi.dent.doctor_schedule_exception.dto.SetDoctorScheduleExceptionRequestDto;
import org.bublapi.dent.doctor_schedule_exception.service.DoctorScheduleExceptionService;
import org.bublapi.dent.doctor_working_hours.dto.DoctorWorkingHoursResponseDto;
import org.bublapi.dent.doctor_working_hours.dto.SetDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.dto.UpdateDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.service.DoctorWorkingHoursService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Doctors management for staff")
@RestController
@RequestMapping("/api/doctors")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST')")
public class StaffDoctorController {

   private final DoctorService doctorService;
   private final DoctorWorkingHoursService doctorWorkingHoursService;
   private final DoctorScheduleExceptionService doctorScheduleExceptionService;

   public StaffDoctorController(DoctorService doctorService, DoctorWorkingHoursService doctorWorkingHoursService, DoctorScheduleExceptionService doctorScheduleExceptionService) {
      this.doctorService = doctorService;
      this.doctorWorkingHoursService = doctorWorkingHoursService;
      this.doctorScheduleExceptionService = doctorScheduleExceptionService;
   }

   @Operation(summary = "Add a new doctor", description = "Add a new doctor to provided clinic")
   @PostMapping
   public DoctorResponseDto createDoctor(@Valid @RequestBody CreateDoctorRequestDto request) {
      return doctorService.create(request);
   }

   @Operation(summary = "Update doctor's profile", description = "Update provided fields in doctor's profile")
   @PatchMapping("/{doctorId}")
   public DoctorResponseDto updateDoctor(@PathVariable UUID doctorId, @Valid @RequestBody UpdateDoctorRequestDto request) {
      return doctorService.update(doctorId, request);
   }

   @Operation(summary = "Deactivate doctor", description = "Deactivates doctor by ID")
   @PatchMapping("/{doctorId}/deactivation")
   public DoctorResponseDto deactivateDoctor(@PathVariable UUID doctorId) {
      return doctorService.deactivate(doctorId);
   }

   @Operation(summary = "Activate doctor", description = "Activates doctor by ID")
   @PatchMapping("/{doctorId}/activation")
   public DoctorResponseDto activateDoctor(@PathVariable UUID doctorId) {
      return doctorService.activate(doctorId);
   }

   @Operation(summary = "Get all doctors in clinic", description = "Shows all doctors in current clinic")
   @GetMapping
   public List<DoctorResponseDto> findAll() {
      return doctorService.findAllForStaff();
   }

   @Operation(summary = "Set doctor's working hours", description = "Set working hours for a doctor on a specific day of week")
   @PostMapping("/{doctorId}/working-hours")
   public DoctorWorkingHoursResponseDto setSchedule(@PathVariable UUID doctorId, @Valid @RequestBody SetDoctorWorkingHoursRequestDto request) {
      return doctorWorkingHoursService.setSchedule(doctorId, request);
   }

   @Operation(summary = "Update doctor's working hours", description = "Update working hours for a doctor on a specific day of week")
   @PatchMapping("/{doctorId}/working-hours/{scheduleId}")
   public DoctorWorkingHoursResponseDto updateWorkingHours(@PathVariable UUID doctorId, @PathVariable UUID scheduleId, @Valid @RequestBody UpdateDoctorWorkingHoursRequestDto request) {
      return doctorWorkingHoursService.updateSchedule(doctorId, scheduleId, request);
   }

   @Operation(summary = "Delete doctor's working hours", description = "Delete a specific day of week from working hours for a doctor")
   @ResponseStatus(HttpStatus.NO_CONTENT)
   @DeleteMapping("/{doctorId}/working-hours/{scheduleId}")
   public void deleteWorkingHours(@PathVariable UUID doctorId, @PathVariable UUID scheduleId) {
      doctorWorkingHoursService.deleteSchedule(doctorId, scheduleId);
   }

   @Operation(summary = "Set doctor's schedule exception", description = "Set schedule exception for doctor")
   @PostMapping("/{doctorId}/schedule-exceptions")
   public DoctorScheduleExceptionResponseDto setScheduleException(@PathVariable UUID doctorId, @Valid @RequestBody SetDoctorScheduleExceptionRequestDto request) {
      return doctorScheduleExceptionService.setException(doctorId, request);
   }

   @Operation(summary = "Delete doctor's schedule exception", description = "Delete schedule exception for doctor")
   @ResponseStatus(HttpStatus.NO_CONTENT)
   @DeleteMapping("/{doctorId}/schedule-exceptions/{scheduleExceptionId}")
   public void deleteScheduleException(@PathVariable UUID doctorId, @PathVariable UUID scheduleExceptionId) {
      doctorScheduleExceptionService.deleteException(doctorId, scheduleExceptionId);
   }

   @Operation(summary = "Link user to doctor", description = "Connect user profile with doctor profile")
   @PatchMapping("/{doctorId}/user-link")
   public DoctorResponseDto linkUserToDoctor(@PathVariable UUID doctorId, @Valid @RequestBody LinkUserToDoctorRequestDto request) {
      return doctorService.linkUser(doctorId, request);
   }
}

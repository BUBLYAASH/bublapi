package org.bublapi.dent.doctor.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.doctor.dto.DoctorAvailabilityResponseDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.service.DoctorAvailabilityService;
import org.bublapi.dent.doctor.service.DoctorService;
import org.bublapi.dent.doctor_working_hours.dto.DoctorWorkingHoursResponseDto;
import org.bublapi.dent.doctor_working_hours.service.DoctorWorkingHoursService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public doctors' information")
@RestController
@RequestMapping("/api/public/doctors")
@SecurityRequirement(name = "apiKey")
public class PublicDoctorController {

   private final DoctorService doctorService;
   private final DoctorWorkingHoursService doctorWorkingHoursService;
   private final DoctorAvailabilityService doctorAvailabilityService;

   public PublicDoctorController(DoctorService doctorService, DoctorWorkingHoursService doctorWorkingHoursService, DoctorAvailabilityService doctorAvailabilityService) {
      this.doctorService = doctorService;
      this.doctorWorkingHoursService = doctorWorkingHoursService;
      this.doctorAvailabilityService = doctorAvailabilityService;
   }

   @Operation(summary = "Get all available doctors in clinic", description = "Shows all available doctors in this clinic")
   @GetMapping
   public List<DoctorResponseDto> findAll() {
      return doctorService.findAllActiveForPublic();
   }

   @Operation(summary = "Get information about one doctor", description = "Shows information about one doctor by ID")
   @GetMapping("/{doctorId}")
   public DoctorResponseDto findById(@PathVariable UUID doctorId) {
      return doctorService.findActiveById(doctorId);
   }

   @Operation(summary = "Get doctor's working hours", description = "Get actual doctor's working hours")
   @GetMapping("/{doctorId}/working-hours")
   public List<DoctorWorkingHoursResponseDto> getSchedule(@PathVariable UUID doctorId) {
      return doctorWorkingHoursService.getSchedule(doctorId);
   }

   @Operation
   @GetMapping("/{doctorId}/availability")
   public List<DoctorAvailabilityResponseDto> getAvailability(@PathVariable UUID doctorId, @RequestParam(defaultValue = "30") int durationMinutes, @RequestParam(defaultValue = "60") int days) {
      return doctorAvailabilityService.getAvailability(doctorId, durationMinutes, days);
   }
}

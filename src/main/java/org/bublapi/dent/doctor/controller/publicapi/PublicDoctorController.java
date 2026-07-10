package org.bublapi.dent.doctor.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.service.DoctorService;
import org.bublapi.dent.doctor_working_hours.dto.DoctorWorkingHoursResponseDto;
import org.bublapi.dent.doctor_working_hours.service.DoctorWorkingHoursService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

   public PublicDoctorController(DoctorService doctorService, DoctorWorkingHoursService doctorWorkingHoursService) {
      this.doctorService = doctorService;
      this.doctorWorkingHoursService = doctorWorkingHoursService;
   }

   @Operation(summary = "Get all available doctors in clinic", description = "Shows all available doctors in this clinic")
   @GetMapping
   public List<DoctorResponseDto> findAll() {
      return doctorService.findAllActiveForPublic();
   }

   @Operation(summary = "Get doctor's working hours", description = "Get actual doctor's working hours")
   @GetMapping("/{doctorId}/working-hours")
   public List<DoctorWorkingHoursResponseDto> getSchedule(@PathVariable UUID doctorId) {
      return doctorWorkingHoursService.getSchedule(doctorId);
   }
}

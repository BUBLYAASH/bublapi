package org.bublapi.dent.doctor_working_hours.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateDoctorWorkingHoursRequestDto(
        @NotNull LocalTime startTime,

        @NotNull LocalTime endTime) {
}

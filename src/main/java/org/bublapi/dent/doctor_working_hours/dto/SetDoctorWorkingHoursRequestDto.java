package org.bublapi.dent.doctor_working_hours.dto;

import jakarta.validation.constraints.NotNull;
import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;

import java.time.LocalTime;

public record SetDoctorWorkingHoursRequestDto(

        @NotNull DayOfWeek dayOfWeek,

        @NotNull LocalTime startTime,

        @NotNull LocalTime endTime) {
}

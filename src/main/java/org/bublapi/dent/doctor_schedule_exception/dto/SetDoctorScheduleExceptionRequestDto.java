package org.bublapi.dent.doctor_schedule_exception.dto;

import jakarta.validation.constraints.NotNull;
import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;

import java.time.LocalDate;
import java.time.LocalTime;

public record SetDoctorScheduleExceptionRequestDto(@NotNull LocalDate date,

                                                   @NotNull ScheduleExceptionType type,

                                                   LocalTime startTime,

                                                   LocalTime endTime,

                                                   String reason) {
}

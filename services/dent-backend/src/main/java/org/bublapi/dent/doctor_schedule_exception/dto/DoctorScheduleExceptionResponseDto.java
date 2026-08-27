package org.bublapi.dent.doctor_schedule_exception.dto;

import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DoctorScheduleExceptionResponseDto(
        UUID id, UUID doctorId, LocalDate date, ScheduleExceptionType type,
        LocalTime startTime, LocalTime endTime, String reason) {
}
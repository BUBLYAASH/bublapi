package org.bublapi.dent.doctor_working_hours.dto;

import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;

import java.time.LocalTime;
import java.util.UUID;

public record DoctorWorkingHoursResponseDto(UUID id, UUID doctorId, DayOfWeek dayOfWeek, LocalTime startTime,
                                            LocalTime endTime) {
}

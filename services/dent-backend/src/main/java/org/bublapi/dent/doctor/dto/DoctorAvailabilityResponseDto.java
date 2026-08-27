package org.bublapi.dent.doctor.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DoctorAvailabilityResponseDto(
        LocalDate date, List<LocalTime> slots) {
}

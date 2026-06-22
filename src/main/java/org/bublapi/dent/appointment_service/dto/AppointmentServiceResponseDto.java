package org.bublapi.dent.appointment_service.dto;

import java.util.UUID;

public record AppointmentServiceResponseDto(
        UUID clinicServiceId, String title, Integer price, Integer durationMinutes, Integer quantity,
        Integer position) {
}

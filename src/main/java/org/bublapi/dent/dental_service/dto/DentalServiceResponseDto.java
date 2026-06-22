package org.bublapi.dent.dental_service.dto;

import java.util.UUID;

public record DentalServiceResponseDto(
        UUID id, String title, String description, String category,
        Integer defaultDurationMinutes) {
}

package org.bublapi.dent.clinic_service.dto;

import java.util.UUID;

public record ClinicServiceResponseDto(
        UUID id, UUID clinicId, UUID dentalServiceId, Integer price, Integer durationMinutes) {
}

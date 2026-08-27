package org.bublapi.dent.clinic_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateClinicServiceRequestDto(
        @Min(0) Integer price,
        @Min(5) @Max(480) Integer durationMinutes
) {
}

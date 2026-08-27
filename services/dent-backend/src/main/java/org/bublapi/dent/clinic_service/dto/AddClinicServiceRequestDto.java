package org.bublapi.dent.clinic_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddClinicServiceRequestDto(
        @NotNull @Min(0) Integer price,

        @NotNull @Min(5) @Max(480) Integer durationMinutes) {
}

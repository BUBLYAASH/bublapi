package org.bublapi.dent.dental_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bublapi.dent.dental_service.entity.ServiceCategory;

public record CreateDentalServiceRequestDto(
        @NotBlank String title,

        String description,

        @NotNull ServiceCategory category,

        @NotNull @Min(5) @Max(480) Integer defaultDurationMinutes) {
}

package org.bublapi.dent.dental_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.bublapi.dent.dental_service.entity.ServiceCategory;

public record UpdateDentalServiceRequestDto(
        String title,

        String description,

        ServiceCategory category,

        @Min(5) @Max(480) Integer defaultDurationMinutes) {
}

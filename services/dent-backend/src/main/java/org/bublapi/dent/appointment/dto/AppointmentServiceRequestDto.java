package org.bublapi.dent.appointment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AppointmentServiceRequestDto(
        @NotNull UUID clinicServiceId,

        @NotNull @Min(1) Integer quantity) {
}

package org.bublapi.dent.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAppointmentRequestDto(
        @NotNull UUID doctorId,

        @NotNull LocalDateTime scheduledAt,

        @NotEmpty List<@Valid AppointmentServiceRequestDto> services,

        String comment) {
}

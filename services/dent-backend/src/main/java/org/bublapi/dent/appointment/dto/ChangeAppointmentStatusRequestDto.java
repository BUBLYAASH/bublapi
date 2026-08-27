package org.bublapi.dent.appointment.dto;

import jakarta.validation.constraints.NotNull;
import org.bublapi.dent.appointment.entity.AppointmentStatus;

public record ChangeAppointmentStatusRequestDto(
        @NotNull AppointmentStatus status
) {
}

package org.bublapi.dent.appointment.dto;

import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment_service.dto.AppointmentServiceResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id, UUID clinicId, UUID patientId, UUID doctorId, String doctorFirstName, String doctorLastName,
        String doctorMiddleName, LocalDateTime scheduledAt,
        LocalDateTime endAt,
        List<AppointmentServiceResponseDto> services, String comment, Integer totalPrice, AppointmentStatus status) {
}

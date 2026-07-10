package org.bublapi.dent.user.dto;

public record CreateUserResponseDto(
        UserResponseDto user,
        PatientCardLinkStatus patientCardLinkStatus,
        String patientCardMessage
) {
}

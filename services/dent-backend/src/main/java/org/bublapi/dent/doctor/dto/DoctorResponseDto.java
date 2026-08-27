package org.bublapi.dent.doctor.dto;

import java.util.UUID;

public record DoctorResponseDto(
        UUID id, UUID clinicId, UUID userId, String firstName,
        String lastName, String middleName, String specialty,
        String avatarUrl, String description, boolean active) {

}

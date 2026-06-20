package org.bublapi.dent.user.dto;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        String middleName,
        UUID clinicId
) {

}

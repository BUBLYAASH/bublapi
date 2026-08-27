package org.bublapi.dent.user.dto;

import org.bublapi.dent.role.entity.RoleName;

import java.util.Set;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        String middleName,
        UUID clinicId,
        Set<RoleName> roles,
        Boolean enabled
) {

}

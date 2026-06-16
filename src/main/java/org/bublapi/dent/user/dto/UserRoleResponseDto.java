package org.bublapi.dent.user.dto;

import java.util.UUID;

public record UserRoleResponseDto(
    UUID userId,
    UUID roleId
) { }

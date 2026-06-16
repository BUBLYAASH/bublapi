package org.bublapi.dent.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UserRoleResponseDto(
    UUID userId,
    UUID roleId
) { }

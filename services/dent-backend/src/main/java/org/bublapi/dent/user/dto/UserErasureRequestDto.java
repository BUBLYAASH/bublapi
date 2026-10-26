package org.bublapi.dent.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserErasureRequestDto(
        @NotNull UUID confirmationUserId, @NotNull UUID clinicId, @AssertTrue boolean retentionReviewed) {
}

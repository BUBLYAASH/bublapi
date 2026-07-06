package org.bublapi.dent.apikey.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequestDto(
        @NotBlank String name) {
}

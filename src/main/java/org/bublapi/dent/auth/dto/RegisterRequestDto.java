package org.bublapi.dent.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequestDto(
        @NotBlank String firstName,

        @NotBlank String lastName,

        String middleName,

        @NotBlank @Email String email,

        @NotBlank String phone,

        @NotBlank String password) {
}

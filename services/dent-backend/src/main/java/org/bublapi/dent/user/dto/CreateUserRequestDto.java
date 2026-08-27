package org.bublapi.dent.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDto(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "^\\d{10,15}$")
        @Size(max = 15)
        String phone,

        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @Size(max = 50)
        String middleName,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {

}

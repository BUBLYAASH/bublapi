package org.bublapi.dent.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record LinkUserToDoctorRequestDto(
        @Email
        String email,

        @Pattern(regexp = "^\\d{10,15}$")
        String phone
) {
}

package org.bublapi.dent.user.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;
import org.bublapi.dent.clinic.entity.Clinic;

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
    String password,

    @NotNull
    UUID clinicId
) {

}

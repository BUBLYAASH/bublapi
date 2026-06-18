package org.bublapi.dent.doctor.dto;

import jakarta.validation.constraints.*;

public record CreateDoctorRequestDto(@NotBlank @Size(max = 50) String firstName,

                                     @NotBlank @Size(max = 50) String lastName,

                                     @Size(max = 50) String middleName,

                                     @NotBlank String specialty,

                                     String avatarUrl,

                                     String description) {

}

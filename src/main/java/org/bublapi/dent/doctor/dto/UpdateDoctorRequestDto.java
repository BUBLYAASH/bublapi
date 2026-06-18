package org.bublapi.dent.doctor.dto;

import jakarta.validation.constraints.Size;

public record UpdateDoctorRequestDto(@Size(max = 50) String firstName,

                                     @Size(max = 50) String lastName,

                                     @Size(max = 50) String middleName,

                                     String specialty,

                                     String avatarUrl,

                                     String description) {

}

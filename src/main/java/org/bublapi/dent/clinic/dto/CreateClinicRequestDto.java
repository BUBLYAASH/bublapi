package org.bublapi.dent.clinic.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClinicRequestDto(
    @NotBlank
    @Size(max = 100)
    String title,

    @Size(max = 255)
    String description,

    @NotBlank
    @Size(max = 255)
    String address,

    @Pattern(regexp = "^\\d{10,15}$")
    @Size(max = 15)
    String phone,

    @Email
    @Size(max = 255)
    String email,

    @Size(max = 255)
    String website,

    @NotBlank
    @Size(max = 50)
    String timezone
) {

}

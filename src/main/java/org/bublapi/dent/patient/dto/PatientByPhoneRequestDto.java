package org.bublapi.dent.patient.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientByPhoneRequestDto(
        @Pattern(regexp = "^\\d{10,15}$") @Size(max = 15) String phone) {
}

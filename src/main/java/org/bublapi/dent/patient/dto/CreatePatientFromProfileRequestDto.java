package org.bublapi.dent.patient.dto;

import java.time.LocalDate;

public record CreatePatientFromProfileRequestDto(
        LocalDate birthDate,

        String notes,

        String allergies,

        String chronicDiseases) {
}

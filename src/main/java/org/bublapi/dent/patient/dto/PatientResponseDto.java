package org.bublapi.dent.patient.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponseDto(
        UUID id, UUID clinicId, UUID userId, String firstName,
        String lastName, String middleName, String phone, String email,
        LocalDate birthDate, String notes, String allergies,
        String chronicDiseases) {

}

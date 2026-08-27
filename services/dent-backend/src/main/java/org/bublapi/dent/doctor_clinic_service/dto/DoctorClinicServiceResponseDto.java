package org.bublapi.dent.doctor_clinic_service.dto;

import java.util.UUID;

public record DoctorClinicServiceResponseDto(
        UUID doctorId, UUID clinicServiceId, UUID dentalServiceId, String title, Integer price,
        Integer durationMinutes) {
}

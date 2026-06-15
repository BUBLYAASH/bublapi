package org.bublapi.dent.clinic.dto;

import java.util.UUID;

public record ClinicResponseDto(
    UUID id,
    String title,
    String description,
    String address,
    String phone,
    String email,
    String website,
    String timezone
) { }

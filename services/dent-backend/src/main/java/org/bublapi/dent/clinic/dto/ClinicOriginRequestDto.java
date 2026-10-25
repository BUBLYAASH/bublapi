package org.bublapi.dent.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicOriginRequestDto(@NotBlank @Size(max = 255) String origin) {}

package org.bublapi.dent.apikey.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApiKeyResponseDto(
        UUID id, UUID clinicId, String name, String prefix, LocalDateTime expiresAt, LocalDateTime graceUntil,
        boolean active) {
}

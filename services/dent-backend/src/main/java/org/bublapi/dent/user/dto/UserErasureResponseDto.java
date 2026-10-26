package org.bublapi.dent.user.dto;

public record UserErasureResponseDto(int deletedAppointments, int anonymizedAppointments, String backupNotice) {
}

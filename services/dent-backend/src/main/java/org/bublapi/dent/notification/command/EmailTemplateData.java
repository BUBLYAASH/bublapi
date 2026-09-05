package org.bublapi.dent.notification.command;

import java.util.List;

public record EmailTemplateData(
        String clinicTitle, String firstName, String scheduledAt, String doctorName, List<String> serviceTitles) {
}

package org.bublapi.dent.notification.message;

public record EmailMessage(String recipient, String subject, String html) {
}

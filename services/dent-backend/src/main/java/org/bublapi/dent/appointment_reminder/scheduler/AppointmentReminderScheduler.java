package org.bublapi.dent.appointment_reminder.scheduler;

import lombok.RequiredArgsConstructor;
import org.bublapi.dent.appointment_reminder.service.AppointmentReminderService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class AppointmentReminderScheduler {

   private final AppointmentReminderService appointmentReminderService;

   @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
   public void sendReminders() {
      appointmentReminderService.processDueReminders();
   }
}

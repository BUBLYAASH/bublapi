package org.bublapi.dent.appointment_reminder.service;

import jakarta.persistence.EntityManager;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment_reminder.entity.ReminderStatus;
import org.bublapi.dent.appointment_reminder.repository.AppointmentReminderRepository;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.bublapi.dent.notification.command.AppointmentNotificationData;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.publisher.NotificationPublisher;
import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class AppointmentReminderService {
   private static final DateTimeFormatter APPOINTMENT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

   private final AppointmentReminderRepository appointmentReminderRepository;
   private final NotificationPublisher notificationPublisher;
   private final EntityManager entityManager;

   public AppointmentReminderService(NotificationPublisher notificationPublisher,
                                     AppointmentReminderRepository appointmentReminderRepository,
                                     EntityManager entityManager) {
      this.appointmentReminderRepository = appointmentReminderRepository;
      this.notificationPublisher = notificationPublisher;
      this.entityManager = entityManager;
   }

   @Transactional
   public void processDueReminders() {
      Session session = entityManager.unwrap(Session.class);
      boolean filterEnabled = session.getEnabledFilter("clinicFilter") != null;

      session.disableFilter("clinicFilter");

      try {
         processDueRemindersInternal();
      } finally {
         if (filterEnabled) {
            session.enableFilter("clinicFilter");
         }
      }
   }

   private void processDueRemindersInternal() {
      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

      var reminders = appointmentReminderRepository.findDueForUpdate(ReminderStatus.PENDING, now,
                                                                     PageRequest.of(0, 100));

      for (var reminder : reminders) {
         Appointment appointment = reminder.getAppointment();

         if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED || !appointment.getScheduledAt()
                                                                                                                                             .isAfter(
                                                                                                                                                     now)) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            continue;
         }

         if (appointment.getPatient() == null || appointment.getPatient().getUser() == null) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            continue;
         }

         notificationPublisher.publishAfterCommit(
                 new CreateNotificationCommand(reminder.getId(), appointment.getClinic().getId(),
                                               appointment.getPatient().getUser().getId(), appointment.getId(),
                                               NotificationType.APPOINTMENT_REMINDER,
                                               new AppointmentNotificationData(appointment.getClinic().getTitle(),
                                                                               appointment.getPatient().getFirstName(),
                                                                               appointment.getScheduledAt()
                                                                                          .format(APPOINTMENT_FORMATTER),
                                                                               appointment.getDoctor()
                                                                                          .getFirstName() + " " + appointment.getDoctor()
                                                                                                                             .getLastName(),
                                                                               appointment.getServices()
                                                                                          .stream()
                                                                                          .map(AppointmentServiceItem::getTitle)
                                                                                          .toList()),
                                               LocalDateTime.now(ZoneOffset.UTC)));

         reminder.setStatus(ReminderStatus.PUBLISHED);
         reminder.setPublishedAt(now);
      }
   }
}

package org.bublapi.dent.integration.appointment;

import org.bublapi.dent.appointment_reminder.entity.AppointmentReminder;
import org.bublapi.dent.appointment_reminder.entity.ReminderStatus;
import org.bublapi.dent.appointment_reminder.repository.AppointmentReminderRepository;
import org.bublapi.dent.appointment_reminder.service.AppointmentReminderService;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentReminderIntegrationTest extends IntegrationTestSupport {

   @Autowired
   AppointmentReminderRepository reminders;

   @Autowired
   AppointmentReminderService reminderService;

   @Autowired
   JdbcTemplate jdbc;

   @Test
   void shouldCreateReminderWhenAppointmentCreated() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime at = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createStaffAppointment(context, at).andExpect(status().isOk()).andReturn());

      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointment_reminders", Long.class)).isEqualTo(1L);

      var reminder = reminderFor(appointmentId);

      assertThat(reminder.getAppointment().getId()).isEqualTo(appointmentId);
      assertThat(reminder.getReminderType()).isEqualTo("BEFORE_24_HOURS");
      assertThat(reminder.getDueAt()).isEqualTo(at.minusDays(1));
      assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
      assertThat(reminder.getPublishedAt()).isNull();
   }

   @Test
   void shouldCancelPendingReminderWhenAppointmentCancelled() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime at = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createStaffAppointment(context, at).andExpect(status().isOk()).andReturn());

      mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appointmentId).header("Authorization", jwtHelper.token(
              context.user().getId())).header("X-API-KEY", context.apiKey().rawKey())).andExpect(status().isOk());

      assertThat(reminderFor(appointmentId).getStatus()).isEqualTo(ReminderStatus.CANCELLED);
   }

   @Test
   void shouldPublishReminderWhenDueAtReached() throws Exception {
      var context = createPatientAppointmentContext();
      LocalDateTime at = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createPatientAppointment(context, at).andExpect(status().isOk()).andReturn());

      var reminder = reminderFor(appointmentId);

      jdbc.update("UPDATE appointment_reminders SET due_at = ? WHERE id = ?",
                  LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), reminder.getId());

      clearInvocations(notificationPublisher);

      reminderService.processDueReminders();

      var updated = reminderFor(appointmentId);

      assertThat(updated.getStatus()).isEqualTo(ReminderStatus.PUBLISHED);
      assertThat(updated.getPublishedAt()).isNotNull();

      ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);

      verify(notificationPublisher, times(1)).publishAfterCommit(captor.capture());

      var command = captor.getValue();

      assertThat(command.requestId()).isEqualTo(reminder.getId());
      assertThat(command.appointmentId()).isEqualTo(appointmentId);
      assertThat(command.clinicId()).isEqualTo(context.clinic().getId());
      assertThat(command.type()).isEqualTo(NotificationType.APPOINTMENT_REMINDER);
      assertThat(command.data()).isNotNull();
   }

   @Test
   void shouldNotPublishReminderBeforeDueAt() throws Exception {
      var context = createPatientAppointmentContext();
      LocalDateTime at = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createPatientAppointment(context, at).andExpect(status().isOk()).andReturn());

      clearInvocations(notificationPublisher);

      reminderService.processDueReminders();

      assertThat(reminderFor(appointmentId).getStatus()).isEqualTo(ReminderStatus.PENDING);

      verifyNoInteractions(notificationPublisher);
   }

   @Test
   void shouldNotPublishSameReminderTwice() throws Exception {
      var context = createPatientAppointmentContext();
      LocalDateTime at = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createPatientAppointment(context, at).andExpect(status().isOk()).andReturn());

      AppointmentReminder reminder = reminderFor(appointmentId);

      jdbc.update("UPDATE appointment_reminders SET due_at = ? WHERE id = ?",
                  Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)), reminder.getId());

      clearInvocations(notificationPublisher);

      reminderService.processDueReminders();
      reminderService.processDueReminders();

      assertThat(reminderFor(appointmentId).getStatus()).isEqualTo(ReminderStatus.PUBLISHED);

      verify(notificationPublisher, times(1)).publishAfterCommit(any(CreateNotificationCommand.class));
   }

   @Test
   void shouldCancelReminderForPastAppointment() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime at = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createStaffAppointment(context, at).andExpect(status().isOk()).andReturn());

      var reminder = reminderFor(appointmentId);

      LocalDateTime past = LocalDateTime.now(ZoneOffset.UTC).minusHours(2);

      jdbc.update("UPDATE appointments SET scheduled_at = ?, end_at = ? WHERE id = ?", past, past.plusMinutes(30),
                  appointmentId);

      jdbc.update("UPDATE appointment_reminders SET due_at = ? WHERE id = ?", past.minusDays(1), reminder.getId());

      clearInvocations(notificationPublisher);

      reminderService.processDueReminders();

      assertThat(reminderFor(appointmentId).getStatus()).isEqualTo(ReminderStatus.CANCELLED);

      verifyNoInteractions(notificationPublisher);
   }

   private AppointmentReminder reminderFor(UUID appointmentId) {
      List<AppointmentReminder> found = reminders.findAllByAppointment_Id(appointmentId);

      assertThat(found).hasSize(1);
      return found.get(0);
   }
}

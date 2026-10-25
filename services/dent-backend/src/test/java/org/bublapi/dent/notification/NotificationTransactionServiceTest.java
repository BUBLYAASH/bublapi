package org.bublapi.dent.notification;

import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.UserNotificationData;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.message.NotificationContent;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.bublapi.dent.notification.service.NotificationTransactionService;
import org.bublapi.dent.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationTransactionServiceTest {
   final NotificationRepository repository = mock(NotificationRepository.class);
   final ClinicRepository clinics = mock(ClinicRepository.class);
   final UserRepository users = mock(UserRepository.class);
   final NotificationTransactionService service = new NotificationTransactionService(repository,
                                                                                     mock(AppointmentRepository.class),
                                                                                     clinics, users);

   @Test
   void failedDeliveryReusesExistingRequestAndChannelInsteadOfCreatingDuplicate() {
      var command = new CreateNotificationCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                                                  NotificationType.USER_REGISTERED,
                                                  new UserNotificationData("Clinic", "Patient"),
                                                  LocalDateTime.now(ZoneOffset.UTC));
      var existing = new Notification();
      existing.setStatus(NotificationStatus.FAILED);
      existing.setErrorMessage("previous failure");
      when(repository.findByRequestIdAndChannel(command.requestId(), NotificationChannel.EMAIL)).thenReturn(
              Optional.of(existing));
      assertThat(service.prepare(command, NotificationChannel.EMAIL,
                                 new NotificationContent("Title", "Message"))).isSameAs(existing);
      assertThat(existing.getStatus()).isEqualTo(NotificationStatus.PENDING);
      assertThat(existing.getErrorMessage()).isNull();
      verify(repository, never()).save(any());
      verifyNoInteractions(clinics, users);
   }

   @Test
   void failureMessageIsBoundedToDatabaseColumnAndNullIsHandled() {
      var notification = new Notification();
      UUID id = UUID.randomUUID();
      when(repository.findById(id)).thenReturn(Optional.of(notification));
      service.markAsFailed(id, "x".repeat(1500));
      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
      assertThat(notification.getErrorMessage()).hasSize(1000);
      service.markAsFailed(id, null);
      assertThat(notification.getErrorMessage()).isEqualTo("Unknown notification error");
      service.markAsSent(id);
      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
      assertThat(notification.getSentAt()).isNotNull();
      assertThat(notification.getErrorMessage()).isNull();
   }
}

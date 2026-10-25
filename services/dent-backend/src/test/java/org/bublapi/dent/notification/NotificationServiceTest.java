package org.bublapi.dent.notification;

import org.bublapi.dent.logging.audit.AdministrativeAuditService;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.UserNotificationData;
import org.bublapi.dent.notification.dispatcher.NotificationDispatcher;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.mapper.NotificationMapper;
import org.bublapi.dent.notification.message.NotificationContent;
import org.bublapi.dent.notification.producer.NotificationProducer;
import org.bublapi.dent.notification.renderer.NotificationContentRenderer;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.bublapi.dent.notification.service.NotificationService;
import org.bublapi.dent.notification.service.NotificationTransactionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
   final NotificationTransactionService transactions = mock(NotificationTransactionService.class);
   final NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
   final NotificationContentRenderer renderer = mock(NotificationContentRenderer.class);
   final NotificationService service = new NotificationService(mock(NotificationRepository.class),
                                                               mock(NotificationMapper.class), transactions, dispatcher,
                                                               renderer, mock(NotificationProducer.class),
                                                               mock(AdministrativeAuditService.class));
   final CreateNotificationCommand command = new CreateNotificationCommand(UUID.randomUUID(), UUID.randomUUID(),
                                                                           UUID.randomUUID(), null,
                                                                           NotificationType.USER_REGISTERED,
                                                                           new UserNotificationData("Clinic",
                                                                                                    "Patient"),
                                                                           LocalDateTime.now(ZoneOffset.UTC));
   final NotificationContent content = new NotificationContent("Title", "Message");

   @Test
   void successfulChannelsAreNotRedeliveredOnRetry() {
      when(renderer.render(command.type())).thenReturn(content);
      var sent = notification(NotificationStatus.SENT);
      var pending = notification(NotificationStatus.PENDING);
      when(transactions.prepare(command, NotificationChannel.IN_APP, content)).thenReturn(sent);
      when(transactions.prepare(command, NotificationChannel.EMAIL, content)).thenReturn(pending);
      service.create(command);
      verify(dispatcher, never()).dispatch(sent, command);
      verify(dispatcher).dispatch(pending, command);
      verify(transactions).markAsSent(pending.getId());
      verify(transactions, never()).markAsFailed(any(), any());
   }

   @Test
   void deliveryFailureIsPersistedAndRethrownForQueueRetry() {
      when(renderer.render(command.type())).thenReturn(content);
      var pending = notification(NotificationStatus.PENDING);
      when(transactions.prepare(command, NotificationChannel.IN_APP, content)).thenReturn(pending);
      var failure = new IllegalStateException("delivery failed");
      doThrow(failure).when(dispatcher).dispatch(pending, command);
      assertThatThrownBy(() -> service.create(command)).isSameAs(failure);
      verify(transactions).markAsFailed(pending.getId(), "delivery failed");
      verify(transactions, never()).markAsSent(any());
      verify(transactions, never()).prepare(command, NotificationChannel.EMAIL, content);
   }

   private Notification notification(NotificationStatus status) {
      var notification = new Notification();
      notification.setId(UUID.randomUUID());
      notification.setStatus(status);
      return notification;
   }
}

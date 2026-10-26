package org.bublapi.dent.notification;

import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.UserNotificationData;
import org.bublapi.dent.notification.consumer.NotificationConsumer;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.producer.NotificationProducer;
import org.bublapi.dent.notification.service.NotificationQueueService;
import org.bublapi.dent.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationErasureTest {
   @Test
   void brokerReceivesNoPersonalPayload() {
      var rabbit = mock(RabbitTemplate.class);
      var queue = mock(NotificationQueueService.class);
      var command = command();
      when(queue.prepare(command)).thenReturn(true);
      new NotificationProducer(rabbit, queue).publish(command);
      var envelope = ArgumentCaptor.forClass(Object.class);
      verify(rabbit).convertAndSend(anyString(), anyString(), envelope.capture());
      var queued = (CreateNotificationCommand) envelope.getValue();
      assertThat(queued.data()).isNull();
      assertThat(queued.requestId()).isEqualTo(command.requestId());
   }

   @Test
   void deletedUserCannotReceiveOrRecreateQueuedNotification() {
      var service = mock(NotificationService.class);
      var clinics = mock(ClinicRepository.class);
      var queue = mock(NotificationQueueService.class);
      var command = command();
      new NotificationConsumer(service, clinics, queue).handle(command);
      verify(queue, never()).load(command);
      verifyNoInteractions(service, clinics);
   }

   @Test
   void publishingForDeletedUserDoesNotEnqueue() {
      var rabbit = mock(RabbitTemplate.class);
      new NotificationProducer(rabbit, mock(NotificationQueueService.class)).publish(command());
      verifyNoInteractions(rabbit);
   }

   private CreateNotificationCommand command() {
      return new CreateNotificationCommand(UUID.randomUUID(), UUID.randomUUID(), null, NotificationType.USER_REGISTERED,
                                           new UserNotificationData("Clinic", "Private name"), LocalDateTime.now());
   }
}

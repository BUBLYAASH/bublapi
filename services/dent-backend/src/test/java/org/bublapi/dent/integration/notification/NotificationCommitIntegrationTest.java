package org.bublapi.dent.integration.notification;

import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.UserNotificationData;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.producer.NotificationProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class NotificationCommitIntegrationTest extends IntegrationTestBase {
   @Autowired
   ApplicationEventPublisher events;
   @Autowired
   PlatformTransactionManager transactions;
   @MockitoBean
   NotificationProducer producer;

   @Test
   void publishesOnlyAfterSuccessfulCommit() {
      var command = command();
      new TransactionTemplate(transactions).executeWithoutResult(status -> {
         events.publishEvent(command);
         verifyNoInteractions(producer);
      });
      verify(producer).publish(command);
      verifyNoMoreInteractions(producer);
   }

   @Test
   void rollbackDoesNotPublish() {
      new TransactionTemplate(transactions).executeWithoutResult(status -> {
         events.publishEvent(command());
         status.setRollbackOnly();
      });
      verifyNoInteractions(producer);
   }

   @Test
   void eventOutsideTransactionDoesNotPublish() {
      events.publishEvent(command());
      verifyNoInteractions(producer);
   }

   private CreateNotificationCommand command() {
      return new CreateNotificationCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                                           NotificationType.USER_REGISTERED,
                                           new UserNotificationData("Clinic", "Patient"),
                                           LocalDateTime.now(ZoneOffset.UTC));
   }
}

package org.bublapi.dent.notification.event;

import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.producer.NotificationProducer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationAfterCommitListener {
   private final NotificationProducer notificationProducer;

   public NotificationAfterCommitListener(NotificationProducer notificationProducer) {
      this.notificationProducer = notificationProducer;
   }

   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void handleAfterCommit(CreateNotificationCommand command) {
      notificationProducer.publish(command);
   }
}

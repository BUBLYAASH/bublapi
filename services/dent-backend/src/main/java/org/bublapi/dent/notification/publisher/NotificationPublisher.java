package org.bublapi.dent.notification.publisher;

import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {
   public NotificationPublisher(ApplicationEventPublisher eventPublisher) {
      this.eventPublisher = eventPublisher;
   }

   public void publishAfterCommit(CreateNotificationCommand command) {
      eventPublisher.publishEvent(command);
   }

   private final ApplicationEventPublisher eventPublisher;
}

package org.bublapi.dent.notification.sender;

import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationSender {
   @Override
   public NotificationChannel channel() {
      return NotificationChannel.IN_APP;
   }

   @Override
   public void send(Notification notification, CreateNotificationCommand command) {
   }
}

package org.bublapi.dent.notification.sender;

import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;

public interface NotificationSender {
   NotificationChannel channel();

   void send(Notification notification, CreateNotificationCommand command);
}

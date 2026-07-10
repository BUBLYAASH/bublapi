package org.bublapi.dent.notification.dispatcher;

import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.sender.NotificationSender;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatcher {
   private final Map<NotificationChannel, NotificationSender> senders;

   public NotificationDispatcher(List<NotificationSender> senderList) {
      this.senders = new EnumMap<>(NotificationChannel.class);

      for (NotificationSender sender : senderList) {
         this.senders.put(sender.channel(), sender);
      }
   }

   public void dispatch(Notification notification, CreateNotificationCommand command) {
      NotificationSender sender = this.senders.get(command.channel());

      if (sender == null) {
         throw new IllegalArgumentException("Unsupported notification channel: " + command.channel());
      }

      sender.send(notification, command);
   }
}

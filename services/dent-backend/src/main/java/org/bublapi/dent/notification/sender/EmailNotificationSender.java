package org.bublapi.dent.notification.sender;

import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
   private final JavaMailSender mailSender;
   private final String fromEmail;
   private final String replyTo;

   public EmailNotificationSender(JavaMailSender mailSender, @Value("${spring.mail.from}") String fromEmail, @Value("${spring.mail.reply-to}") String replyTo) {
      this.mailSender = mailSender;
      this.fromEmail = fromEmail;
      this.replyTo = replyTo;
   }

   @Override
   public NotificationChannel channel() {
      return NotificationChannel.EMAIL;
   }

   @Override
   public void send(Notification notification, CreateNotificationCommand command) {
      if (command.recipientEmail() == null || command.recipientEmail().isBlank()) {
         throw new IllegalArgumentException("Recipient email is required");
      }

      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(this.fromEmail);
      message.setReplyTo(this.replyTo);
      message.setTo(command.recipientEmail());
      message.setSubject(command.title());
      message.setText(command.message());

      mailSender.send(message);
   }
}

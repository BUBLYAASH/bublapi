package org.bublapi.dent.notification.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.message.EmailMessage;
import org.bublapi.dent.notification.renderer.EmailNotificationRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;


@Component
public class EmailNotificationSender implements NotificationSender {
   private final JavaMailSender mailSender;
   private final EmailNotificationRenderer renderer;
   private final String fromEmail;
   private final String replyTo;

   public EmailNotificationSender(JavaMailSender mailSender, EmailNotificationRenderer renderer,
                                  @Value("${spring.mail.from}") String fromEmail,
                                  @Value("${spring.mail.reply-to}") String replyTo) {
      this.mailSender = mailSender;
      this.renderer = renderer;
      this.fromEmail = fromEmail;
      this.replyTo = replyTo;
   }

   @Override
   public NotificationChannel channel() {
      return NotificationChannel.EMAIL;
   }

   @Override
   public void send(Notification notification, CreateNotificationCommand command) {
      EmailMessage message = renderer.render(command.type(), command.data(), notification.getUser().getEmail());

      sendEmail(message);
   }

   private void sendEmail(EmailMessage message) {
      MimeMessage mimeMessage = mailSender.createMimeMessage();

      try {
         MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

         helper.setFrom(fromEmail, "BublAPI");
         helper.setReplyTo(replyTo);
         helper.setTo(message.recipient());
         helper.setSubject(message.subject());
         helper.setText(message.html(), true);

         mailSender.send(mimeMessage);
      } catch (MessagingException | UnsupportedEncodingException e) {
         throw new IllegalStateException("Failed to create email message", e);
      }
   }
}

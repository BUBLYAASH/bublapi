package org.bublapi.dent.notification.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.EmailTemplateData;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;


@Component
public class EmailNotificationSender implements NotificationSender {
   private final JavaMailSender mailSender;
   private final TemplateEngine templateEngine;
   private final String fromEmail;
   private final String replyTo;

   public EmailNotificationSender(JavaMailSender mailSender, TemplateEngine templateEngine,
                                  @Value("${spring.mail.from}") String fromEmail,
                                  @Value("${spring.mail.reply-to}") String replyTo) {
      this.mailSender = mailSender;
      this.templateEngine = templateEngine;
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

      EmailTemplateData data = command.emailData();

      Context context = new Context();

      context.setVariable("subject", command.title());
      context.setVariable("clinicTitle", data.clinicTitle());
      context.setVariable("firstName", data.firstName());
      context.setVariable("scheduledAt", data.scheduledAt());
      context.setVariable("doctorName", data.doctorName());
      context.setVariable("serviceTitles", data.serviceTitles());

      String html = templateEngine.process(resolveTemplate(command.type()), context);

      MimeMessage mimeMessage = mailSender.createMimeMessage();

      try {
         MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

         helper.setFrom(fromEmail, "BublAPI");
         helper.setReplyTo(replyTo);
         helper.setTo(command.recipientEmail());
         helper.setSubject(command.title());
         helper.setText(html, true);

         mailSender.send(mimeMessage);
      } catch (MessagingException | UnsupportedEncodingException e) {
         throw new IllegalStateException("Failed to create email message", e);
      }
   }

   private String resolveTemplate(NotificationType type) {
      return switch (type) {
         case APPOINTMENT_CANCELLED -> "email/appointment-cancelled";
         case APPOINTMENT_CREATED -> "email/appointment-created";
         case APPOINTMENT_REMINDER -> "email/appointment-reminder";
         case APPOINTMENT_CONFIRMED -> "email/appointment-confirmed";
         case APPOINTMENT_COMPLETED -> "email/appointment-completed";
         case APPOINTMENT_STATUS_CHANGED -> "email/appointment-status-changed";
         case USER_REGISTERED -> "email/user-registered";
         case USER_ACTIVATED -> "email/user-activated";
         case USER_DEACTIVATED -> "email/user-deactivated";
         case CLINIC_SERVICE_DEACTIVATED -> "email/clinic-service-deactivated";
         case PATIENT_CARD_IS_BUSY -> "email/patient-card-is-busy";
         case PATIENT_CARD_LINKED -> "email/patient-card-linked";

         default -> throw new IllegalArgumentException("Unsupported email notification type: " + type);
      };
   }
}

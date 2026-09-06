package org.bublapi.dent.notification.renderer;

import org.bublapi.dent.notification.command.AppointmentNotificationData;
import org.bublapi.dent.notification.command.ClinicServiceNotificationData;
import org.bublapi.dent.notification.command.NotificationData;
import org.bublapi.dent.notification.command.UserNotificationData;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.message.EmailMessage;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class EmailNotificationRenderer {
   private final TemplateEngine templateEngine;

   public EmailNotificationRenderer(TemplateEngine templateEngine) {
      this.templateEngine = templateEngine;
   }

   public EmailMessage render(NotificationType type, NotificationData data, String recipient) {
      String subject = resolveSubject(type);
      String template = resolveTemplate(type);

      Context context = createContext(data);
      context.setVariable("subject", subject);

      String html = templateEngine.process(template, context);

      return new EmailMessage(recipient, subject, html);
   }

   private Context createContext(NotificationData data) {
      Context context = new Context();

      switch (data) {
         case AppointmentNotificationData appointment -> {
            context.setVariable("clinicTitle", appointment.clinicTitle());
            context.setVariable("firstName", appointment.firstName());
            context.setVariable("scheduledAt", appointment.scheduledAt());
            context.setVariable("doctorName", appointment.doctorName());
            context.setVariable("serviceTitles", appointment.serviceTitles());
         }

         case UserNotificationData user -> {
            context.setVariable("clinicTitle", user.clinicTitle());
            context.setVariable("firstName", user.firstName());
         }

         case ClinicServiceNotificationData clinicService -> {
            context.setVariable("clinicTitle", clinicService.clinicTitle());
            context.setVariable("firstName", clinicService.firstName());
            context.setVariable("serviceTitle", clinicService.serviceTitle());
         }
      }

      return context;
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
      };
   }

   private String resolveSubject(NotificationType type) {
      return switch (type) {
         case APPOINTMENT_CREATED -> "Запись создана";
         case APPOINTMENT_CANCELLED -> "Запись отменена";
         case APPOINTMENT_CONFIRMED -> "Запись подтверждена";
         case APPOINTMENT_COMPLETED -> "Приём завершён";
         case APPOINTMENT_STATUS_CHANGED -> "Статус записи изменён";
         case APPOINTMENT_REMINDER -> "Напоминание о записи";
         case USER_REGISTERED -> "Добро пожаловать в BublAPI";
         case USER_ACTIVATED -> "Доступ к аккаунту восстановлен";
         case USER_DEACTIVATED -> "Доступ к аккаунту приостановлен";
         case PATIENT_CARD_LINKED -> "Карта пациента привязана";
         case PATIENT_CARD_IS_BUSY -> "Не удалось привязать карту пациента";
         case CLINIC_SERVICE_DEACTIVATED -> "Услуга клиники недоступна";
      };
   }
}

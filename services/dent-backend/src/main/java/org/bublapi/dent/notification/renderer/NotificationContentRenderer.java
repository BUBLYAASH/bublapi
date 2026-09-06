package org.bublapi.dent.notification.renderer;

import org.bublapi.dent.notification.command.NotificationData;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.message.NotificationContent;
import org.springframework.stereotype.Component;

@Component
public class NotificationContentRenderer {
   public NotificationContent render(NotificationType type, NotificationData data) {
      return switch (type) {
         case APPOINTMENT_CREATED -> new NotificationContent("Запись создана", "Вы успешно записались на приём");
         case APPOINTMENT_CANCELLED -> new NotificationContent("Запись отменена", "Запись на приём была отменена");
         case APPOINTMENT_CONFIRMED ->
                 new NotificationContent("Запись подтверждена", "Ваша запись на приём подтверждена");
         case APPOINTMENT_COMPLETED -> new NotificationContent("Приём завершён", "Приём отмечен как завершённый");
         case APPOINTMENT_STATUS_CHANGED ->
                 new NotificationContent("Статус записи изменён", "Статус вашей записи был изменён");
         case APPOINTMENT_REMINDER ->
                 new NotificationContent("Напоминание о записи", "Напоминаем о предстоящем приёме");
         case USER_REGISTERED -> new NotificationContent("Добро пожаловать", "Ваш аккаунт успешно зарегистрирован");
         case USER_ACTIVATED -> new NotificationContent("Аккаунт активирован", "Доступ к аккаунту восстановлен");
         case USER_DEACTIVATED -> new NotificationContent("Аккаунт деактивирован", "Доступ к аккаунту приостановлен");
         case PATIENT_CARD_LINKED ->
                 new NotificationContent("Карта пациента привязана", "Карта пациента успешно привязана");
         case PATIENT_CARD_IS_BUSY ->
                 new NotificationContent("Карта пациента уже используется", "Не удалось привязать карту пациента");
         case CLINIC_SERVICE_DEACTIVATED ->
                 new NotificationContent("Услуга недоступна", "Одна из услуг клиники была деактивирована");
      };
   }
}

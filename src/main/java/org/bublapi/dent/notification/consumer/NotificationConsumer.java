package org.bublapi.dent.notification.consumer;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.rabbit.RabbitMqConfig;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
public class NotificationConsumer {
   private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

   private final NotificationService notificationService;
   private final ClinicRepository clinicRepository;

   public NotificationConsumer(NotificationService notificationService, ClinicRepository clinicRepository) {
      this.notificationService = notificationService;
      this.clinicRepository = clinicRepository;
   }

   @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_REQUESTED_QUEUE)
   public void handle(CreateNotificationCommand command) {
      log.info("Notification requested: type={}, channel={}, clinicId={}, userId={}", command.type(), command.channel(),
               command.clinicId(), command.userId());

      Clinic clinic = clinicRepository.findByIdAndActiveTrue(command.clinicId())
                                      .orElseThrow(() -> new RuntimeException("Clinic not found"));

      ClinicContext.set(clinic);

      try {
         notificationService.create(command);
      } catch (Exception e) {
         log.error("Failed to handle notification: type={}, channel={}, clinicId={}, appointmentId={}", command.type(),
                   command.channel(), command.clinicId(), command.appointmentId(), e);

         throw e;
      } finally {
         ClinicContext.clear();
      }
   }
}

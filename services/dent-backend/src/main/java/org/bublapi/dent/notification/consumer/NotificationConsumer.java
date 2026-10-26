package org.bublapi.dent.notification.consumer;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.rabbit.RabbitMqConfig;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.service.NotificationQueueService;
import org.bublapi.dent.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class NotificationConsumer {
   private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
   private final NotificationService notificationService;
   private final ClinicRepository clinicRepository;
   private final NotificationQueueService queue;

   public NotificationConsumer(NotificationService notificationService, ClinicRepository clinicRepository,
                               NotificationQueueService queue) {
      this.queue = queue;
      this.notificationService = notificationService;
      this.clinicRepository = clinicRepository;
   }

   @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_REQUESTED_QUEUE)
   @Transactional
   public void handle(CreateNotificationCommand command) {
      queue.lockUser(command.userId());
      if (!queue.userExists(command.userId())) {
         return;
      }
      log.info("Notification requested: type={}, clinicId={}, userId={}", command.type(), command.clinicId(),
               command.userId());

      Clinic clinic = clinicRepository.findById(command.clinicId())
                                      .orElseThrow(() -> new RuntimeException("Clinic not found"));

      ClinicContext.set(clinic);

      try {
         command = queue.load(command);
         if (command == null) return;
         notificationService.create(command);
      } finally {
         ClinicContext.clear();
      }
   }
}

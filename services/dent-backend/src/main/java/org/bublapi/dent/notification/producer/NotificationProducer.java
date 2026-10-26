package org.bublapi.dent.notification.producer;

import org.bublapi.dent.common.rabbit.RabbitMqConfig;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.service.NotificationQueueService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {
   private final RabbitTemplate rabbitTemplate;
   private final NotificationQueueService queue;

   public NotificationProducer(RabbitTemplate rabbitTemplate,
                               NotificationQueueService queue) {
      this.rabbitTemplate = rabbitTemplate;
      this.queue = queue;
   }

   public void publish(CreateNotificationCommand command) {
      if (!queue.prepare(command)) {
         return;
      }
      command = new CreateNotificationCommand(command.requestId(), command.clinicId(), command.userId(),
                                              command.appointmentId(), command.type(), null, command.scheduledAt());
      rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATION_EXCHANGE,
                                    RabbitMqConfig.NOTIFICATION_REQUESTED_ROUTING_KEY, command);
   }
}

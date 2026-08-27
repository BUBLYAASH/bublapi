package org.bublapi.dent.notification.producer;

import org.bublapi.dent.common.rabbit.RabbitMqConfig;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {
   private final RabbitTemplate rabbitTemplate;

   public NotificationProducer(RabbitTemplate rabbitTemplate) {
      this.rabbitTemplate = rabbitTemplate;
   }

   public void publish(CreateNotificationCommand command) {
      rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATION_EXCHANGE,
                                    RabbitMqConfig.NOTIFICATION_REQUESTED_ROUTING_KEY, command);
   }
}

package org.bublapi.dent.common.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
   public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
   public static final String NOTIFICATION_REQUESTED_QUEUE = "notification.requested.queue";
   public static final String NOTIFICATION_REQUESTED_ROUTING_KEY = "notification.requested";

   public static final String NOTIFICATION_DLX = "notification.dlx";
   public static final String NOTIFICATION_REQUESTED_DLQ = "notification.requested.dlq";
   public static final String NOTIFICATION_REQUESTED_DLQ_ROUTING_KEY = "notification.requested.dlq";

   @Bean
   public DirectExchange appointmentExchange() {
      return new DirectExchange(NOTIFICATION_EXCHANGE);
   }

   @Bean
   public DirectExchange appointmentDeadLetterExchange() {
      return new DirectExchange(NOTIFICATION_DLX);
   }

   @Bean
   public Queue appointmentCreateDlq() {
      return QueueBuilder.durable(NOTIFICATION_REQUESTED_DLQ).build();
   }

   @Bean
   public Binding appointmentCreateDlqBinding() {
      return BindingBuilder.bind(appointmentCreateDlq())
                           .to(appointmentDeadLetterExchange())
                           .with(NOTIFICATION_REQUESTED_DLQ_ROUTING_KEY);
   }

   @Bean
   public Queue appointmentCreatedQueue() {
      return QueueBuilder.durable(NOTIFICATION_REQUESTED_QUEUE)
                         .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                         .withArgument("x-dead-letter-routing-key", NOTIFICATION_REQUESTED_DLQ_ROUTING_KEY)
                         .build();
   }

   @Bean
   public Binding appointmentCreatedBinding() {
      return BindingBuilder.bind(appointmentCreatedQueue())
                           .to(appointmentExchange())
                           .with(NOTIFICATION_REQUESTED_ROUTING_KEY);
   }

   @Bean
   public MessageConverter jsonMessageConverter() {
      return new Jackson2JsonMessageConverter();
   }
}
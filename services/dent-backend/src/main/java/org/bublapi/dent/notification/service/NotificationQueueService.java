package org.bublapi.dent.notification.service;

import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.renderer.NotificationContentRenderer;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.bublapi.dent.user.repository.UserRepository;
import org.bublapi.dent.user.service.UserOperationLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationQueueService {
   private final NotificationRepository notifications;
   private final NotificationTransactionService transactions;
   private final NotificationContentRenderer renderer;
   private final UserRepository users;
   private final UserOperationLock operationLock;

   public NotificationQueueService(NotificationRepository notifications, NotificationTransactionService transactions,
                                   NotificationContentRenderer renderer, UserRepository users,
                                   UserOperationLock operationLock) {
      this.notifications = notifications;
      this.transactions = transactions;
      this.renderer = renderer;
      this.users = users;
      this.operationLock = operationLock;
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public boolean prepare(CreateNotificationCommand command) {
      operationLock.lockUser(command.userId());
      var user = users.findById(command.userId()).orElse(null);
      if (user == null || user.getClinic() == null || !user.getClinic().getId().equals(command.clinicId()))
         return false;
      var previousClinic = ClinicContext.get();
      ClinicContext.set(user.getClinic());
      try {
         var content = renderer.render(command.type());
         transactions.prepare(command, NotificationChannel.IN_APP, content);
         transactions.prepare(command, NotificationChannel.EMAIL, content);
         return true;
      } finally {
         ClinicContext.set(previousClinic);
      }
   }

   @Transactional(readOnly = true)
   public CreateNotificationCommand load(CreateNotificationCommand envelope) {
      if (envelope.data() != null) return envelope;
      var notification = notifications.findByRequestIdAndChannel(envelope.requestId(), NotificationChannel.IN_APP)
                                      .or(() -> notifications.findByRequestIdAndChannel(envelope.requestId(),
                                                                                        NotificationChannel.EMAIL))
                                      .orElse(null);
      if (notification == null || notification.getUser() == null || !notification.getUser()
                                                                                 .getId()
                                                                                 .equals(envelope.userId()) || notification.getData() == null || !notification.getClinic()
                                                                                                                                                              .getId()
                                                                                                                                                              .equals(envelope.clinicId()))
         return null;
      return new CreateNotificationCommand(notification.getRequestId(), notification.getClinic().getId(),
                                           notification.getUser().getId(),
                                           notification.getAppointment() == null ? null : notification.getAppointment()
                                                                                                      .getId(),
                                           notification.getType(), notification.getData(),
                                           notification.getScheduledAt());
   }

   public void lockUser(UUID userId) {
      operationLock.lockUser(userId);
   }

   public boolean userExists(UUID userId) {
      return users.existsById(userId);
   }
}

package org.bublapi.dent.notification.service;

import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationTransactionService {
   private final NotificationRepository notificationRepository;
   private final AppointmentRepository appointmentRepository;
   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;

   public NotificationTransactionService(NotificationRepository notificationRepository,
                                         AppointmentRepository appointmentRepository, ClinicRepository clinicRepository,
                                         UserRepository userRepository) {
      this.notificationRepository = notificationRepository;
      this.appointmentRepository = appointmentRepository;
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public Notification prepare(CreateNotificationCommand command) {
      Notification existing = notificationRepository.findByRequestId(command.requestId()).orElse(null);

      if (existing != null) {
         if (existing.getStatus() == NotificationStatus.SENT) {
            return existing;
         }

         existing.setStatus(NotificationStatus.PENDING);
         existing.setErrorMessage(null);
         return existing;
      }

      Clinic clinic = clinicRepository.findById(command.clinicId())
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      Notification notification = new Notification();

      notification.setRequestId(command.requestId());
      notification.setClinic(clinic);
      notification.setType(command.type());
      notification.setChannel(command.channel());
      notification.setTitle(command.title());
      notification.setMessage(command.message());
      notification.setScheduledAt(command.scheduledAt());
      notification.setStatus(NotificationStatus.PENDING);

      if (command.appointmentId() != null) {
         Appointment appointment = appointmentRepository.findById(command.appointmentId())
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Appointment not found"));

         notification.setAppointment(appointment);
      }

      if (command.userId() != null) {
         User user = userRepository.findById(command.userId())
                                   .orElseThrow(() -> new ResourceNotFoundException("User not found"));

         notification.setUser(user);
      }

      return notificationRepository.save(notification);
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void markAsSent(UUID notificationId) {
      Notification notification = findNotification(notificationId);

      notification.setStatus(NotificationStatus.SENT);
      notification.setSentAt(LocalDateTime.now());
      notification.setErrorMessage(null);
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void markAsFailed(UUID notificationId, String errorMessage) {
      Notification notification = findNotification(notificationId);

      notification.setStatus(NotificationStatus.FAILED);
      notification.setErrorMessage(limitErrorMessage(errorMessage));
   }

   private Notification findNotification(UUID notificationId) {
      return notificationRepository.findById(notificationId)
                                   .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
   }

   private String limitErrorMessage(String errorMessage) {
      if (errorMessage == null) {
         return "Unknown notification error";
      }

      if (errorMessage.length() > 1000) {
         return errorMessage.substring(0, 1000);
      } else {
         return errorMessage;
      }
   }
}

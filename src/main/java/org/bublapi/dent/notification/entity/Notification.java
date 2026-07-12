package org.bublapi.dent.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "clinic_id", nullable = false)
   private Clinic clinic;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "appointment_id")
   private Appointment appointment;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private NotificationType type;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 50)
   private NotificationChannel channel;

   @Column(nullable = false, length = 50)
   private String title;

   @Column(nullable = false)
   private String message;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 50)
   private NotificationStatus status = NotificationStatus.PENDING;

   @Column(name = "scheduled_at")
   private LocalDateTime scheduledAt;

   @Column(name = "sent_at")
   private LocalDateTime sentAt;

   @Column(nullable = false)
   private boolean read = false;

   @Column(name = "read_at")
   private LocalDateTime readAt;

   @Column(nullable = false)
   private boolean deleted = false;

   @Column(name = "deleted_at")
   private LocalDateTime deletedAt;

   @Column(name = "request_id", nullable = false, unique = true)
   private UUID requestId;

   @Column(name = "error_message", length = 1000)
   private String errorMessage;

   @Column(name = "created_at", nullable = false)
   private LocalDateTime createdAt;

   @PrePersist
   void prePersist() {
      if (createdAt == null) {
         createdAt = LocalDateTime.now();
      }

      if (status == null) {
         status = NotificationStatus.PENDING;
      }
   }
}

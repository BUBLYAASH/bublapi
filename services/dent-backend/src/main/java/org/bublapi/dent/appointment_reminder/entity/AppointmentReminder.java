package org.bublapi.dent.appointment_reminder.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.appointment.entity.Appointment;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "appointment_reminders")
@Getter
@Setter
public class AppointmentReminder {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "appointment_id", nullable = false)
   private Appointment appointment;

   @Column(name = "reminder_type", length = 50, nullable = false)
   private String reminderType;

   @Column(name = "due_at", nullable = false)
   private LocalDateTime dueAt;

   @Column(length = 25, nullable = false)
   @Enumerated(EnumType.STRING)
   private ReminderStatus status = ReminderStatus.PENDING;

   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @Column(name = "published_at")
   private LocalDateTime publishedAt;

   @PrePersist
   private void prePersist() {
      this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
   }
}

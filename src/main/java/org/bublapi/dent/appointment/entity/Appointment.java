package org.bublapi.dent.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.patient.entity.Patient;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "appointments", uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "scheduled_at"}))
public class Appointment {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "clinic_id", nullable = false)
   private Clinic clinic;

   @ManyToOne
   @JoinColumn(name = "patient_id", nullable = false)
   private Patient patient;

   @ManyToOne
   @JoinColumn(name = "doctor_id", nullable = false)
   private Doctor doctor;

   @Column(name = "scheduled_at", nullable = false)
   private LocalDateTime scheduledAt;

   @Column(name = "end_at", nullable = false)
   private LocalDateTime endAt;

   private String comment;

   @Column(name = "total_price", nullable = false)
   private Integer totalPrice;

   @Column(name = "created_at", nullable = false)
   private LocalDateTime createdAt;

   @Enumerated(EnumType.STRING)
   @Column(length = 25, nullable = false)
   private AppointmentStatus status = AppointmentStatus.CREATED;

   @PrePersist
   private void prePersist() {
      this.createdAt = LocalDateTime.now();
   }
}

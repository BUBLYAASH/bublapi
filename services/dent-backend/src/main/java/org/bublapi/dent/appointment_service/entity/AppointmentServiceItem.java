package org.bublapi.dent.appointment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.clinic_service.entity.ClinicService;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "appointment_services", uniqueConstraints = @UniqueConstraint(columnNames = {"appointment_id",
                                                                                           "position"}))
public class AppointmentServiceItem {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "appointment_id", nullable = false)
   private Appointment appointment;

   @ManyToOne
   @JoinColumn(name = "clinic_service_id", nullable = false)
   private ClinicService clinicService;

   @Column(nullable = false)
   private String title;

   @Column(nullable = false)
   private Integer price;

   @Column(name = "duration_minutes", nullable = false)
   private Integer durationMinutes;

   @Column(nullable = false)
   private Integer quantity = 1;

   @Column(nullable = false)
   private Integer position;
}

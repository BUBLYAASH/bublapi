package org.bublapi.dent.clinic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clinic_services", uniqueConstraints = @UniqueConstraint(columnNames = {"clinic_id", "service_id"}))
@Filter(name = "clinicFilter")
public class ClinicService {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "clinic_id")
   private Clinic clinic;

   @ManyToOne
   @JoinColumn(name = "service_id")
   private DentalService dentalService;

   @Column(nullable = false)
   private Integer price;

   @Column(name = "duration_minutes", nullable = false)
   private Integer durationMinutes;

   @Column(name = "created_at", nullable = false)
   private LocalDateTime createdAt;

   @Column(nullable = false)
   private boolean active = true;

   @Column(name = "disabled_by_clinic", nullable = false)
   private boolean disabledByClinic = false;

   @PrePersist
   public void prePersist() {
      this.createdAt = LocalDateTime.now();
   }
}

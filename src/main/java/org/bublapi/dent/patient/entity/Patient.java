package org.bublapi.dent.patient.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.user.entity.User;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "patients", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "phone"}),
        @UniqueConstraint(columnNames = {"user_id"})
})
@Filter(name = "clinicFilter", condition = "clinic_id = :clinicId")
public class Patient {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "clinic_id", nullable = false)
   private Clinic clinic;

   @OneToOne
   @JoinColumn(name = "user_id", unique = true)
   private User user;

   @Column(name = "first_name", length = 50, nullable = false)
   private String firstName;

   @Column(name = "last_name", length = 50, nullable = false)
   private String lastName;

   @Column(name = "middle_name", length = 50)
   private String middleName;

   @Column(length = 15, nullable = false)
   private String phone;

   private String email;

   @Column(name = "birth_date")
   private LocalDate birthDate;

   private String notes;

   private String allergies;

   @Column(name = "chronic_diseases")
   private String chronicDiseases;

   @Column(name = "created_at", nullable = false)
   private LocalDateTime createdAt;

   @Column(name = "updated_at")
   private LocalDateTime updatedAt;

   @Column(nullable = false)
   private Boolean active = true;

   @PrePersist
   private void prePersist() {
      this.createdAt = LocalDateTime.now();
      this.updatedAt = LocalDateTime.now();
   }

   @PreUpdate
   private void preUpdate() {
      this.updatedAt = LocalDateTime.now();
   }
}

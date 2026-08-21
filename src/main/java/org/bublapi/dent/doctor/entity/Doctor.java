package org.bublapi.dent.doctor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.user.entity.User;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "doctors")
@Filter(name = "clinicFilter")
public class Doctor {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "clinic_id", nullable = false)
   private Clinic clinic;

   @OneToOne
   @JoinColumn(name = "user_id", unique = true)
   private User user;

   @Column(name = "first_name", nullable = false, length = 50)
   private String firstName;

   @Column(name = "last_name", nullable = false, length = 50)
   private String lastName;

   @Column(name = "middle_name", length = 50)
   private String middleName;

   @Column(nullable = false)
   private String specialty;

   @Column(name = "avatar_url")
   private String avatarUrl;

   private String description;

   @Column(name = "created_at", nullable = false)
   private LocalDateTime createdAt;

   @Column(name = "updated_at", nullable = false)
   private LocalDateTime updatedAt;

   @Column(nullable = false)
   private Boolean active = true;

   @Column(name = "disabled_by_clinic", nullable = false)
   private Boolean disabledByClinic = false;

   @PrePersist
   public void prePersist() {
      this.createdAt = LocalDateTime.now();
      this.updatedAt = LocalDateTime.now();
   }

   @PreUpdate
   public void preUpdate() {
      this.updatedAt = LocalDateTime.now();
   }
}

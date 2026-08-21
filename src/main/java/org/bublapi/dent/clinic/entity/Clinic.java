package org.bublapi.dent.clinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clinics")
public class Clinic {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(length = 100, nullable = false)
   private String title;

   private String description;

   @Column(nullable = false)
   private String address;

   @Column(length = 15)
   private String phone;

   private String email;

   private String website;

   @Column(length = 50, nullable = false)
   private String timezone = "Europe/Moscow";

   @Column(name = "created_at", nullable = false)
   private LocalDateTime createdAt;

   @Column(name = "updated_at")
   private LocalDateTime updatedAt;

   @Column(nullable = false)
   private boolean active = true;

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

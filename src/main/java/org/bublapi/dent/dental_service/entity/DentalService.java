package org.bublapi.dent.dental_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dental_services")
public class DentalService {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(unique = true, nullable = false)
   private String title;

   private String description;

   @Enumerated(EnumType.STRING)
   @Column(length = 50, nullable = false)
   private ServiceCategory category;

   @Column(name = "default_duration_minutes", nullable = false)
   private Integer defaultDurationMinutes;

   @Column(nullable = false)
   private boolean active = true;
}

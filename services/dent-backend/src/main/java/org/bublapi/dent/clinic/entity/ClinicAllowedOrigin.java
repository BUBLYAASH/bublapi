package org.bublapi.dent.clinic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clinic_allowed_origins", uniqueConstraints = @UniqueConstraint(columnNames = {"clinic_id", "origin"}))
public class ClinicAllowedOrigin {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;
   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "clinic_id", nullable = false)
   private Clinic clinic;
   @Column(nullable = false, length = 255)
   private String origin;
}

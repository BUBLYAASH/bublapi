package org.bublapi.dent.apikey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic.entity.Clinic;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "api_keys")
public class ApiKey {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @OneToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "clinic_id", nullable = false, unique = true)
   private Clinic clinic;

   @Column(length = 50, nullable = false)
   private String name;

   @Column(length = 16, nullable = false, unique = true)
   private String prefix;

   @Column(nullable = false)
   private String hash;

   @Column(name = "expires_at", nullable = false)
   private LocalDateTime expiresAt;

   @Column(name = "grace_until", nullable = false)
   private LocalDateTime graceUntil;

   @Column(nullable = false)
   private boolean active = true;
}

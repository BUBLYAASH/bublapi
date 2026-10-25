package org.bublapi.dent.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.user.entity.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(name = "token_hash", nullable = false, unique = true, length = 64)
   private String tokenHash;

   @Column(name = "expires_at", nullable = false)
   private LocalDateTime expiresAt;

   @Column(name = "revoked_at")
   private LocalDateTime revokedAt;

   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @Enumerated(EnumType.STRING)
   @Column(name = "token_type", nullable = false, length = 10)
   private RefreshTokenType tokenType;

   public boolean isRevoked() {
      return this.revokedAt != null;
   }

   public boolean isExpired() {
      return !expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC));
   }

   @PrePersist
   private void prePersist() {
      if (createdAt == null) {
         this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
      }
   }
}

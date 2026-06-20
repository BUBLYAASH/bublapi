package org.bublapi.dent.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.role.entity.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_clinic_email", columnNames = {"clinic_id", "email"}),
        @UniqueConstraint(name = "uk_users_clinic_phone", columnNames = {"clinic_id", "phone"})
})
public class User {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(nullable = false)
   private String email;

   @Column(nullable = false, length = 15)
   private String phone;

   @Column(name = "first_name", length = 50, nullable = false)
   private String firstName;

   @Column(name = "last_name", length = 50, nullable = false)
   private String lastName;

   @Column(name = "middle_name", length = 50)
   private String middleName;

   @Column(name = "password_hash", nullable = false)
   private String passwordHash;

   @ManyToOne
   @JoinColumn(name = "clinic_id", nullable = false)
   private Clinic clinic;

   @Column(name = "joined_at", nullable = false)
   private LocalDateTime joinedAt;

   @Column(name = "updated_at")
   private LocalDateTime updatedAt;

   @Column(nullable = false)
   private Boolean enabled = true;

   @Column(name = "disabled_by_clinic", nullable = false)
   private Boolean disabledByClinic = false;

   @ManyToMany
   @JoinTable(
           name = "user_roles",
           joinColumns = @JoinColumn(name = "user_id"),
           inverseJoinColumns = @JoinColumn(name = "role_id")
   )
   private Set<Role> roles = new HashSet<>();

   @PrePersist
   public void prePersist() {
      this.joinedAt = LocalDateTime.now();
      this.updatedAt = LocalDateTime.now();
   }

   @PreUpdate
   public void preUpdate() {
      this.updatedAt = LocalDateTime.now();
   }
}

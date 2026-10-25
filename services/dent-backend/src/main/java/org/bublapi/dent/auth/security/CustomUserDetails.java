package org.bublapi.dent.auth.security;

import org.bublapi.dent.user.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
   @Serial
   private static final long serialVersionUID = 1L;

   private final UUID userId;
   private final UUID clinicId;
   private final String username;
   private final String password;
   private final boolean enabled;
   private final List<SimpleGrantedAuthority> authorities;

   public CustomUserDetails(User user) {
      this.userId = user.getId();
      this.clinicId = user.getClinic() != null ? user.getClinic().getId() : null;
      this.username = user.getEmail();
      this.password = user.getPasswordHash();
      this.enabled = user.isEnabled();
      this.authorities = user.getRoles()
                             .stream()
                             .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                             .toList();
   }

   public UUID getId() {
      return this.userId;
   }

   public UUID getClinicId() {
      return this.clinicId;
   }

   @Override
   public List<SimpleGrantedAuthority> getAuthorities() {
      return this.authorities;
   }

   @Override
   public String getPassword() {
      return this.password;
   }

   @Override
   public String getUsername() {
      return this.username;
   }

   @Override
   public boolean isEnabled() {
      return this.enabled;
   }
}

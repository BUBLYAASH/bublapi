package org.bublapi.dent.auth.security;

import org.bublapi.dent.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
   private final User user;

   public CustomUserDetails(User user) {
      this.user = user;
   }

   public UUID getId() {
      return user.getId();
   }

   public UUID getClinicId() {
      return user.getClinic() != null ? user.getClinic().getId() : null;
   }

   @Override
   public String getUsername() {
      return user.getEmail();
   }

   @Override
   public String getPassword() {
      return user.getPasswordHash();
   }

   @Override
   public boolean isEnabled() {
      return user.isEnabled();
   }

   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
      return user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).toList();
   }
}

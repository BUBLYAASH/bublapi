package org.bublapi.dent.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("clinicSecurity")
public class ClinicSecurity {
   public boolean hasAccess(Authentication auth, UUID clinicId) {
      boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

      if (isAdmin) {
         return true;
      }

      CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

      return userDetails.getClinicId().equals(clinicId);
   }
}

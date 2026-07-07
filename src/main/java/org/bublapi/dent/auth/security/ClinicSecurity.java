package org.bublapi.dent.auth.security;

import org.bublapi.dent.common.context.ClinicContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("clinicSecurity")
public class ClinicSecurity {
   public boolean hasAccess(Authentication auth) {
      if (auth == null || !auth.isAuthenticated()) {
         return false;
      }

      boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

      if (isAdmin) {
         return true;
      }

      Object principal = auth.getPrincipal();

      if (!(principal instanceof CustomUserDetails userDetails)) {
         return false;
      }

      UUID userClinicId = userDetails.getClinicId();
      UUID apiClinicId = ClinicContext.getClinicId();

      return userClinicId.equals(apiClinicId);
   }
}

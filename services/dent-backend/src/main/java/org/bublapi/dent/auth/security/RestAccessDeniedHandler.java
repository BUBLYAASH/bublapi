package org.bublapi.dent.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bublapi.dent.logging.SecurityLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

   private final SecurityLogService securityLogService;

   public RestAccessDeniedHandler(SecurityLogService securityLogService) {
      this.securityLogService = securityLogService;
   }

   @Override
   public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) {
      securityLogService.accessDenied();

      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
   }
}

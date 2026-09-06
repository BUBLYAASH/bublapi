package org.bublapi.dent.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bublapi.dent.logging.SecurityLogService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
   private final SecurityLogService securityLogService;

   public RestAuthenticationEntryPoint(SecurityLogService securityLogService) {
      this.securityLogService = securityLogService;
   }

   @Override
   public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) {
      securityLogService.authenticationRequired();

      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
   }
}

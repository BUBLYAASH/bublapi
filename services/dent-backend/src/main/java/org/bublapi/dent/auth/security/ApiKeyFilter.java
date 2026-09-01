package org.bublapi.dent.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.logging.SecurityLogService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {
   private final ApiKeyService apiKeyService;
   private final SecurityLogService securityLogService;

   public ApiKeyFilter(ApiKeyService apiKeyService, SecurityLogService securityLogService) {
      this.apiKeyService = apiKeyService;
      this.securityLogService = securityLogService;
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws
           ServletException,
           IOException {

      String path = request.getRequestURI();

      if (path.startsWith("/api/admin") || path.startsWith("/actuator/") || path.startsWith(
              "/swagger") || path.startsWith("/v3/api-docs")) {
         filterChain.doFilter(request, response);
         return;
      }

      String apiKey = request.getHeader("X-API-KEY");

      if (apiKey == null || apiKey.isBlank()) {
         securityLogService.apiKeyAuthenticationFailed("MISSING_API_KEY", null);

         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      }
      try {
         ApiKey key = apiKeyService.validate(apiKey);

         ClinicContext.set(key.getClinic());

         securityLogService.apiKeyAuthenticationSuccess(key.getId(), key.getClinic().getId());

         filterChain.doFilter(request, response);
      } catch (IllegalArgumentException | ResourceNotFoundException e) {
         securityLogService.apiKeyAuthenticationFailed("INVALID_API_KEY", apiKey);

         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      } finally {
         ClinicContext.clear();
      }
   }
}

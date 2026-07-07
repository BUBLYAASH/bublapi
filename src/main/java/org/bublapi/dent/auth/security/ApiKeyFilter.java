package org.bublapi.dent.auth.security;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.common.context.ClinicContext;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {
   private final ApiKeyService apiKeyService;
   private final EntityManager entityManager;

   public ApiKeyFilter(ApiKeyService apiKeyService, EntityManager entityManager) {
      this.apiKeyService = apiKeyService;
      this.entityManager = entityManager;
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws
           ServletException,
           IOException {

      String path = request.getRequestURI();

      if (path.startsWith("/api/admin") || path.startsWith("/actuator/health") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
         filterChain.doFilter(request, response);
         return;
      }

      String apiKey = request.getHeader("X-API-KEY");

      if (apiKey == null || apiKey.isBlank()) {
         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      }
      try {
         ApiKey key = apiKeyService.validate(apiKey);

         Session session = entityManager.unwrap(Session.class);

         session.enableFilter("clinicFilter").setParameter("clinicId", key.getClinic().getId());

         ClinicContext.set(key.getClinic());

         filterChain.doFilter(request, response);
      } finally {
         ClinicContext.clear();

         Session session = entityManager.unwrap(Session.class);
         session.disableFilter("clinicFilter");
      }
   }
}

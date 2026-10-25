package org.bublapi.dent.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.clinic.repository.ClinicAllowedOriginRepository;
import org.bublapi.dent.clinic.service.WebsiteOrigin;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OriginPolicy {
   private final ClinicAllowedOriginRepository origins;
   private final ApiKeyService apiKeys;
   private final Set<String> adminOrigins;

   public OriginPolicy(ClinicAllowedOriginRepository origins, ApiKeyService apiKeys,
                       @Value("${security.admin-allowed-origins:http://localhost:3001}") String configured) {
      this.origins = origins;
      this.apiKeys = apiKeys;
      this.adminOrigins = Arrays.stream(configured.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(WebsiteOrigin::requireOrigin)
                                .collect(Collectors.toUnmodifiableSet());
   }

   public boolean allows(HttpServletRequest request) {
      String origin = request.getHeader("Origin");
      if (!WebsiteOrigin.isCanonical(origin)) return false;
      String path = request.getRequestURI();
      if (path.equals("/api/v1/admin") || path.startsWith("/api/v1/admin/") || path.startsWith(
              "/swagger") || path.startsWith("/v3/api-docs") || path.startsWith("/actuator/")) {
         return adminOrigins.contains(origin);
      }

      if (CorsUtils.isPreFlightRequest(request)) return origins.existsByClinic_ActiveTrueAndOrigin(origin);
      String key = request.getHeader("X-API-KEY");
      if (key == null || key.isBlank()) return false;
      try {
         return origins.existsByClinic_IdAndClinic_ActiveTrueAndOrigin(apiKeys.validate(key).getClinic().getId(),
                                                                       origin);
      } catch (IllegalArgumentException | ResourceNotFoundException e) {
         return false;
      }
   }
}

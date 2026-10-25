package org.bublapi.dent.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RefreshOriginFilter extends OncePerRequestFilter {
   private final OriginPolicy originPolicy;

   public RefreshOriginFilter(OriginPolicy originPolicy) {
      this.originPolicy = originPolicy;
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
      String path = request.getRequestURI();

      boolean protectedEndpoint = path.equals("/api/v1/auth/refresh") || path.equals(
              "/api/v1/auth/logout") || path.equals("/api/v1/admin/auth/refresh") || path.equals(
              "/api/v1/admin/auth/logout");

      if (protectedEndpoint && "POST".equalsIgnoreCase(request.getMethod())) {
         if (!originPolicy.allows(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
         }
      }

      filterChain.doFilter(request, response);
   }
}

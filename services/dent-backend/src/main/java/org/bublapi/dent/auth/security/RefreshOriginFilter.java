package org.bublapi.dent.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class RefreshOriginFilter extends OncePerRequestFilter {
   private static final Set<String> ALLOWED_ORIGINS = Set.of("http://localhost:3000", "http://localhost:3001",
                                                             "http://localhost:3002");

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
      String path = request.getRequestURI();

      boolean protectedEndpoint = path.equals("/api/v1/auth/refresh") || path.equals(
              "/api/v1/auth/logout") || path.equals("/api/v1/admin/auth/refresh") || path.equals(
              "/api/v1/admin/auth/logout");

      if (protectedEndpoint && "POST".equalsIgnoreCase(request.getMethod())) {
         String origin = request.getHeader("Origin");

         if (origin == null || !ALLOWED_ORIGINS.contains(origin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
         }
      }

      filterChain.doFilter(request, response);
   }
}

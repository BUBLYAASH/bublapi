package org.bublapi.dent.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bublapi.dent.auth.service.JwtService;
import org.bublapi.dent.logging.SecurityLogService;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
   private final JwtService jwtService;
   private final CustomUserDetailsService userDetailsService;
   private final SecurityLogService securityLogService;

   public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService,
                                  SecurityLogService securityLogService) {
      this.jwtService = jwtService;
      this.userDetailsService = userDetailsService;
      this.securityLogService = securityLogService;
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws
           ServletException,
           IOException {
      String authHeader = request.getHeader("Authorization");

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
         filterChain.doFilter(request, response);
         return;
      }

      try {
         String token = authHeader.substring(7);

         String userIdValue = jwtService.extractUserId(token);

         UUID userId;

         try {
            userId = UUID.fromString(userIdValue);
         } catch (IllegalArgumentException e) {
            securityLogService.jwtAuthenticationFailed("INVALID_USER_ID");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         UserDetails userDetails = userDetailsService.loadUserByUserId(userId);

         if (!jwtService.isTokenValid(token, userId)) {
            securityLogService.jwtAuthenticationFailed("INVALID_TOKEN");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         if (!userDetails.isEnabled()) {
            securityLogService.jwtAuthenticationFailed("USER_DISABLED");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);


            if (userDetails instanceof CustomUserDetails customUserDetails) {
               MDC.put("userId", customUserDetails.getId().toString());

               if (customUserDetails.getClinicId() != null) {
                  MDC.put("clinicId", customUserDetails.getClinicId().toString());
               }

               securityLogService.jwtAuthenticationSuccess();
            }
         }
      } catch (ExpiredJwtException e) {
         securityLogService.jwtAuthenticationFailed("EXPIRED_TOKEN");

         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      } catch (UsernameNotFoundException e) {
         securityLogService.jwtAuthenticationFailed("USER_NOT_FOUND");

         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      } catch (JwtException e) {
         securityLogService.jwtAuthenticationFailed("INVALID_TOKEN");

         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      }

      try {
         filterChain.doFilter(request, response);
      } finally {
         MDC.remove("userId");
         MDC.remove("clinicId");
      }
   }
}

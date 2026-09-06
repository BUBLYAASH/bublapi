package org.bublapi.dent.auth.config;


import org.bublapi.dent.auth.security.ApiKeyFilter;
import org.bublapi.dent.auth.security.JwtAuthenticationFilter;
import org.bublapi.dent.auth.security.RestAccessDeniedHandler;
import org.bublapi.dent.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
   private final JwtAuthenticationFilter jwtAuthenticationFilter;
   private final ApiKeyFilter apiKeyFilter;
   private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
   private final RestAccessDeniedHandler restAccessDeniedHandler;

   public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ApiKeyFilter apiKeyFilter,
                         RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                         RestAccessDeniedHandler restAccessDeniedHandler) {
      this.jwtAuthenticationFilter = jwtAuthenticationFilter;
      this.apiKeyFilter = apiKeyFilter;
      this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
      this.restAccessDeniedHandler = restAccessDeniedHandler;
   }

   @Bean
   public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
   }

   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http.csrf(AbstractHttpConfigurer::disable)
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint)
                                                   .accessDeniedHandler(restAccessDeniedHandler))
          .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**", "/api/admin/auth/**")
                                             .permitAll()
                                             .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                                              "/actuator/health/**", "/actuator/info/**")
                                             .hasRole("ADMIN")
                                             .requestMatchers("/actuator/**")
                                             .denyAll()
                                             .requestMatchers("/api/public/**")
                                             .permitAll()
                                             .anyRequest()
                                             .authenticated())
          .formLogin(AbstractHttpConfigurer::disable)
          .httpBasic(AbstractHttpConfigurer::disable)
          .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
          .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

      return http.build();
   }

   @Bean
   public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration configuration = new CorsConfiguration();

      configuration.setAllowedOrigins(List.of("http://localhost:3001", "http://localhost:3002"));
      configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
      configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-KEY", "X-Request-ID"));
      configuration.setExposedHeaders(List.of("X-Request-ID"));
      configuration.setAllowCredentials(true);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

      source.registerCorsConfiguration("/**", configuration);

      return source;
   }
}

package org.bublapi.dent.auth.config;


import org.bublapi.dent.auth.security.ApiKeyFilter;
import org.bublapi.dent.auth.security.JwtAuthenticationFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
   private final JwtAuthenticationFilter jwtAuthenticationFilter;
   private final ApiKeyFilter apiKeyFilter;

   public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ApiKeyFilter apiKeyFilter) {
      this.jwtAuthenticationFilter = jwtAuthenticationFilter;
      this.apiKeyFilter = apiKeyFilter;
   }

   @Bean
   public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
   }

   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**")
                                             .permitAll()
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
}

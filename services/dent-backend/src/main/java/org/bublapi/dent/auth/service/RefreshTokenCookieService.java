package org.bublapi.dent.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenCookieService {
   private static final Duration MAX_AGE = Duration.ofDays(30);

   public void setUserCookie(HttpServletResponse response, String token) {
      setCookie(response, "refresh_token", token, "/api/v1/auth");
   }

   public void setAdminCookie(HttpServletResponse response, String token) {
      setCookie(response, "admin_refresh_token", token, "/api/v1/admin/auth");
   }

   public void clearUserCookie(HttpServletResponse response) {
      clearCookie(response, "refresh_token", "/api/v1/auth");
   }

   public void clearAdminCookie(HttpServletResponse response) {
      clearCookie(response, "admin_refresh_token", "/api/v1/admin/auth");
   }

   private void setCookie(HttpServletResponse response, String name, String token, String path) {
      ResponseCookie cookie = ResponseCookie.from(name, token)
                                            .httpOnly(true)
                                            .secure(true)
                                            .sameSite("Lax")
                                            .path(path)
                                            .maxAge(MAX_AGE)
                                            .build();

      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
   }

   private void clearCookie(HttpServletResponse response, String name, String path) {
      ResponseCookie cookie = ResponseCookie.from(name, "")
                                            .httpOnly(true)
                                            .secure(true)
                                            .sameSite("Lax")
                                            .path(path)
                                            .maxAge(Duration.ZERO)
                                            .build();

      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
   }
}

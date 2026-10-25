package org.bublapi.dent.auth;

import org.bublapi.dent.auth.service.RefreshTokenCookieService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieServiceTest {
   final RefreshTokenCookieService service = new RefreshTokenCookieService();

   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void setsAndClearsSecureCookieAtMatchingScope(boolean admin) {
      var response = new MockHttpServletResponse();
      if (admin) service.setAdminCookie(response, "opaque-token");
      else service.setUserCookie(response, "opaque-token");
      String name = admin ? "admin_refresh_token" : "refresh_token";
      String path = admin ? "/api/v1/admin/auth" : "/api/v1/auth";
      var cookie = response.getCookie(name);
      assertThat(cookie).isNotNull();
      assertThat(cookie.getValue()).isEqualTo("opaque-token");
      assertThat(cookie.getPath()).isEqualTo(path);
      assertThat(cookie.getMaxAge()).isEqualTo(30 * 24 * 60 * 60);
      assertThat(cookie.isHttpOnly()).isTrue();
      assertThat(cookie.getSecure()).isTrue();
      assertThat(response.getHeader("Set-Cookie")).contains("SameSite=Lax");
      var cleared = new MockHttpServletResponse();
      if (admin) service.clearAdminCookie(cleared);
      else service.clearUserCookie(cleared);
      assertThat(cleared.getCookie(name).getPath()).isEqualTo(path);
      assertThat(cleared.getCookie(name).getMaxAge()).isZero();
      assertThat(cleared.getCookie(name).getValue()).isEmpty();
      assertThat(cleared.getHeader("Set-Cookie")).contains("Secure", "HttpOnly", "SameSite=Lax");
   }
}

package org.bublapi.dent.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.auth.security.ApiKeyFilter;
import org.bublapi.dent.auth.security.OriginPolicy;
import org.bublapi.dent.auth.security.RefreshOriginFilter;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.logging.security.SecurityLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecurityFilterTest {
   final ApiKeyService keys = mock(ApiKeyService.class);
   final SecurityLogService logs = mock(SecurityLogService.class);
   final ApiKeyFilter filter = new ApiKeyFilter(keys, logs);

   @AfterEach
   void cleanUp() {
      ClinicContext.clear();
      MDC.clear();
   }

   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void clinicContextAndLoggingContextAreClearedEvenWhenDownstreamThrows(boolean failure) throws Exception {
      Clinic clinic = new Clinic();
      clinic.setId(UUID.randomUUID());
      ApiKey key = new ApiKey();
      key.setClinic(clinic);
      when(keys.validate("key")).thenReturn(key);
      var request = new MockHttpServletRequest("GET", "/api/v1/public/doctors");
      request.addHeader("X-API-KEY", "key");
      FilterChain chain = (req, res) -> {
         assertThat(ClinicContext.getClinicId()).isEqualTo(clinic.getId());
         assertThat(MDC.get("clinicId")).isEqualTo(clinic.getId().toString());
         if (failure) throw new ServletException("downstream failure");
      };
      if (failure)
         assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), chain)).isInstanceOf(
                 ServletException.class);
      else filter.doFilter(request, new MockHttpServletResponse(), chain);
      assertThat(ClinicContext.get()).isNull();
      assertThat(MDC.get("clinicId")).isNull();
   }

   @ParameterizedTest
   @ValueSource(strings = {"/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/admin/auth/refresh",
                           "/api/v1/admin/auth/logout"})
   void cookieMutationsRequireAllowedOrigin(String path) throws Exception {
      OriginPolicy policy = mock(OriginPolicy.class);
      RefreshOriginFilter originFilter = new RefreshOriginFilter(policy);
      var request = new MockHttpServletRequest("POST", path);
      var response = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);
      originFilter.doFilter(request, response, chain);
      assertThat(response.getStatus()).isEqualTo(403);
      verifyNoInteractions(chain);
      when(policy.allows(any())).thenReturn(true);
      originFilter.doFilter(new MockHttpServletRequest("POST", path), new MockHttpServletResponse(), chain);
      verify(chain).doFilter(any(), any());
   }

   @Test
   void loginDoesNotRequireCookieOriginGuard() throws Exception {
      OriginPolicy policy = mock(OriginPolicy.class);
      FilterChain chain = mock(FilterChain.class);
      new RefreshOriginFilter(policy).doFilter(new MockHttpServletRequest("POST", "/api/v1/auth/login"),
                                               new MockHttpServletResponse(), chain);
      verifyNoInteractions(policy);
      verify(chain).doFilter(any(), any());
   }
}

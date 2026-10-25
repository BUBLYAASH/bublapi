package org.bublapi.dent.auth;

import org.bublapi.dent.auth.security.OriginPolicy;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicAllowedOriginRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OriginPolicyTest {
   final ClinicAllowedOriginRepository origins = mock(ClinicAllowedOriginRepository.class);
   final ApiKeyService keys = mock(ApiKeyService.class);
   final OriginPolicy policy = new OriginPolicy(origins, keys, "https://admin.example");

   MockHttpServletRequest request(String path, String origin) {
      var req = new MockHttpServletRequest("POST", path);
      req.addHeader("Origin", origin);
      return req;
   }
   @Test void scopesActualRequestToKeyClinic() {
      var clinic = new Clinic(); clinic.setId(UUID.randomUUID());
      var key = new ApiKey(); key.setClinic(clinic);
      when(keys.validate("key-a")).thenReturn(key);
      when(origins.existsByClinic_IdAndClinic_ActiveTrueAndOrigin(clinic.getId(), "https://a.example")).thenReturn(true);
      var req = request("/api/v1/auth/login", "https://a.example"); req.addHeader("X-API-KEY", "key-a");
      assertTrue(policy.allows(req));
      req.removeHeader("Origin"); req.addHeader("Origin", "https://b.example");
      assertFalse(policy.allows(req));
   }
   @Test void preflightDoesNotRequireKeyAndDoesNotApproveUnknownOrigin() {
      var req = request("/api/v1/auth/login", "https://a.example"); req.setMethod("OPTIONS");
      req.addHeader("Access-Control-Request-Method", "POST");
      when(origins.existsByClinic_ActiveTrueAndOrigin("https://a.example")).thenReturn(true);
      assertTrue(policy.allows(req)); verifyNoInteractions(keys);
      req.removeHeader("Origin"); req.addHeader("Origin", "https://unknown.example");
      assertFalse(policy.allows(req));
   }
   @Test void adminOriginsAreSeparateFromClinicOrigins() {
      assertTrue(policy.allows(request("/api/v1/admin/auth/refresh", "https://admin.example")));
      assertFalse(policy.allows(request("/api/v1/admin/auth/login", "https://a.example")));
      assertFalse(policy.allows(request("/api/v1/auth/refresh", "https://admin.example")));
      assertFalse(policy.allows(request("/api/v1/auth/logout", "null")));
   }
}

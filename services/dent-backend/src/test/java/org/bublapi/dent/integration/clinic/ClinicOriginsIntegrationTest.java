package org.bublapi.dent.integration.clinic;

import org.bublapi.dent.clinic.service.ClinicOriginService;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicOriginsIntegrationTest extends IntegrationTestBase {
   @Autowired
   TestDataFactory data;
   @Autowired
   TestJwtHelper jwt;
   @Autowired
   ClinicOriginService origins;

   @Test
   void createsInitialOriginAndManagesSites() throws Exception {
      String token = jwt.token(data.createAdmin("admin@origins.test").getId());
      String result = mockMvc.perform(post("/api/v1/admin/clinics").header("Authorization", token)
                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                   .content("""
                                                                                    {"title":"Origins clinic","address":"Moscow","timezone":"Europe/Moscow","website":"https://clinic.test/booking"}
                                                                                    """))
                             .andExpect(status().isOk())
                             .andReturn()
                             .getResponse()
                             .getContentAsString();
      String id = objectMapper.readTree(result).get("id").asText();
      String base = "/api/v1/admin/clinics/" + id + "/origins";
      mockMvc.perform(get(base).header("Authorization", token))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$[0].origin").value("https://clinic.test"));
      String added = mockMvc.perform(post(base).header("Authorization", token)
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content("{\"origin\":\"https://booking.clinic.test\"}"))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
      String oid = objectMapper.readTree(added).get("id").asText();
      mockMvc.perform(post(base).header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"origin\":\"https://booking.clinic.test\"}"))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value(oid));
      mockMvc.perform(post(base).header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"origin\":\"https://evil.test/path\"}")).andExpect(status().isBadRequest());
      mockMvc.perform(delete(base + "/" + oid).header("Authorization", token)).andExpect(status().isNoContent());
   }

   @Test
   void allNonAdminRolesCannotManageClinicsOrOrigins() throws Exception {
      var clinic = data.createClinic();
      for (RoleName role : new RoleName[]{RoleName.OWNER, RoleName.DOCTOR, RoleName.RECEPTIONIST, RoleName.PATIENT}) {
         String token = jwt.token(data.createUserWithRoles(clinic, role + "@origins.test", role).getId());
         String base = "/api/v1/admin/clinics/" + clinic.getId();
         mockMvc.perform(get("/api/v1/admin/clinics").header("Authorization", token)).andExpect(status().isForbidden());
         mockMvc.perform(post("/api/v1/admin/clinics").header("Authorization", token)
                                                      .contentType(MediaType.APPLICATION_JSON)
                                                      .content("{}")).andExpect(status().isForbidden());
         mockMvc.perform(
                        patch(base).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
         mockMvc.perform(get(base + "/origins").header("Authorization", token)).andExpect(status().isForbidden());
         mockMvc.perform(post(base + "/origins").header("Authorization", token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"origin\":\"https://clinic.test\"}"))
                .andExpect(status().isForbidden());
         mockMvc.perform(delete(base + "/origins/" + java.util.UUID.randomUUID()).header("Authorization", token))
                .andExpect(status().isForbidden());
      }
   }

   @Test
   void corsAndRefreshEnforceClinicOwnershipAndRevocation() throws Exception {
      var a = data.createClinic();
      var b = data.createClinic();
      String keyA = data.createApiKey(a).rawKey();
      var site = origins.add(a.getId(), "https://a.clinic.test");
      origins.add(b.getId(), "https://b.clinic.test");
      mockMvc.perform(options("/api/v1/auth/refresh").header("Origin", "https://a.clinic.test")
                                                     .header("Access-Control-Request-Method", "POST")
                                                     .header("Access-Control-Request-Headers", "X-API-KEY"))
             .andExpect(status().isOk())
             .andExpect(header().string("Access-Control-Allow-Origin", "https://a.clinic.test"));
      mockMvc.perform(post("/api/v1/auth/refresh").header("Origin", "https://a.clinic.test").header("X-API-KEY", keyA))
             .andExpect(status().isUnauthorized());
      mockMvc.perform(post("/api/v1/auth/refresh").header("Origin", "https://b.clinic.test").header("X-API-KEY", keyA))
             .andExpect(status().isForbidden());
      origins.remove(a.getId(), site.id());
      mockMvc.perform(post("/api/v1/auth/refresh").header("Origin", "https://a.clinic.test").header("X-API-KEY", keyA))
             .andExpect(status().isForbidden());
   }

   @Test
   void loginRefreshLogoutWorkForClinicAndAdminOrigins() throws Exception {
      var clinic = data.createClinic();
      origins.add(clinic.getId(), "https://clinic.test");
      String key = data.createApiKey(clinic).rawKey();
      data.createUser(clinic, "patient@clinic.test");
      data.createAdmin("admin@clinic.test");
      for (boolean admin : new boolean[]{false, true}) {
         String base = admin ? "/api/v1/admin/auth" : "/api/v1/auth";
         String origin = admin ? "http://localhost:3001" : "https://clinic.test";
         String email = admin ? "admin@clinic.test" : "patient@clinic.test";
         var login = mockMvc.perform(post(base + "/login").header("Origin", origin)
                                                          .header("X-API-KEY", key)
                                                          .contentType(MediaType.APPLICATION_JSON)
                                                          .content("{\"email\":\"" + email
                                                                           + "\",\"password\":\"password123\"}"))
                            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                            .andReturn().getResponse();
         String cookieName = admin ? "admin_refresh_token" : "refresh_token";
         var cookie = login.getCookie(cookieName);
         org.junit.jupiter.api.Assertions.assertNotNull(cookie);
         var refresh = mockMvc.perform(post(base + "/refresh").header("Origin", origin).header("X-API-KEY", key)
                                                              .cookie(cookie))
                              .andExpect(status().isOk())
                              .andReturn()
                              .getResponse();
         mockMvc.perform(post(base + "/logout").header("Origin", origin).header("X-API-KEY", key)
                                               .cookie(refresh.getCookie(cookieName)))
                .andExpect(status().isNoContent());
         mockMvc.perform(post(base + "/refresh").header("X-API-KEY", key).cookie(cookie))
                .andExpect(status().isForbidden());
      }
   }

}

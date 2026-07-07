package org.bublapi.dent.integration.clinic;

import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicIntegrationTest extends IntegrationTestBase {

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;


   @Test
   void shouldCreateClinicSuccessfully() throws Exception {

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());


      CreateClinicRequestDto request = new CreateClinicRequestDto("Dental Clinic", "Best clinic", "Moscow, Red Square 1", "79991234567", "clinic@mail.com", "https://clinic.com", "Europe/Moscow");


      mockMvc.perform(post("/api/admin/clinics").header("Authorization", token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.title").value("Dental Clinic"))
             .andExpect(jsonPath("$.address").value("Moscow, Red Square 1"));
   }


   @Test
   void shouldNotAllowDuplicateClinicTitleAndAddress() throws Exception {

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());


      CreateClinicRequestDto request = new CreateClinicRequestDto("Dental Clinic", null, "Same address", "79991234567", "clinic1@mail.com", null, "Europe/Moscow");


      mockMvc.perform(post("/api/admin/clinics").header("Authorization", token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk());


      mockMvc.perform(post("/api/admin/clinics").header("Authorization", token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isConflict());
   }


   @Test
   void shouldPersistClinicAsActive() throws Exception {

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());


      CreateClinicRequestDto request = new CreateClinicRequestDto("Active Clinic", null, "Some address", "79990000000", "active@mail.com", null, "Europe/Moscow");


      mockMvc.perform(post("/api/admin/clinics").header("Authorization", token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.active").value(true));
   }
}
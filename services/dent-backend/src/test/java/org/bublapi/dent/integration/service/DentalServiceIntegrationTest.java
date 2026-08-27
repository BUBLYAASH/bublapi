package org.bublapi.dent.integration.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.dental_service.dto.CreateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.entity.ServiceCategory;
import org.bublapi.dent.dental_service.repository.DentalServiceRepository;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class DentalServiceIntegrationTest extends IntegrationTestBase {

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Autowired
   private DentalServiceRepository dentalServiceRepository;

   private CreateDentalServiceRequestDto createRequest() {
      return new CreateDentalServiceRequestDto("Лечение кариеса", "Терапевтическое лечение зубов",
                                               ServiceCategory.THERAPY, 30);
   }

   @Test
   void shouldCreateDentalService() throws Exception {
      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      mockMvc.perform(post("/api/admin/catalog/dental-services").header("Authorization", token)
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(objectMapper.writeValueAsString(
                                                                        createRequest())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.title").value("Лечение кариеса"));
   }

   @Test
   void shouldHaveDefaultCategoryTherapy() throws Exception {
      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      mockMvc.perform(post("/api/admin/catalog/dental-services").header("Authorization", token)
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(objectMapper.writeValueAsString(
                                                                        createRequest())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.category").value("THERAPY"));
   }

   @Test
   void shouldHaveDefaultDuration30() throws Exception {
      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      mockMvc.perform(post("/api/admin/catalog/dental-services").header("Authorization", token)
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(objectMapper.writeValueAsString(
                                                                        createRequest())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.defaultDurationMinutes").value(30));
   }

   @Test
   void shouldPersistActiveService() throws Exception {
      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      mockMvc.perform(post("/api/admin/catalog/dental-services").header("Authorization", token)
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(objectMapper.writeValueAsString(
                                                                        createRequest())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()));

      assertThat(dentalServiceRepository.findAll().isEmpty()).isFalse();
   }

   @Test
   void staff_shouldSeeActiveServicesOnly() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User receptionist = dataFactory.createUserWithRoles(clinic, "receptionist@mail.com", RoleName.RECEPTIONIST);

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(get("/api/catalog/dental-services").header("Authorization", jwtHelper.token(receptionist.getId()))
                                                         .header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$").isArray());
   }

   @Test
   void staff_shouldNotCreateDentalService() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User receptionist = dataFactory.createUserWithRoles(clinic, "receptionist@mail.com", RoleName.RECEPTIONIST);

      CreateDentalServiceRequestDto request = new CreateDentalServiceRequestDto("Удаление зуба", null,
                                                                                ServiceCategory.SURGERY, 90);

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(
                     post("/api/admin/catalog/dental-services").header("Authorization", jwtHelper.token(receptionist.getId()))
                                                               .header("X-API-KEY", apiKey)
                                                               .contentType(MediaType.APPLICATION_JSON)
                                                               .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isForbidden());

   }
}
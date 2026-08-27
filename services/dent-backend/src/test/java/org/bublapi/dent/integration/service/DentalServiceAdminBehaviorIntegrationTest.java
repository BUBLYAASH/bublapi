package org.bublapi.dent.integration.service;

import org.bublapi.dent.dental_service.dto.CreateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.dto.UpdateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.dental_service.entity.ServiceCategory;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DentalServiceAdminBehaviorIntegrationTest extends IntegrationTestSupport {

   private static final String ADMIN_DENTAL_SERVICES_URL = "/api/admin/catalog/dental-services";

   @Test
   void shouldNotCreateDuplicateDentalServiceTitle() throws Exception {
      User admin = dataFactory.createAdmin("admin-" + UUID.randomUUID() + "@test.com");
      String title = "Unique Service " + UUID.randomUUID();
      CreateDentalServiceRequestDto request = new CreateDentalServiceRequestDto(title, "desc", ServiceCategory.THERAPY,
                                                                                30);

      mockMvc.perform(post(ADMIN_DENTAL_SERVICES_URL)
                              .header("Authorization", jwtHelper.token(admin.getId()))
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk());

      mockMvc.perform(post(ADMIN_DENTAL_SERVICES_URL)
                              .header("Authorization", jwtHelper.token(admin.getId()))
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.message").value("Service with this title already exists"));
   }

   @Test
   void shouldUpdateDentalService() throws Exception {
      User admin = dataFactory.createAdmin("admin-" + UUID.randomUUID() + "@test.com");
      DentalService service = dataFactory.createDentalService();
      UpdateDentalServiceRequestDto request = new UpdateDentalServiceRequestDto("Updated service",
                                                                                "Updated description",
                                                                                ServiceCategory.SURGERY, 45);

      mockMvc.perform(patch(ADMIN_DENTAL_SERVICES_URL + "/{dentalServiceId}", service.getId())
                              .header("Authorization", jwtHelper.token(admin.getId()))
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.title").value("Updated service"))
             .andExpect(jsonPath("$.defaultDurationMinutes").value(45));
   }

   @Test
   void shouldDeactivateDentalService() throws Exception {
      User admin = dataFactory.createAdmin("admin-" + UUID.randomUUID() + "@test.com");
      DentalService service = dataFactory.createDentalService();

      mockMvc.perform(patch(ADMIN_DENTAL_SERVICES_URL + "/{dentalServiceId}/deactivation", service.getId())
                              .header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value(service.getId().toString()));

      mockMvc.perform(get(ADMIN_DENTAL_SERVICES_URL + "/{dentalServiceId}", service.getId())
                              .header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isNotFound());
   }

   @Test
   void staffShouldSeeOnlyActiveDentalServices() throws Exception {
      TestClinicData data = createClinicData(org.bublapi.dent.role.entity.RoleName.OWNER);
      DentalService active = dataFactory.createDentalService();
      DentalService inactive = dataFactory.createDentalService();
      User admin = dataFactory.createAdmin("admin-" + UUID.randomUUID() + "@test.com");

      mockMvc.perform(patch(ADMIN_DENTAL_SERVICES_URL + "/{dentalServiceId}/deactivation", inactive.getId())
                              .header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());

      mockMvc.perform(get("/api/catalog/dental-services")
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(active.getId().toString()));
   }

   @Test
   void adminShouldSeeInactiveDentalServicesToo() throws Exception {
      User admin = dataFactory.createAdmin("admin-" + UUID.randomUUID() + "@test.com");
      DentalService active = dataFactory.createDentalService();
      DentalService inactive = dataFactory.createDentalService();

      mockMvc.perform(patch(ADMIN_DENTAL_SERVICES_URL + "/{dentalServiceId}/deactivation", inactive.getId())
                              .header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());

      mockMvc.perform(get(ADMIN_DENTAL_SERVICES_URL)
                              .header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(2)))
             .andExpect(jsonPath("$[0].id", notNullValue()))
             .andExpect(jsonPath("$[1].id", notNullValue()));
   }
}

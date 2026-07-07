package org.bublapi.dent.integration.service;

import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.dto.AddClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class ClinicServiceIntegrationTest extends IntegrationTestBase {

   private static final String STAFF_SERVICES_URL = "/api/services";
   private static final String PUBLIC_SERVICES_URL = "/api/public/services";

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Autowired
   private ClinicServiceRepository clinicServiceRepository;


   private AddClinicServiceRequestDto createRequest() {
      return new AddClinicServiceRequestDto(1_500, 45);
   }


   private <T> T inClinicContext(Clinic clinic, Supplier<T> action) {
      ClinicContext.set(clinic);
      try {
         return action.get();
      } finally {
         ClinicContext.clear();
      }
   }

   @Test
   void shouldCreateClinicService() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User owner = dataFactory.createUserWithRoles(clinic, "owner@test.com", RoleName.OWNER);

      DentalService dentalService = dataFactory.createDentalService();

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(post(STAFF_SERVICES_URL + "/{dentalServiceId}", dentalService.getId()).header("Authorization", jwtHelper.token(owner.getId()))
                                                                                            .header("X-API-KEY", apiKey.rawKey())
                                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                                            .content(objectMapper.writeValueAsString(createRequest())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.clinicId").value(clinic.getId().toString()))
             .andExpect(jsonPath("$.dentalServiceId").value(dentalService.getId().toString()))
             .andExpect(jsonPath("$.price").value(1_500))
             .andExpect(jsonPath("$.durationMinutes").value(45));

      List<ClinicService> savedServices = inClinicContext(clinic, clinicServiceRepository::findAll);
      assertThat(savedServices).hasSize(1);
   }


   @Test
   void shouldCreateInactiveClinicService() {
      Clinic clinic = dataFactory.createClinic();

      ClinicService clinicService = dataFactory.createInactiveClinicService(clinic);

      assertThat(clinicService.getId()).isNotNull();
      assertThat(clinicService.getClinic().getId()).isEqualTo(clinic.getId());
      assertThat(clinicService.getActive()).isFalse();
   }


   @Test
   void shouldLinkClinicAndDentalService() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User receptionist = dataFactory.createUserWithRoles(clinic, "receptionist@test.com", RoleName.RECEPTIONIST);

      DentalService dentalService = dataFactory.createDentalService();

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(post(STAFF_SERVICES_URL + "/{dentalServiceId}", dentalService.getId()).header("Authorization", jwtHelper.token(receptionist.getId()))
                                                                                            .header("X-API-KEY", apiKey.rawKey())
                                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                                            .content(objectMapper.writeValueAsString(createRequest())))
             .andExpect(status().isOk());

      ClinicService savedClinicService = inClinicContext(clinic, () -> clinicServiceRepository.findAll().getFirst());
      assertThat(savedClinicService.getClinic().getId()).isEqualTo(clinic.getId());
      assertThat(savedClinicService.getDentalService().getId()).isEqualTo(dentalService.getId());
   }


   @Test
   void shouldHaveCorrectPriceAndDuration() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User owner = dataFactory.createUserWithRoles(clinic, "owner@test.com", RoleName.OWNER);

      DentalService dentalService = dataFactory.createDentalService();

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      AddClinicServiceRequestDto request = new AddClinicServiceRequestDto(2_750, 75);

      mockMvc.perform(post(STAFF_SERVICES_URL + "/{dentalServiceId}", dentalService.getId()).header("Authorization", jwtHelper.token(owner.getId()))
                                                                                            .header("X-API-KEY", apiKey.rawKey())
                                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                                            .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.price").value(2_750))
             .andExpect(jsonPath("$.durationMinutes").value(75));
   }


   @Test
   void shouldNotAllowDuplicateClinicService() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User owner = dataFactory.createUserWithRoles(clinic, "owner@test.com", RoleName.OWNER);

      DentalService dentalService = dataFactory.createDentalService();

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(post(STAFF_SERVICES_URL + "/{dentalServiceId}", dentalService.getId()).header("Authorization", jwtHelper.token(owner.getId()))
                                                                                            .header("X-API-KEY", apiKey.rawKey())
                                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                                            .content(objectMapper.writeValueAsString(createRequest())))
             .andExpect(status().isOk());

      mockMvc.perform(post(STAFF_SERVICES_URL + "/{dentalServiceId}", dentalService.getId()).header("Authorization", jwtHelper.token(owner.getId()))
                                                                                            .header("X-API-KEY", apiKey.rawKey())
                                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                                            .content(objectMapper.writeValueAsString(createRequest())))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.message").value("Dental Service is already in this clinic"));
   }


   @Test
   void shouldDeactivateClinicService() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User owner = dataFactory.createUserWithRoles(clinic, "owner@test.com", RoleName.OWNER);

      ClinicService clinicService = dataFactory.createClinicService(clinic);

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(patch(STAFF_SERVICES_URL + "/{clinicServiceId}/deactivation", clinicService.getId()).header("Authorization", jwtHelper.token(owner.getId()))
                                                                                                          .header("X-API-KEY", apiKey.rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value(clinicService.getId().toString()));

      ClinicService updatedService = inClinicContext(clinic, () -> clinicServiceRepository.findById(clinicService.getId())
                                                                                          .orElseThrow());
      assertThat(updatedService.getActive()).isFalse();
   }


   @Test
   void shouldNotReturnInactiveServiceForPublic() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      ClinicService activeService = dataFactory.createClinicService(clinic);
      ClinicService inactiveService = dataFactory.createInactiveClinicService(clinic);

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(get(PUBLIC_SERVICES_URL).header("X-API-KEY", apiKey.rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$").isArray())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(activeService.getId().toString()));

      mockMvc.perform(get(PUBLIC_SERVICES_URL + "/{clinicServiceId}", inactiveService.getId()).header("X-API-KEY", apiKey.rawKey()))
             .andExpect(status().isNotFound());
   }
}
package org.bublapi.dent.integration.service;

import org.bublapi.dent.clinic_service.dto.UpdateClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicServiceExtraBehaviorIntegrationTest extends IntegrationTestSupport {

   @Autowired
   private ClinicServiceRepository clinicServiceRepository;

   @Test
   void shouldReactivateClinicService() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      ClinicService service = dataFactory.createInactiveClinicService(data.clinic());

      mockMvc.perform(patch("/api/services/{clinicServiceId}/activation", service.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value(service.getId().toString()));

      ClinicContext.set(data.clinic());
      try {
         ClinicService updated = clinicServiceRepository.findById(service.getId()).orElseThrow();
         assertThat(updated.getActive()).isTrue();
      } finally {
         ClinicContext.clear();
      }
   }

   @Test
   void shouldUpdateClinicServicePriceAndDuration() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      ClinicService service = dataFactory.createClinicService(data.clinic());
      UpdateClinicServiceRequestDto request = new UpdateClinicServiceRequestDto(2_500, 60);

      mockMvc.perform(patch("/api/services/{clinicServiceId}", service.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.price").value(2_500))
             .andExpect(jsonPath("$.durationMinutes").value(60));
   }

   @Test
   void shouldNotCreateAppointmentWithInactiveClinicService() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      ClinicService inactiveService = dataFactory.createInactiveClinicService(context.clinic());
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createStaffAppointment(context, scheduledAt, services(
              new org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto(inactiveService.getId(), 1)))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.message").value("Clinic service not found"));
   }

   @Test
   void publicShouldSeeReactivatedClinicService() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      ClinicService service = dataFactory.createInactiveClinicService(data.clinic());

      mockMvc.perform(get("/api/public/services")
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));

      mockMvc.perform(patch("/api/services/{clinicServiceId}/activation", service.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk());

      mockMvc.perform(get("/api/public/services")
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(service.getId().toString()));
   }
}

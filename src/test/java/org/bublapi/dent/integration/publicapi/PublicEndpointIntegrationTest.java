package org.bublapi.dent.integration.publicapi;

import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicEndpointIntegrationTest extends IntegrationTestSupport {

   @Test
   void publicShouldSeeOnlyActiveDoctors() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      Doctor activeDoctor = dataFactory.createDoctor(data.clinic());
      dataFactory.createInactiveDoctor(data.clinic());

      mockMvc.perform(get("/api/public/doctors")
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(activeDoctor.getId().toString()));
   }

   @Test
   void publicShouldNotSeeInactiveDoctors() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      dataFactory.createInactiveDoctor(data.clinic());

      mockMvc.perform(get("/api/public/doctors")
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));
   }

   @Test
   void publicShouldSeeOnlyActiveClinicServices() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      ClinicService activeService = dataFactory.createClinicService(data.clinic());
      dataFactory.createInactiveClinicService(data.clinic());

      mockMvc.perform(get("/api/public/services")
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(activeService.getId().toString()));
   }

   @Test
   void publicShouldNotSeeInactiveClinicServices() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      ClinicService inactiveService = dataFactory.createInactiveClinicService(data.clinic());

      mockMvc.perform(get("/api/public/services/{clinicServiceId}", inactiveService.getId())
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isNotFound());
   }

   @Test
   void publicShouldGetDoctorWorkingHours() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      addRegularWorkingHours(context.doctor(), LocalDate.now().plusDays(7), LocalTime.of(9, 0), LocalTime.of(18, 0));

      mockMvc.perform(get("/api/public/doctors/{doctorId}/working-hours", context.doctor().getId())
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].doctorId").value(context.doctor().getId().toString()));
   }
}

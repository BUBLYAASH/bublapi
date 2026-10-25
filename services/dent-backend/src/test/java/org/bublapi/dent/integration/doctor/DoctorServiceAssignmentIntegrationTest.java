package org.bublapi.dent.integration.doctor;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorServiceAssignmentIntegrationTest extends IntegrationTestSupport {
   @Test
   void assignmentIsVisibleFromBothPublicEndpointsAndRemovalIsConsistent() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      String key = context.apiKey().rawKey();
      String token = jwtHelper.token(context.user().getId());
      String link = "/api/v1/doctors/" + context.doctor().getId() + "/services/" + context.clinicService().getId();
      String doctorServices = "/api/v1/public/doctors/" + context.doctor().getId() + "/services";
      String serviceDoctors = "/api/v1/public/services/" + context.clinicService().getId() + "/doctors";
      mockMvc.perform(post(link).header("Authorization", token).header("X-API-KEY", key)).andExpect(status().isOk())
             .andExpect(jsonPath("$.doctorId").value(context.doctor().getId().toString()))
             .andExpect(jsonPath("$.clinicServiceId").value(context.clinicService().getId().toString()));
      mockMvc.perform(post(link).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isBadRequest());
      mockMvc.perform(get(doctorServices).header("X-API-KEY", key))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)));
      mockMvc.perform(get(serviceDoctors).header("X-API-KEY", key))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)));
      mockMvc.perform(delete(link).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNoContent());
      mockMvc.perform(get(doctorServices).header("X-API-KEY", key))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));
      mockMvc.perform(get(serviceDoctors).header("X-API-KEY", key))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));
   }

   @Test
   void foreignClinicServiceCannotBeAssignedToLocalDoctor() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      var foreign = dataFactory.createClinicService(dataFactory.createClinic());
      mockMvc.perform(post("/api/v1/doctors/" + context.doctor().getId() + "/services/" + foreign.getId())
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isNotFound());
      mockMvc.perform(get("/api/v1/public/doctors/" + context.doctor().getId() + "/services")
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));
   }
}

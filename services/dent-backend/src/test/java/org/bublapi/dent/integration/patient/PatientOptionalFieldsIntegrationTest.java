package org.bublapi.dent.integration.patient;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientOptionalFieldsIntegrationTest extends IntegrationTestSupport {
   @Test
   void patientCanBeCreatedWithoutOptionalEmail() throws Exception {
      var data = createClinicData(RoleName.RECEPTIONIST);
      mockMvc.perform(post("/api/v1/patients").header("Authorization", jwtHelper.token(data.user().getId()))
                                              .header("X-API-KEY", data.apiKey().rawKey())
                                              .contentType("application/json")
                                              .content(
                                                      "{\"firstName\":\"Test\",\"lastName\":\"Patient\",\"phone\":\"79991234567\"}"))
             .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNotEmpty());
   }
   @Test
   void missingEmailStillAllowsLinkingPatientToUserByPhone() throws Exception {
      var data = createClinicData(RoleName.RECEPTIONIST);
      var user = dataFactory.createUserWithPhone(data.clinic(), "patient@optional.test", "79991234567");
      mockMvc.perform(post("/api/v1/patients").header("Authorization", jwtHelper.token(data.user().getId()))
              .header("X-API-KEY", data.apiKey().rawKey()).contentType("application/json")
              .content("{\"firstName\":\"Test\",\"lastName\":\"Patient\",\"phone\":\"79991234567\"}"))
              .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(user.getId().toString()))
              .andExpect(jsonPath("$.email").value(nullValue()));
   }

}

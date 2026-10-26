package org.bublapi.dent.integration.service;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicServiceNotificationIntegrationTest extends IntegrationTestSupport {
   @Test
   void serviceCanBeDeactivatedWhenUpcomingPatientHasNoUserAccount() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      var at = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      createStaffAppointment(context, at).andExpect(status().isOk());
      mockMvc.perform(patch("/api/v1/services/" + context.clinicService().getId() + "/deactivation")
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
      verifyNoInteractions(notificationPublisher);
   }

   @Test
   void unlinkedPatientDoesNotPreventNotificationForLinkedPatient() throws Exception {
      var context = createPatientAppointmentContext();
      var at = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      createPatientAppointment(context, at).andExpect(status().isOk());
      var owner = dataFactory.createUserWithRoles(context.clinic(), "owner@service-notification.test", RoleName.OWNER);
      var unlinkedPatient = dataFactory.createPatient(context.clinic());
      var staffContext = new AppointmentContext(context.clinic(), owner, unlinkedPatient, context.doctor(),
                                                context.clinicService(), context.apiKey());
      createStaffAppointment(staffContext, at.plusHours(1)).andExpect(status().isOk());
      clearInvocations(notificationPublisher);
      mockMvc.perform(patch("/api/v1/services/" + context.clinicService().getId() + "/deactivation")
                              .header("Authorization", jwtHelper.token(owner.getId()))
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
      verify(notificationPublisher).publishAfterCommit(argThat(command ->
                                                                       command.userId()
                                                                              .equals(context.user()
                                                                                             .getId()) && command.type() == NotificationType.CLINIC_SERVICE_DEACTIVATED));
   }

}

package org.bublapi.dent.integration.user;

import jakarta.persistence.EntityManager;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.service.RefreshTokenService;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.UserNotificationData;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.service.NotificationQueueService;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.dto.UserErasureRequestDto;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserErasureIntegrationTest extends IntegrationTestSupport {
   @Autowired
   EntityManager entityManager;
   @Autowired
   TransactionTemplate transaction;
   @Autowired
   AppointmentRepository appointments;
   @Autowired
   RefreshTokenService refreshTokens;
   @Autowired
   NotificationQueueService queue;

   @Test
   void deletesIdentityAndPlannedAppointmentsAndPreservesUnlinkedHistory() throws Exception {
      var clinic = dataFactory.createClinic();
      var user = dataFactory.createUser(clinic, "erase@test.com");
      var history = appointment(user, AppointmentStatus.COMPLETED, -3);
      UUID patientId = history.getPatient().getId();
      var future = new Appointment();
      future.setClinic(clinic);
      future.setPatient(history.getPatient());
      future.setDoctor(history.getDoctor());
      future.setScheduledAt(history.getScheduledAt().plusDays(10));
      future.setEndAt(future.getScheduledAt().plusMinutes(30));
      future.setTotalPrice(1000);
      future.setStatus(AppointmentStatus.CONFIRMED);
      future = appointments.saveAndFlush(future);
      refreshTokens.create(user, RefreshTokenType.USER);
      queue.prepare(new CreateNotificationCommand(clinic.getId(), user.getId(), history.getId(),
                                                  NotificationType.USER_REGISTERED,
                                                  new UserNotificationData("Clinic", "Private name"),
                                                  LocalDateTime.now()));
      var other = dataFactory.createUser(clinic, "other@test.com");
      String oldAccess = jwtHelper.token(user.getId());
      var key = dataFactory.createApiKey(clinic).rawKey();
      mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", user.getId()).header("Authorization",
                                                                                    jwtHelper.token(admin().getId()))
                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                            .content(body(user)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.deletedAppointments").value(1))
             .andExpect(jsonPath("$.anonymizedAppointments").value(1));
      assertThat(query("SELECT count(*) FROM users WHERE id = ?", Integer.class, user.getId())).isZero();
      assertThat(query("SELECT count(*) FROM patients WHERE id = ?", Integer.class, patientId)).isZero();
      assertThat(appointments.existsById(future.getId())).isFalse();
      var saved = findAppointment(history);
      assertThat(saved.getPatient()).isNull();
      assertThat(saved.getComment()).isNull();
      assertThat(saved.getDoctor().getId()).isEqualTo(history.getDoctor().getId());
      assertThat(saved.getTotalPrice()).isEqualTo(1000);
      assertThat(
              query("SELECT count(*) FROM appointment_services WHERE appointment_id = ?", Integer.class,
                    history.getId())).isEqualTo(1);
      for (String table : new String[]{"refresh_tokens", "user_roles", "notifications"}) {
         assertThat(query("SELECT count(*) FROM " + table + " WHERE user_id = ?", Integer.class,
                          user.getId())).isZero();
      }
      assertThat(
              query("SELECT count(*) FROM users WHERE id = ?", Integer.class, other.getId())).isEqualTo(
              1);
      mockMvc.perform(get("/api/v1/profile").header("Authorization", oldAccess).header("X-API-KEY", key))
             .andExpect(status().isUnauthorized());
      var staff = dataFactory.createUserWithRoles(clinic, "staff@test.com", RoleName.OWNER);
      mockMvc.perform(
                     get("/api/v1/appointments/{id}", history.getId()).header("Authorization", jwtHelper.token(staff.getId()))
                                                                      .header("X-API-KEY", key))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.patientId").doesNotExist())
             .andExpect(jsonPath("$.comment").doesNotExist());
   }

   @ParameterizedTest
   @EnumSource(value = RoleName.class, names = {"PATIENT", "OWNER", "DOCTOR"})
   void nonAdminCannotErase(RoleName role) throws Exception {
      var clinic = dataFactory.createClinic();
      var user = dataFactory.createUserWithRoles(clinic, "user@test.com", role);
      mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", user.getId()).header("Authorization",
                                                                                    jwtHelper.token(user.getId()))
                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                            .content(body(user)))
             .andExpect(status().isForbidden());
      assertThat(query("SELECT count(*) FROM users WHERE id = ?", Integer.class, user.getId())).isEqualTo(
              1);
   }

   @Test
   void rejectsMissingConfirmationWrongClinicAndPlatformAdmin() throws Exception {
      var user = dataFactory.createUser(dataFactory.createClinic(), "user@test.com");
      String token = jwtHelper.token(admin().getId());
      for (String invalid : new String[]{"{}", body(user).replace("true", "false"),
                                         body(user).replace(user.getId().toString(), UUID.randomUUID().toString())}) {
         mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", user.getId()).header("Authorization", token)
                                                                               .contentType(MediaType.APPLICATION_JSON)
                                                                               .content(invalid))
                .andExpect(status().isBadRequest());
      }
      mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", user.getId()).header("Authorization", token)
                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                            .content(body(user).replace(
                                                                                    user.getClinic().getId().toString(),
                                                                                    UUID.randomUUID().toString())))
             .andExpect(status().isNotFound());
      var platformAdmin = admin();
      mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", platformAdmin.getId()).header("Authorization", token)
                                                                                     .contentType(
                                                                                             MediaType.APPLICATION_JSON)
                                                                                     .content(body(user).replace(
                                                                                             user.getId().toString(),
                                                                                             platformAdmin.getId()
                                                                                                          .toString())))
             .andExpect(status().isNotFound());
   }

   @Test
   void doctorProfileIsProtectedAndDeletionFailureRollsBack() throws Exception {
      var user = dataFactory.createUser(dataFactory.createClinic(), "user@test.com");
      var doctor = dataFactory.createDoctor(user.getClinic());
      update("UPDATE doctors SET user_id = ? WHERE id = ?", user.getId(), doctor.getId());
      String token = jwtHelper.token(admin().getId());
      mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", user.getId()).header("Authorization", token)
                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                            .content(body(user)))
             .andExpect(status().isBadRequest());
      update("UPDATE doctors SET user_id = NULL WHERE id = ?", doctor.getId());
      var a = appointment(user, AppointmentStatus.COMPLETED, -1);
      update("CREATE TABLE erasure_test_hold(user_id UUID REFERENCES users(id))");
      try {
         update("INSERT INTO erasure_test_hold VALUES (?)", user.getId());
         mockMvc.perform(post("/api/v1/admin/users/{id}/erasure", user.getId()).header("Authorization", token)
                                                                               .contentType(MediaType.APPLICATION_JSON)
                                                                               .content(body(user)))
                .andExpect(status().isConflict());
         assertThat(findAppointment(a).getPatient()).isNotNull();
         assertThat(query("SELECT comment FROM appointments WHERE id = ?", String.class,
                          a.getId())).isEqualTo("private patient information");
      } finally {
         update("DROP TABLE erasure_test_hold");
      }
   }

   private <T> T query(String sql, Class<T> type, Object... values) {
      var query = entityManager.createNativeQuery(sql);
      for (int i = 0; i < values.length; i++) query.setParameter(i + 1, values[i]);
      Object result = query.getSingleResult();
      if (type == Integer.class && result instanceof Number number) return type.cast(number.intValue());
      return type.cast(result);
   }

   private void update(String sql, Object... values) {
      transaction.executeWithoutResult(status -> {
         var query = entityManager.createNativeQuery(sql);
         for (int i = 0; i < values.length; i++) query.setParameter(i + 1, values[i]);
         query.executeUpdate();
      });
   }

   private String body(User user) throws Exception {
      return objectMapper.writeValueAsString(
              new UserErasureRequestDto(user.getId(), user.getClinic().getId(), true));
   }

   private Appointment findAppointment(Appointment original) {
      ClinicContext.set(original.getClinic());
      try {
         return appointments.findById(original.getId()).orElseThrow();
      } finally {
         ClinicContext.clear();
      }
   }

   private User admin() {
      return dataFactory.createAdmin(UUID.randomUUID() + "@admin.test");
   }

   private Appointment appointment(User user, AppointmentStatus status, int days) {
      var clinic = user.getClinic();
      var patient = dataFactory.createPatientForUser(clinic, user);
      var doctor = dataFactory.createDoctor(clinic);
      var service = dataFactory.createClinicService(clinic);
      var a = new Appointment();
      a.setClinic(clinic);
      a.setPatient(patient);
      a.setDoctor(doctor);
      a.setScheduledAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(days));
      a.setEndAt(a.getScheduledAt().plusMinutes(30));
      a.setStatus(status);
      a.setTotalPrice(1000);
      a.setComment("private patient information");
      var item = new AppointmentServiceItem();
      item.setAppointment(a);
      item.setClinicService(service);
      item.setTitle("Consultation");
      item.setPrice(1000);
      item.setDurationMinutes(30);
      item.setPosition(1);
      item.setQuantity(1);
      a.getServices().add(item);
      return appointments.saveAndFlush(a);
   }
}

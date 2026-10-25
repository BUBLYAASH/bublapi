package org.bublapi.dent.integration.notification;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTest extends IntegrationTestSupport {
   @Autowired
   NotificationRepository notifications;

   @Test
   void patientSeesOnlyOwnSentInAppNotifications() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      var visible = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.SENT);
      notification(data.user(), NotificationChannel.EMAIL, NotificationStatus.SENT);
      notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.FAILED);
      var deleted = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.SENT);
      deleted.setDeleted(true);
      notifications.save(deleted);
      var other = dataFactory.createUser(data.clinic(), "other@notification.test");
      notification(other, NotificationChannel.IN_APP, NotificationStatus.SENT);
      mockMvc.perform(get("/api/v1/notifications").header("Authorization", jwtHelper.token(data.user().getId()))
                                                  .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(visible.getId().toString()));
      mockMvc.perform(
                     get("/api/v1/notifications/unread-count").header("Authorization", jwtHelper.token(data.user().getId()))
                                                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.count").value(1));
   }

   @Test
   void readIsIdempotentAndSoftDeleteHidesNotification() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      var notification = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.SENT);
      String base = "/api/v1/notifications/" + notification.getId();
      String token = jwtHelper.token(data.user().getId());
      String key = data.apiKey().rawKey();
      mockMvc.perform(patch(base + "/read").header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNoContent());
      var firstRead = notifications.findById(notification.getId()).orElseThrow();
      assertThat(firstRead.isRead()).isTrue();
      assertThat(firstRead.getReadAt()).isNotNull();
      mockMvc.perform(patch(base + "/read").header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNoContent());
      assertThat(notifications.findById(notification.getId()).orElseThrow().getReadAt()).isEqualTo(
              firstRead.getReadAt());
      mockMvc.perform(delete(base).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNoContent());
      assertThat(notifications.findById(notification.getId()).orElseThrow().isDeleted()).isTrue();
      mockMvc.perform(get(base).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNotFound());
   }

   @Test
   void otherPatientCannotReadModifyOrDeleteNotification() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      var other = dataFactory.createUser(data.clinic(), "other@notification.test");
      var notification = notification(other, NotificationChannel.IN_APP, NotificationStatus.SENT);
      String base = "/api/v1/notifications/" + notification.getId();
      String token = jwtHelper.token(data.user().getId());
      String key = data.apiKey().rawKey();
      mockMvc.perform(get(base).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNotFound());
      mockMvc.perform(patch(base + "/read").header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNotFound());
      mockMvc.perform(delete(base).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNotFound());
      var stored = notifications.findById(notification.getId()).orElseThrow();
      assertThat(stored.isRead()).isFalse();
      assertThat(stored.isDeleted()).isFalse();
   }

   @Test
   void readAllDoesNotChangeEmailFailedOrOtherUsersNotifications() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      var own = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.SENT);
      var email = notification(data.user(), NotificationChannel.EMAIL, NotificationStatus.SENT);
      var failed = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.FAILED);
      var otherUser = dataFactory.createUser(data.clinic(), "other@notification.test");
      var other = notification(otherUser, NotificationChannel.IN_APP, NotificationStatus.SENT);
      mockMvc.perform(
                     patch("/api/v1/notifications/read-all").header("Authorization", jwtHelper.token(data.user().getId()))
                                                            .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isNoContent());
      assertThat(notifications.findById(own.getId()).orElseThrow().isRead()).isTrue();
      for (var id : List.of(email.getId(), failed.getId(), other.getId()))
         assertThat(notifications.findById(id).orElseThrow().isRead()).isFalse();
   }

   @Test
   void patientTokenCannotUseAnotherClinicKeyForNotifications() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      var notification = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.SENT);
      String token = jwtHelper.token(data.user().getId());
      String foreignKey = dataFactory.createApiKey(dataFactory.createClinic()).rawKey();
      mockMvc.perform(get("/api/v1/notifications").header("Authorization", jwtHelper.token(data.user().getId()))
                                                  .header("X-API-KEY", foreignKey)).andExpect(status().isForbidden());
      String base = "/api/v1/notifications/" + notification.getId();
      mockMvc.perform(get(base).header("Authorization", token).header("X-API-KEY", foreignKey))
             .andExpect(status().isForbidden());
      mockMvc.perform(
                     get("/api/v1/notifications/unread-count").header("Authorization", token).header("X-API-KEY", foreignKey))
             .andExpect(status().isForbidden());
      mockMvc.perform(patch(base + "/read").header("Authorization", token).header("X-API-KEY", foreignKey))
             .andExpect(status().isForbidden());
      mockMvc.perform(
                     patch("/api/v1/notifications/read-all").header("Authorization", token).header("X-API-KEY", foreignKey))
             .andExpect(status().isForbidden());
      mockMvc.perform(delete(base).header("Authorization", token).header("X-API-KEY", foreignKey))
             .andExpect(status().isForbidden());
      var stored = notifications.findById(notification.getId()).orElseThrow();
      assertThat(stored.isRead()).isFalse();
      assertThat(stored.isDeleted()).isFalse();
   }

   @Test
   void adminCanInspectNotificationButCannotRetrySuccessfulDelivery() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      var notification = notification(data.user(), NotificationChannel.IN_APP, NotificationStatus.SENT);
      String token = jwtHelper.token(dataFactory.createAdmin("admin@notification.test").getId());
      mockMvc.perform(get("/api/v1/admin/notifications/" + notification.getId()).header("Authorization", token))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value(notification.getId().toString()));
      mockMvc.perform(
                     post("/api/v1/admin/notifications/" + notification.getId() + "/retry").header("Authorization", token))
             .andExpect(status().isBadRequest());
   }

   private Notification notification(User user, NotificationChannel channel, NotificationStatus status) {
      var notification = new Notification();
      notification.setClinic(user.getClinic());
      notification.setUser(user);
      notification.setRequestId(UUID.randomUUID());
      notification.setChannel(channel);
      notification.setStatus(status);
      notification.setType(NotificationType.USER_REGISTERED);
      notification.setTitle("Test notification");
      notification.setMessage("Message");
      notification.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
      return notifications.save(notification);
   }
}

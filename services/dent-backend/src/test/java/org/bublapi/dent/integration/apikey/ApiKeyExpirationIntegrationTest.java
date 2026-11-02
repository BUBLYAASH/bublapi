package org.bublapi.dent.integration.apikey;

import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.apikey.scheduler.ApiKeyExpirationScheduler;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.clinic.service.ClinicService;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyExpirationIntegrationTest extends IntegrationTestSupport {

   @Autowired
   private ApiKeyRepository apiKeyRepository;

   @Autowired
   private ApiKeyService apiKeyService;

   @Autowired
   private ClinicService clinicService;

   @Autowired
   private JdbcTemplate jdbc;

   private void runScheduler() {
      new ApiKeyExpirationScheduler(apiKeyRepository, clinicService).deactivateExpiredClinics();
   }

   private void setGraceUntil(UUID clinicId, LocalDateTime graceUntil) {
      var key = apiKeyRepository.findByClinic_IdAndActiveTrue(clinicId).orElseThrow();

      key.setExpiresAt(graceUntil.minusDays(14));
      key.setGraceUntil(graceUntil);

      apiKeyRepository.saveAndFlush(key);
   }

   private boolean clinicActive(UUID clinicId) {
      return Boolean.TRUE.equals(
              jdbc.queryForObject("SELECT active FROM clinics WHERE id = ?", Boolean.class, clinicId));
   }

   @Test
   void shouldDeactivateClinicWhenGracePeriodExpired() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);

      setGraceUntil(clinic.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

      runScheduler();

      assertThat(clinicActive(clinic.getId())).isFalse();
   }

   @Test
   void shouldNotDeactivateClinicBeforeGracePeriodExpired() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);

      setGraceUntil(clinic.getId(), LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

      runScheduler();

      assertThat(clinicActive(clinic.getId())).isTrue();
   }

   @Test
   void shouldNotDeactivateClinicTwice() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);

      setGraceUntil(clinic.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

      runScheduler();

      assertThat(clinicActive(clinic.getId())).isFalse();

      runScheduler();

      assertThat(clinicActive(clinic.getId())).isFalse();

      assertThat(apiKeyRepository.findAllExpiredClinicIds(LocalDateTime.now(ZoneOffset.UTC))).doesNotContain(
              clinic.getId());
   }

   @Test
   void shouldRestoreClinicWhenExpiredApiKeyRenewed() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);

      setGraceUntil(clinic.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

      runScheduler();

      assertThat(clinicActive(clinic.getId())).isFalse();

      apiKeyService.renewApiKey(clinic.getId());

      assertThat(clinicActive(clinic.getId())).isTrue();

      var renewedKey = apiKeyRepository.findByClinic_IdAndActiveTrue(clinic.getId()).orElseThrow();

      assertThat(renewedKey.getExpiresAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC));

      assertThat(renewedKey.getGraceUntil()).isAfter(LocalDateTime.now(ZoneOffset.UTC));

      runScheduler();

      assertThat(clinicActive(clinic.getId())).isTrue();
   }

   @Test
   void shouldDeactivateAndRestoreRelatedEntities() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);

      var user = dataFactory.createUser(clinic, "expired-" + UUID.randomUUID() + "@test.com");

      var doctor = dataFactory.createDoctor(clinic);
      var service = dataFactory.createClinicService(clinic);

      setGraceUntil(clinic.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

      runScheduler();

      System.out.println("CLINIC: " + clinicActive(clinic.getId()));
      System.out.println("USER: " + userEnabled(user.getId()));
      System.out.println("DOCTOR: " + doctorActive(doctor.getId()));
      System.out.println("SERVICE: " + serviceActive(service.getId()));

      assertThat(clinicActive(clinic.getId())).isFalse();

      assertThat(userEnabled(user.getId())).isFalse();
      assertThat(doctorActive(doctor.getId())).isFalse();
      assertThat(serviceActive(service.getId())).isFalse();

      apiKeyService.renewApiKey(clinic.getId());

      assertThat(clinicActive(clinic.getId())).isTrue();

      assertThat(userEnabled(user.getId())).isTrue();
      assertThat(doctorActive(doctor.getId())).isTrue();
      assertThat(serviceActive(service.getId())).isTrue();
   }

   private boolean userEnabled(UUID id) {
      return Boolean.TRUE.equals(jdbc.queryForObject("SELECT enabled FROM users WHERE id = ?", Boolean.class, id));
   }

   private boolean doctorActive(UUID id) {
      return Boolean.TRUE.equals(jdbc.queryForObject("SELECT active FROM doctors WHERE id = ?", Boolean.class, id));
   }

   private boolean serviceActive(UUID id) {
      return Boolean.TRUE.equals(
              jdbc.queryForObject("SELECT active FROM clinic_services WHERE id = ?", Boolean.class, id));
   }
}

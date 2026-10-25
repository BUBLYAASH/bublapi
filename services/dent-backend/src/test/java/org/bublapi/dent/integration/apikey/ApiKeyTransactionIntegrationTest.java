package org.bublapi.dent.integration.apikey;

import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiKeyTransactionIntegrationTest extends IntegrationTestSupport {
   @Autowired
   ApiKeyService service;
   @Autowired
   ApiKeyRepository keys;
   @Autowired
   ClinicRepository clinics;

   @Test
   void failedRotationRollsBackOldKeyDeactivation() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);
      var old = keys.findByClinic_IdAndActiveTrue(clinic.getId()).orElseThrow();
      clinic.setActive(false);
      clinics.save(clinic);
      assertThatThrownBy(() -> service.rotate(clinic.getId())).isInstanceOf(ResourceNotFoundException.class);
      assertThat(keys.findById(old.getId()).orElseThrow().isActive()).isTrue();
      assertThat(keys.count()).isEqualTo(1);
   }

   @Test
   void renewAfterGracePeriodRestartsValidityFromNow() {
      var clinic = dataFactory.createClinic();
      dataFactory.createApiKey(clinic);
      var key = keys.findByClinic_IdAndActiveTrue(clinic.getId()).orElseThrow();
      key.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMonths(2));
      key.setGraceUntil(LocalDateTime.now(ZoneOffset.UTC).minusMonths(1));
      keys.save(key);
      LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).plusMonths(1);
      service.renewApiKey(clinic.getId());
      var renewed = keys.findById(key.getId()).orElseThrow();
      assertThat(renewed.getExpiresAt()).isBetween(before, LocalDateTime.now(ZoneOffset.UTC).plusMonths(1));
      assertThat(renewed.getGraceUntil()).isEqualTo(renewed.getExpiresAt().plusDays(14));
   }

   @Test
   void inactiveClinicKeyCannotAccessPublicApiWithoutJwt() throws Exception {
      var clinic = dataFactory.createClinic();
      String raw = dataFactory.createApiKey(clinic).rawKey();
      clinic.setActive(false);
      clinics.save(clinic);
      mockMvc.perform(get("/api/v1/public/doctors").header("X-API-KEY", raw)).andExpect(status().isUnauthorized());
   }
}

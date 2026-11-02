package org.bublapi.dent.apikey.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.clinic.service.ClinicService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class ApiKeyExpirationScheduler {

   private final ApiKeyRepository apiKeyRepository;
   private final ClinicService clinicService;

   @Scheduled(cron = "0 0 * * * *", zone = "UTC")
   public void deactivateExpiredClinics() {
      List<UUID> clinicIds = apiKeyRepository.findAllExpiredClinicIds(LocalDateTime.now(ZoneOffset.UTC));

      for (UUID clinicId : clinicIds) {
         try {
            clinicService.deactivate(clinicId);
            log.info("Clinic {} deactivated: API Key expired", clinicId);
         } catch (Exception e) {
            log.error("Failed to deactivate clinic {}", clinicId, e);
         }
      }
   }
}

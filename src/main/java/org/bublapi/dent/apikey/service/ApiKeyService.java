package org.bublapi.dent.apikey.service;

import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ApiKeyService {

   private final ApiKeyRepository apiKeyRepository;
   private final ClinicRepository clinicRepository;

   public ApiKeyService(ApiKeyRepository apiKeyRepository, ClinicRepository clinicRepository) {
      this.apiKeyRepository = apiKeyRepository;
      this.clinicRepository = clinicRepository;
   }

   @Transactional
   public CreateApiKeyResponseDto createApiKey(UUID clinicId, String name) {
      if (apiKeyRepository.existsByClinic_IdAndActiveTrue(clinicId)) {
         throw new IllegalStateException("API Key already exists");
      }

      Clinic clinic = clinicRepository.findByIdAndActiveTrue(clinicId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Clinic not found or unavailable"));

      String rawKey = generateRawKey();
      ParsedKey parsed = parse(rawKey);

      LocalDateTime now = LocalDateTime.now();

      ApiKey apiKey = new ApiKey();
      apiKey.setClinic(clinic);
      apiKey.setName(name);
      apiKey.setPrefix(parsed.prefix);
      apiKey.setHash(hash(parsed.secret));
      apiKey.setExpiresAt(now.plusMonths(1));
      apiKey.setGraceUntil(now.plusMonths(1).plusDays(14));

      apiKeyRepository.save(apiKey);

      return new CreateApiKeyResponseDto(rawKey);
   }

   @Transactional
   public void renewApiKey(UUID clinicId) {
      ApiKey apiKey = apiKeyRepository.findByClinic_IdAndActiveTrue(clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));

      LocalDateTime now = LocalDateTime.now();

      if (apiKey.getGraceUntil().isAfter(now)) {
         apiKey.setExpiresAt(apiKey.getExpiresAt().plusMonths(1));
         apiKey.setGraceUntil(apiKey.getGraceUntil().plusMonths(1));
      } else {
         apiKey.setExpiresAt(now.plusMonths(1));
         apiKey.setGraceUntil(now.plusMonths(1).plusDays(14));
      }
   }

   public ApiKey validate(String rawKey) {
      ParsedKey parsed = parse(rawKey);

      ApiKey apiKey = apiKeyRepository.findByPrefix(parsed.prefix)
                                      .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));

      String hashed = hash(parsed.secret);

      if (!apiKey.getHash().equals(hashed)) {
         throw new ResourceNotFoundException("Invalid API Key");
      }

      if (!apiKey.isActive()) {
         throw new ResourceNotFoundException("API Key disabled");
      }

      LocalDateTime now = LocalDateTime.now();
      boolean expired = apiKey.getExpiresAt().isBefore(now);
      boolean graceExpired = apiKey.getGraceUntil().isBefore(now);

      if (expired && graceExpired) {
         throw new ResourceNotFoundException("API Key expired");
      }

      return apiKey;
   }

   @Transactional
   public CreateApiKeyResponseDto rotate(UUID clinicId) {
      ApiKey old = apiKeyRepository.findByClinic_IdAndActiveTrue(clinicId)
                                   .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));
      old.setActive(false);

      return createApiKey(clinicId, old.getName());
   }

   @Transactional
   public void revoke(UUID apiKeyId) {
      ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                                      .orElseThrow(() -> new ResourceNotFoundException("ApiKey not found"));

      apiKey.setActive(false);
   }

   private String generateRawKey() {
      String prefix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
      String secret = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

      return "pk_live_" + prefix + "_" + secret;
   }

   private String hash(String value) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));

         StringBuilder hex = new StringBuilder();
         for (byte b : hash) {
            hex.append(String.format("%02x", b));
         }

         return hex.toString();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private record ParsedKey(String prefix, String secret) {
   }

   private ParsedKey parse(String rawKey) {
      if (!rawKey.startsWith("pk_live_")) {
         throw new IllegalArgumentException("Invalid API Key format");
      }

      String withoutPrefix = rawKey.substring("pk_live_".length());

      int idx = withoutPrefix.indexOf('_');
      if (idx == -1) {
         throw new IllegalArgumentException("Invalid API Key structure");
      }

      String prefix = withoutPrefix.substring(0, idx);
      String secret = withoutPrefix.substring(idx + 1);

      return new ParsedKey(prefix, secret);
   }

// TODO:
//  - auto-deactivate after grace period and auto-activate when renewed
}

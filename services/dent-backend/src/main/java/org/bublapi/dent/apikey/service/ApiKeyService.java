package org.bublapi.dent.apikey.service;

import org.bublapi.dent.apikey.dto.ApiKeyResponseDto;
import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.logging.audit.AdministrativeAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {
   private static final String API_KEY_PREFIX = "pk_live_";

   private static final String API_KEY_NOT_FOUND = "API Key not found";

   private final ApiKeyRepository apiKeyRepository;
   private final ClinicRepository clinicRepository;
   private final AdministrativeAuditService administrativeAuditService;

   public ApiKeyService(ApiKeyRepository apiKeyRepository, ClinicRepository clinicRepository,
                        AdministrativeAuditService administrativeAuditService) {
      this.apiKeyRepository = apiKeyRepository;
      this.clinicRepository = clinicRepository;
      this.administrativeAuditService = administrativeAuditService;
   }

   @Transactional
   public CreateApiKeyResponseDto createApiKey(UUID clinicId, String name) {
      ApiKeyCreationResult result = createApiKeyInternal(clinicId, name);

      administrativeAuditService.apiKeyCreated(result.apiKeyId, clinicId);

      return new CreateApiKeyResponseDto(result.rawKey);
   }

   @Transactional
   public void renewApiKey(UUID clinicId) {
      ApiKey apiKey = apiKeyRepository.findByClinic_IdAndActiveTrue(clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException(API_KEY_NOT_FOUND));

      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

      if (apiKey.getGraceUntil().isAfter(now)) {
         apiKey.setExpiresAt(apiKey.getExpiresAt().plusMonths(1));
         apiKey.setGraceUntil(apiKey.getGraceUntil().plusMonths(1));
      } else {
         apiKey.setExpiresAt(now.plusMonths(1));
         apiKey.setGraceUntil(now.plusMonths(1).plusDays(14));
      }

      administrativeAuditService.apiKeyRenewed(apiKey.getId(), clinicId);
   }

   @Transactional(readOnly = true)
   public ApiKey validate(String rawKey) {
      ParsedKey parsed = parse(rawKey);

      ApiKey apiKey = apiKeyRepository.findByPrefix(parsed.prefix)
                                      .orElseThrow(() -> new ResourceNotFoundException(API_KEY_NOT_FOUND));

      String hashed = hash(parsed.secret);

      if (!apiKey.getHash().equals(hashed)) {
         throw new ResourceNotFoundException("Invalid API Key");
      }

      if (!apiKey.isActive()) {
         throw new ResourceNotFoundException("API Key disabled");
      }

      if (!apiKey.getClinic().isActive()) {
         throw new ResourceNotFoundException("Clinic disabled");
      }

      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
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
                                   .orElseThrow(() -> new ResourceNotFoundException(API_KEY_NOT_FOUND));
      old.setActive(false);

      ApiKeyCreationResult result = createApiKeyInternal(clinicId, old.getName());

      administrativeAuditService.apiKeyRotated(result.apiKeyId, clinicId);

      return new CreateApiKeyResponseDto(result.rawKey);
   }

   @Transactional
   public void revoke(UUID apiKeyId) {
      ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                                      .orElseThrow(() -> new ResourceNotFoundException(API_KEY_NOT_FOUND));

      apiKey.setActive(false);

      administrativeAuditService.apiKeyRevoked(apiKey.getId(), apiKey.getClinic().getId());
   }

   public List<ApiKeyResponseDto> findAll() {
      return apiKeyRepository.findAll()
                             .stream()
                             .map(apiKey -> new ApiKeyResponseDto(apiKey.getId(), apiKey.getClinic().getId(),
                                                                  apiKey.getName(), apiKey.getPrefix(),
                                                                  apiKey.getExpiresAt(), apiKey.getGraceUntil(),
                                                                  apiKey.isActive()))
                             .toList();
   }

   private ApiKeyCreationResult createApiKeyInternal(UUID clinicId, String name) {
      if (apiKeyRepository.existsByClinic_IdAndActiveTrue(clinicId)) {
         throw new IllegalStateException("API Key already exists");
      }

      Clinic clinic = clinicRepository.findByIdAndActiveTrue(clinicId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Clinic not found or unavailable"));

      String rawKey = generateRawKey();
      ParsedKey parsed = parse(rawKey);

      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

      ApiKey apiKey = new ApiKey();
      apiKey.setClinic(clinic);
      apiKey.setName(name);
      apiKey.setPrefix(parsed.prefix);
      apiKey.setHash(hash(parsed.secret));
      apiKey.setExpiresAt(now.plusMonths(1));
      apiKey.setGraceUntil(now.plusMonths(1).plusDays(14));

      ApiKey saved = apiKeyRepository.save(apiKey);

      return new ApiKeyCreationResult(saved.getId(), rawKey);
   }

   private String generateRawKey() {
      String prefix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
      String secret = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

      return API_KEY_PREFIX + prefix + "_" + secret;
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

   private ParsedKey parse(String rawKey) {
      if (!rawKey.startsWith(API_KEY_PREFIX)) {
         throw new IllegalArgumentException("Invalid API Key format");
      }

      String withoutPrefix = rawKey.substring(API_KEY_PREFIX.length());

      int idx = withoutPrefix.indexOf('_');
      if (idx == -1) {
         throw new IllegalArgumentException("Invalid API Key structure");
      }

      String prefix = withoutPrefix.substring(0, idx);
      String secret = withoutPrefix.substring(idx + 1);

      return new ParsedKey(prefix, secret);
   }

   private record ParsedKey(String prefix, String secret) {
   }

   private record ApiKeyCreationResult(UUID apiKeyId, String rawKey) {
   }

// TODO:
//  - auto-deactivate after grace period and auto-activate when renewed
}

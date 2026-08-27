package org.bublapi.dent.apikey.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.apikey.dto.ApiKeyResponseDto;
import org.bublapi.dent.apikey.dto.CreateApiKeyRequestDto;
import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin API Keys management")
@RestController
@RequestMapping("/api/admin/api-keys")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiKeyController {
   private final ApiKeyService apiKeyService;

   public AdminApiKeyController(ApiKeyService apiKeyService) {
      this.apiKeyService = apiKeyService;
   }

   @Operation(summary = "Create API Key for clinic", description = "Create an API Key for the clinic")
   @PostMapping("/{clinicId}")
   public CreateApiKeyResponseDto create(@PathVariable UUID clinicId, @RequestBody CreateApiKeyRequestDto request) {
      return apiKeyService.createApiKey(clinicId, request.name());
   }

   @Operation(summary = "Renew API Key for clinic", description = "Renews an API Key subscription for the clinic")
   @PatchMapping("/{clinicId}/renew")
   public ResponseEntity<Void> renew(@PathVariable UUID clinicId) {
      apiKeyService.renewApiKey(clinicId);
      return ResponseEntity.ok().build();
   }

   @Operation(summary = "Rotate API Key")
   @PostMapping("/{clinicId}/rotate")
   public CreateApiKeyResponseDto rotate(@PathVariable UUID clinicId) {
      return apiKeyService.rotate(clinicId);
   }

   @Operation(summary = "Revoke API Key")
   @ResponseStatus(HttpStatus.NO_CONTENT)
   @DeleteMapping("/{apiKeyId}")
   public void revoke(@PathVariable UUID apiKeyId) {
      apiKeyService.revoke(apiKeyId);
   }

   @Operation(summary = "Show all API Keys for admin", description = "Shows all API Keys")
   @GetMapping
   public List<ApiKeyResponseDto> getAll() {
      return apiKeyService.findAll();
   }
}

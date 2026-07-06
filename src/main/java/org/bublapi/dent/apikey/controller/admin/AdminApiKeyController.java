package org.bublapi.dent.apikey.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.apikey.dto.CreateApiKeyRequestDto;
import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin API Keys management")
@RestController
@RequestMapping("/api/admin/clinics/{clinicId}/api-key")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiKeyController {
   private final ApiKeyService apiKeyService;

   public AdminApiKeyController(ApiKeyService apiKeyService) {
      this.apiKeyService = apiKeyService;
   }

   @Operation(summary = "Create API Key for clinic", description = "Create an API Key for the clinic")
   @PostMapping
   public CreateApiKeyResponseDto create(@PathVariable UUID clinicId, @RequestBody CreateApiKeyRequestDto request) {
      return apiKeyService.createApiKey(clinicId, request.name());
   }
}

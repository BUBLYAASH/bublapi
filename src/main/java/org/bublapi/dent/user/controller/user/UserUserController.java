package org.bublapi.dent.user.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.auth.security.CustomUserDetails;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User actions for authenticated")
@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth")
public class UserUserController {

   private final UserService userService;

   public UserUserController(UserService userService) {
      this.userService = userService;
   }

   @Operation(summary = "Update a user from profile", description = "Update only provided fields from profile")
   @PatchMapping
   public UserResponseDto updateUser(@AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody UpdateUserRequestDto request) {
      return userService.update(userDetails.getId(), request);
   }

   @Operation(summary = "Deactivate own user account", description = "Deactivates the user account from profile")
   @PatchMapping("/deactivation")
   public UserResponseDto deactivate(@AuthenticationPrincipal CustomUserDetails userDetails) {
      return userService.deactivate(userDetails.getId());
   }
}

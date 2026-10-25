package org.bublapi.dent.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String error, String message, int status, LocalDateTime timestamp, Map<String, String> validationErrors) {
   public static ApiErrorResponse of(String error, String message, HttpStatus status) {
      return new ApiErrorResponse(error, message, status.value(), LocalDateTime.now(ZoneOffset.UTC), null);
   }

   public static ApiErrorResponse validation(String message, Map<String, String> validationErrors) {
      return new ApiErrorResponse("Validation failed", message, HttpStatus.BAD_REQUEST.value(),
                                  LocalDateTime.now(ZoneOffset.UTC), Map.copyOf(validationErrors));
   }
}

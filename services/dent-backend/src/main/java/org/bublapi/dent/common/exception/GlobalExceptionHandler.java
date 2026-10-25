package org.bublapi.dent.common.exception;

import org.bublapi.dent.common.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@RestControllerAdvice
public class GlobalExceptionHandler {

   private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

   @ExceptionHandler(ResourceNotFoundException.class)
   public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
      return errorResponse(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage());

   }

   @ExceptionHandler(BadRequestException.class)
   public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException e) {
      return errorResponse(HttpStatus.BAD_REQUEST, "Bad request", e.getMessage());
   }

   @ExceptionHandler(HttpMessageNotReadableException.class)
   public ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
      return errorResponse(HttpStatus.BAD_REQUEST, "Bad request", "Request body is missing or malformed");
   }

   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
   public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch() {
      return errorResponse(HttpStatus.BAD_REQUEST, "Bad request", "Request parameter has an invalid value");
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
      Map<String, String> validationErrors = new HashMap<>();

      e.getBindingResult()
       .getFieldErrors()
       .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

      return ResponseEntity.badRequest()
                           .body(ApiErrorResponse.validation("Request contains invalid data", validationErrors));
   }

   @ExceptionHandler(DataIntegrityViolationException.class)
   public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation() {
      return errorResponse(HttpStatus.CONFLICT, "Data integrity violation",
                           "The operation conflicts with existing data");
   }

   @ExceptionHandler(AccessDeniedException.class)
   public ResponseEntity<ApiErrorResponse> handleAccessDenied() {
      return errorResponse(HttpStatus.FORBIDDEN, "Forbidden", "Access denied");
   }

   @ExceptionHandler(AuthenticationException.class)
   public ResponseEntity<ApiErrorResponse> handleAuthentication() {
      return errorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required");
   }

   @ExceptionHandler(InvalidRefreshTokenException.class)
   public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken() {
      return errorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid refresh token");
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ApiErrorResponse> handleGeneral(Exception e) {
      log.atError()
         .addKeyValue("errorClass", e.getClass().getName())
         .addKeyValue("errorLocation", getErrorLocation(e))
         .setCause(e)
         .log("Unhandled exception");

      return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
   }

   private static String getErrorLocation(Exception e) {
      StackTraceElement[] stackTrace = e.getStackTrace();

      if (stackTrace.length == 0) {
         return "unknown";
      }

      StackTraceElement element = stackTrace[0];

      return element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
   }

   private static ResponseEntity<ApiErrorResponse> errorResponse(HttpStatus status, String error, String message) {
      return ResponseEntity.status(status).body(ApiErrorResponse.of(error, message, status));
   }
}

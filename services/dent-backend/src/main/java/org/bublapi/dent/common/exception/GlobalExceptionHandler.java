package org.bublapi.dent.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@RestControllerAdvice
public class GlobalExceptionHandler {

   private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

   @ExceptionHandler(ResourceNotFoundException.class)
   public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Resource not found");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.NOT_FOUND.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
   }

   @ExceptionHandler(BadRequestException.class)
   public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Bad request");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.BAD_REQUEST.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Method argument not valid");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.BAD_REQUEST.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
   }

   @ExceptionHandler(DataIntegrityViolationException.class)
   public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Data Integrity Violation");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.CONFLICT.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.CONFLICT);
   }

   @ExceptionHandler(AccessDeniedException.class)
   public ResponseEntity<Map<String, Object>> handleAccessDenied() {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Forbidden");
      response.put("message", "Access denied");
      response.put("status", HttpStatus.FORBIDDEN.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
   }

   @ExceptionHandler(AuthenticationException.class)
   public ResponseEntity<Map<String, Object>> handleAuthentication() {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Unauthorized");
      response.put("message", "Authentication required");
      response.put("status", HttpStatus.UNAUTHORIZED.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
      log.atError().addKeyValue("errorClass", e.getClass().getName()).setCause(e).log("Unhandled exception");

      Map<String, Object> response = new HashMap<>();

      response.put("error", "Internal Server Error");
      response.put("message", "An unexpected error occurred");
      response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
   }
}

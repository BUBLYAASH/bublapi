package org.bublapi.dent.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(ResourceNotFoundException.class)
   public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Resource not found");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.NOT_FOUND.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
           MethodArgumentNotValidException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Method argument not valid");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.BAD_REQUEST.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
   }

   @ExceptionHandler(DataIntegrityViolationException.class)
   public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
           DataIntegrityViolationException e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Data Integrity Violation");
      response.put("message", e.getMessage());
      response.put("status", HttpStatus.CONFLICT.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.CONFLICT);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
      Map<String, Object> response = new HashMap<>();

      response.put("error", "Internal Server Error");
      response.put("message", "Unexpected Server Error");
      response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.put("timestamp", LocalDateTime.now());

      return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
   }
}

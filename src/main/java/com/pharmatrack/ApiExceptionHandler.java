package com.pharmatrack;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Catches exceptions thrown anywhere in the controllers/services and turns them into
// a clean JSON answer {"error": "..."} with HTTP status 400, instead of a crash page.
@RestControllerAdvice
public class ApiExceptionHandler {

    // Our own rule violations (duplicate batch, bad quantity, not enough stock ...).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleRule(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    // Broken JSON, text in a number field, or a date that is not YYYY-MM-DD (e.g. 2027-13-45).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleBadJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error",
                "Invalid input: quantity must be a whole number and expiry date must be a real date (YYYY-MM-DD)."));
    }

    // The database's unique rule on batch number caught a duplicate that slipped past the service check.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DataIntegrityViolationException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "That batch number already exists."));
    }
}

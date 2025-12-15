package com.platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You don't have permission to perform this action. Please check your role and permissions.")
            .path("/api")
            .build();
            
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException occurred: {}", ex.getMessage());
        
        String userFriendlyMessage = getUserFriendlyMessage(ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(userFriendlyMessage)
            .path("/api/invitations")
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException occurred: {}", ex.getMessage(), ex);
        
        String userFriendlyMessage = getUserFriendlyMessage(ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message(userFriendlyMessage)
            .path("/api")
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.error("ResponseStatusException occurred: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(ex.getStatusCode().value())
            .error(ex.getStatusCode().toString())
            .message(ex.getReason() != null ? ex.getReason() : "An error occurred")
            .path("/api")
            .build();
            
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error occurred: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Please check the provided information and try again")
            .fieldErrors(fieldErrors)
            .path("/api")
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please try again later.")
            .path("/api")
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getUserFriendlyMessage(String originalMessage) {
        if (originalMessage == null) {
            return "An error occurred. Please try again.";
        }
        
        // Map technical messages to user-friendly ones
        switch (originalMessage) {
            case "User already belongs to a corporate":
                return "This user is already a member of an organization and cannot be invited to another one.";
            case "User already has a pending invitation":
                return "This user already has a pending invitation. Please wait for them to respond or cancel the existing invitation.";
            case "Invitation is not pending":
                return "This invitation is no longer valid or has already been processed.";
            case "Invitation has expired":
                return "This invitation has expired. Please request a new invitation.";
            case "Cannot cancel invitation from another corporate":
                return "You can only cancel invitations from your own organization.";
            case "Can only cancel pending invitations":
                return "Only pending invitations can be cancelled.";
            case "Cannot resend invitation from another corporate":
                return "You can only resend invitations from your own organization.";
            default:
                // For unknown messages, return a generic friendly message
                if (originalMessage.toLowerCase().contains("not found")) {
                    return "The requested resource was not found.";
                } else if (originalMessage.toLowerCase().contains("access denied") || 
                          originalMessage.toLowerCase().contains("forbidden")) {
                    return "You don't have permission to perform this action.";
                } else if (originalMessage.toLowerCase().contains("duplicate") || 
                          originalMessage.toLowerCase().contains("already exists")) {
                    return "This information already exists. Please use different details.";
                } else {
                    return "An error occurred. Please check your information and try again.";
                }
        }
    }
}
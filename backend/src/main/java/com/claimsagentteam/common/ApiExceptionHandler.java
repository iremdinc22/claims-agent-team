package com.claimsagentteam.common;

import java.util.LinkedHashMap;
import java.util.Map;

import com.claimsagentteam.claim.ClaimController;
import com.claimsagentteam.claim.FutureIncidentDateException;
import com.claimsagentteam.claim.PolicyNotFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ClaimController.class)
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return validationError(fieldErrors);
    }

    @ExceptionHandler(FutureIncidentDateException.class)
    ResponseEntity<ApiErrorResponse> handleFutureIncidentDate(FutureIncidentDateException exception) {
        return validationError(Map.of("incidentDate", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        String field = extractFieldName(exception);
        Map<String, String> fieldErrors = field == null
                ? Map.of()
                : Map.of(field, invalidValueMessage(field));
        return validationError(fieldErrors);
    }

    @ExceptionHandler(PolicyNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handlePolicyNotFound(PolicyNotFoundException exception) {
        return ResponseEntity.unprocessableEntity().body(new ApiErrorResponse(
                "POLICY_NOT_FOUND",
                "Referenced policy was not found",
                Map.of("policyNumber", exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected failure while processing API request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                "INTERNAL_ERROR",
                "The request could not be completed",
                Map.of()));
    }

    private ResponseEntity<ApiErrorResponse> validationError(Map<String, String> fieldErrors) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                fieldErrors));
    }

    private String extractFieldName(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof JsonMappingException mappingException && !mappingException.getPath().isEmpty()) {
            return mappingException.getPath().getLast().getFieldName();
        }
        return null;
    }

    private String invalidValueMessage(String field) {
        return switch (field) {
            case "incidentType" -> "Incident type must be one of COLLISION, THEFT, GLASS_DAMAGE, or OTHER";
            case "incidentDate" -> "Incident date must use YYYY-MM-DD format";
            default -> "Invalid value";
        };
    }
}

package com.eventledger.gateway.exception;

import com.eventledger.gateway.metrics.EventMetrics;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final EventMetrics eventMetrics;

    public GlobalExceptionHandler(EventMetrics eventMetrics) {
        this.eventMetrics = eventMetrics;
    }

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAccountServiceUnavailable(
            AccountServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        recordPostEventMetric(request, "account_service_unavailable");
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(EventConflictException.class)
    public ResponseEntity<Map<String, Object>> handleEventConflict(
            EventConflictException exception,
            HttpServletRequest request
    ) {
        recordPostEventMetric(request, "duplicate");
        return errorResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {
        return errorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        recordPostEventMetric(request, "validation_error");
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return errorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleDownstreamClientError(
            RestClientResponseException exception,
            HttpServletRequest request
    ) {
        recordPostEventMetric(request, "account_service_unavailable");
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        return errorResponse(status, exception.getMessage(), request);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(response);
    }

    private void recordPostEventMetric(HttpServletRequest request, String result) {
        if ("POST".equals(request.getMethod()) && "/events".equals(request.getRequestURI())) {
            eventMetrics.recordEventReceived(result);
        }
    }
}

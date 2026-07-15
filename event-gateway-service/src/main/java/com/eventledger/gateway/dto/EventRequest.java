package com.eventledger.gateway.dto;

import com.eventledger.gateway.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record EventRequest(
        @NotBlank(message = "Event ID is required.")
        @Size(max = 100, message = "Event ID must not exceed 100 characters.")
        String eventId,

        @NotBlank(message = "Account ID is required.")
        @Size(max = 100, message = "Account ID must not exceed 100 characters.")
        String accountId,

        @NotNull(message = "Event type is required.")
        EventType type,

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount,

        @NotBlank(message = "Currency is required.")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters.")
        String currency,

        @NotNull(message = "Event timestamp is required.")
        Instant eventTimestamp,

        Map<String, String> metadata
) {
}

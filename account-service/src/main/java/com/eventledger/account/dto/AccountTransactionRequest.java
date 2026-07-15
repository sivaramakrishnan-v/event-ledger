package com.eventledger.account.dto;

import com.eventledger.account.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionRequest(
        @NotBlank(message = "Event ID is required.")
        @Size(max = 100, message = "Event ID must not exceed 100 characters.")
        String eventId,

        @NotNull(message = "Transaction type is required.")
        TransactionType type,

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount,

        @NotBlank(message = "Currency is required.")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters.")
        String currency,

        @NotNull(message = "Event timestamp is required.")
        Instant eventTimestamp
) {
}

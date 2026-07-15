package com.eventledger.gateway.client;

import com.eventledger.gateway.entity.EventType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionRequest(
        String eventId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp
) {
}

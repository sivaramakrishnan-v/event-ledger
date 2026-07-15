package com.eventledger.gateway.dto;

import com.eventledger.gateway.entity.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record EventResponse(
        String eventId,
        String accountId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Map<String, String> metadata,
        Instant createdAt
) {
}

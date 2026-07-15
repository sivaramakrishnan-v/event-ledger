package com.eventledger.gateway.mapper;

import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.entity.EventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventMapper {

    private final ObjectMapper objectMapper;

    public EventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventEntity toEntity(EventRequest request) {
        String metadataJson = null;

        if (request.metadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(request.metadata());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Failed to serialize event metadata",
                        e
                );
            }
        }

        return new EventEntity(
                request.eventId(),
                request.accountId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp(),
                metadataJson
        );
    }

    public EventResponse toResponse(EventEntity entity) {
        Map<String, String> metadata = null;

        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            try {
                metadata = objectMapper.readValue(
                        entity.getMetadata(),
                        new TypeReference<Map<String, String>>() {
                        }
                );
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Failed to deserialize event metadata",
                        e
                );
            }
        }

        return new EventResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                metadata,
                entity.getCreatedAt()
        );
    }
}
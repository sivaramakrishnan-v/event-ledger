package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.client.AccountTransactionRequest;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.entity.EventEntity;
import com.eventledger.gateway.exception.EventConflictException;
import com.eventledger.gateway.mapper.EventMapper;
import com.eventledger.gateway.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final AccountServiceClient accountServiceClient;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            AccountServiceClient accountServiceClient
    ) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.accountServiceClient = accountServiceClient;
    }

    public EventSubmissionResult submitEvent(EventRequest request) {
        Optional<EventEntity> existingEvent = eventRepository.findByEventId(request.eventId());

        if (existingEvent.isPresent()) {
            EventEntity entity = existingEvent.get();
            if (matchesExistingEvent(entity, request)) {
                return new EventSubmissionResult(eventMapper.toResponse(entity), false);
            }
            throw new EventConflictException(
                    "Event ID already exists with different event data: "
                            + request.eventId()
            );
        }

        AccountTransactionRequest transactionRequest = new AccountTransactionRequest(
                request.eventId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp()
        );

        accountServiceClient.applyTransaction(request.accountId(), transactionRequest);

        EventEntity newEventEntity = eventMapper.toEntity(request);
        EventEntity savedEntity = eventRepository.save(newEventEntity);

        return new EventSubmissionResult(eventMapper.toResponse(savedEntity), true);
    }

    private boolean matchesExistingEvent(
            EventEntity existingEvent,
            EventRequest request
    ) {
        EventResponse existingEventResponse = eventMapper.toResponse(existingEvent);

        return Objects.equals(existingEvent.getAccountId(), request.accountId()) &&
                Objects.equals(existingEvent.getType(), request.type()) &&
                existingEvent.getAmount().compareTo(request.amount()) == 0 &&
                Objects.equals(existingEvent.getCurrency(), request.currency()) &&
                Objects.equals(existingEvent.getEventTimestamp(), request.eventTimestamp()) &&
                Objects.equals(existingEventResponse.metadata(), request.metadata());
    }

    public EventResponse getEvent(String eventId) {
        return eventRepository.findByEventId(eventId)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
    }

    public List<EventResponse> getEventsByAccount(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }
}

package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.metrics.EventMetrics;
import com.eventledger.gateway.service.EventService;
import com.eventledger.gateway.service.EventSubmissionResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final EventMetrics eventMetrics;

    public EventController(EventService eventService, EventMetrics eventMetrics) {
        this.eventService = eventService;
        this.eventMetrics = eventMetrics;
    }

    @PostMapping
    public ResponseEntity<EventResponse> submitEvent(
            @Valid @RequestBody EventRequest request
    ) {
        EventSubmissionResult result = eventService.submitEvent(request);
        String metricResult = result.created() ? "success" : "duplicate";
        eventMetrics.recordEventReceived(metricResult);
        logger.atInfo()
                .addKeyValue("result", metricResult)
                .log("Event submission completed");
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.event());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable("id") String eventId
    ) {
        EventResponse response = eventService.getEvent(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccount(
            @RequestParam("account") String accountId
    ) {
        List<EventResponse> response = eventService.getEventsByAccount(accountId);
        return ResponseEntity.ok(response);
    }
}

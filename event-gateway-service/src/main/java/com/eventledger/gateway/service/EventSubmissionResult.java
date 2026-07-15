package com.eventledger.gateway.service;

import com.eventledger.gateway.dto.EventResponse;

public record EventSubmissionResult(
        EventResponse event,
        boolean created
) {
}

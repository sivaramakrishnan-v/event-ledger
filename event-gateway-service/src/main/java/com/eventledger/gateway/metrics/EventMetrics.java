package com.eventledger.gateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EventMetrics {

    private static final String EVENTS_RECEIVED_METRIC = "eventledger.gateway.events.received";
    private static final String RESULT_TAG = "result";

    private final MeterRegistry meterRegistry;

    public EventMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordEventReceived(String result) {
        meterRegistry.counter(EVENTS_RECEIVED_METRIC, RESULT_TAG, result).increment();
    }
}

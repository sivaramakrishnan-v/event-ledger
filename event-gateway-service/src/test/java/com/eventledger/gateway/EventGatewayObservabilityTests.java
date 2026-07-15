package com.eventledger.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.micrometer.core.instrument.MeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "account-service.connect-timeout=2s",
                "account-service.read-timeout=2s"
        }
)
@AutoConfigureObservability
class EventGatewayObservabilityTests {

    private static final BlockingQueue<String> TRACEPARENT_HEADERS = new LinkedBlockingQueue<>();
    private static final BlockingQueue<String> REQUEST_BODIES = new LinkedBlockingQueue<>();
    private static final HttpServer ACCOUNT_SERVICE = startAccountService();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private static HttpServer startAccountService() {
        HttpServer accountService;
        try {
            accountService = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start test Account Service", exception);
        }
        accountService.createContext("/accounts/acct-trace/transactions", EventGatewayObservabilityTests::handleTransaction);
        accountService.start();
        return accountService;
    }

    @AfterAll
    static void stopAccountService() {
        ACCOUNT_SERVICE.stop(0);
    }

    @DynamicPropertySource
    static void accountServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("account-service.base-url", () -> "http://127.0.0.1:" + ACCOUNT_SERVICE.getAddress().getPort());
    }

    @BeforeEach
    void resetCapturedHeaders() {
        TRACEPARENT_HEADERS.clear();
        REQUEST_BODIES.clear();
    }

    @Test
    void healthEndpointReportsDatabaseStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"status\":\"UP\"")
                .contains("\"db\":{\"status\":\"UP\"");
    }

    @Test
    void postEventsPropagatesTraceparentAndRecordsSuccessMetric() {
        double before = successMetricCount();
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01");
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "eventId": "evt-trace",
                  "accountId": "acct-trace",
                  "type": "CREDIT",
                  "amount": 150.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T14:02:11Z",
                  "metadata": {
                    "source": "test"
                  }
                }
                """, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/events", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"eventId\":\"evt-trace\"");
        assertThat(REQUEST_BODIES)
                .singleElement()
                .asString()
                .contains("\"eventId\":\"evt-trace\"")
                .contains("\"type\":\"CREDIT\"")
                .contains("\"amount\":150.00")
                .contains("\"currency\":\"USD\"");

        assertThat(TRACEPARENT_HEADERS)
                .singleElement()
                .asString()
                .startsWith("00-" + traceId + "-")
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
        assertThat(successMetricCount()).isEqualTo(before + 1.0);
    }

    private double successMetricCount() {
        return meterRegistry.counter(
                "eventledger.gateway.events.received",
                "result",
                "success"
        ).count();
    }

    private static void handleTransaction(HttpExchange exchange) throws IOException {
        String traceparent = exchange.getRequestHeaders().getFirst("traceparent");
        TRACEPARENT_HEADERS.add(traceparent == null ? "" : traceparent);
        REQUEST_BODIES.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(response);
        }
    }
}

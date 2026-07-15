package com.eventledger.gateway;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.client.AccountTransactionRequest;
import com.eventledger.gateway.entity.EventEntity;
import com.eventledger.gateway.entity.EventType;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "account-service.base-url=http://localhost:1",
        "account-service.connect-timeout=100ms",
        "account-service.read-timeout=100ms"
})
@AutoConfigureMockMvc
class EventGatewayServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @BeforeEach
    void resetDatabase() {
        eventRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void postEventsReturnsServiceUnavailableWhenAccountServiceIsUnreachable() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "evt-unavailable",
                                  "accountId": "acct-unavailable",
                                  "type": "CREDIT",
                                  "amount": 150.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:02:11Z",
                                  "metadata": {
                                    "source": "test"
                                  }
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Account service is unavailable"));
    }

    @Test
    void getEventByIdStillWorksWhenAccountServiceIsUnavailable() throws Exception {
        eventRepository.save(new EventEntity(
                "evt-local",
                "acct-local",
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                null
        ));

        mockMvc.perform(get("/events/evt-local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-local"));
    }

    @Test
    void getEventsByAccountStillWorksWhenAccountServiceIsUnavailable() throws Exception {
        eventRepository.save(new EventEntity(
                "evt-local-list",
                "acct-local-list",
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                null
        ));

        mockMvc.perform(get("/events").param("account", "acct-local-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-local-list"));
    }

    @Test
    void circuitBreakerFallbackThrowsAccountServiceUnavailableException() {
        AccountTransactionRequest request = new AccountTransactionRequest(
                "evt-fallback",
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z")
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                accountServiceClient,
                "fallback",
                "acct-fallback",
                request,
                new IllegalStateException("downstream failed")
        )).isInstanceOf(AccountServiceUnavailableException.class);
    }

    @Test
    void duplicateEventBehaviorRemainsUnchanged() throws Exception {
        eventRepository.save(new EventEntity(
                "evt-duplicate",
                "acct-duplicate",
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                "{\"source\":\"test\"}"
        ));

        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "evt-duplicate",
                                  "accountId": "acct-duplicate",
                                  "type": "CREDIT",
                                  "amount": 150.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:02:11Z",
                                  "metadata": {
                                    "source": "test"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-duplicate"))
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}

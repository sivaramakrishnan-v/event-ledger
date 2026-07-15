package com.eventledger.account;

import com.eventledger.account.dto.AccountTransactionRequest;
import com.eventledger.account.entity.TransactionType;
import com.eventledger.account.exception.AccountConflictException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import com.eventledger.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountCoreFunctionalityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void resetDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void duplicateTransactionWithSamePayloadIsIdempotent() throws Exception {
        String transaction = """
                {
                  "eventId": "evt-idempotent",
                  "type": "CREDIT",
                  "amount": 150.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T14:02:11Z"
                }
                """;

        mockMvc.perform(post("/accounts/acct-idempotent/transactions")
                        .contentType("application/json")
                        .content(transaction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-idempotent"));

        mockMvc.perform(post("/accounts/acct-idempotent/transactions")
                        .contentType("application/json")
                        .content(transaction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-idempotent"));

        assertThat(transactionRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/accounts/acct-idempotent/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void duplicateTransactionWithDifferentPayloadIsRejected() {
        accountService.applyTransaction(
                "acct-conflict",
                new AccountTransactionRequest(
                        "evt-conflict",
                        TransactionType.CREDIT,
                        new BigDecimal("100.00"),
                        "USD",
                        Instant.parse("2026-05-15T14:02:11Z")
                )
        );

        assertThatThrownBy(() -> accountService.applyTransaction(
                "acct-conflict",
                new AccountTransactionRequest(
                        "evt-conflict",
                        TransactionType.CREDIT,
                        new BigDecimal("101.00"),
                        "USD",
                        Instant.parse("2026-05-15T14:02:11Z")
                )
        )).isInstanceOf(AccountConflictException.class)
                .hasMessageContaining("Event ID already exists");

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void outOfOrderTransactionsProduceCorrectBalanceAndChronologicalHistory() throws Exception {
        mockMvc.perform(post("/accounts/acct-out-of-order/transactions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "evt-later-debit",
                                  "type": "DEBIT",
                                  "amount": 30.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:02:11Z"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/accounts/acct-out-of-order/transactions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "evt-earlier-credit",
                                  "type": "CREDIT",
                                  "amount": 100.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T13:02:11Z"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/acct-out-of-order/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00))
                .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(get("/accounts/acct-out-of-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].eventId").value("evt-earlier-credit"))
                .andExpect(jsonPath("$.transactions[1].eventId").value("evt-later-debit"));
    }

    @Test
    void invalidTransactionPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/accounts/acct-validation/transactions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "",
                                  "type": "CREDIT",
                                  "amount": 0,
                                  "currency": "US",
                                  "eventTimestamp": "2026-05-15T14:02:11Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}

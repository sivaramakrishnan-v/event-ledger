package com.eventledger.gateway.client;

import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${account-service.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "fallback")
    public void applyTransaction(
            String accountId,
            AccountTransactionRequest request
    ) {
        restClient.post()
                .uri("/accounts/{accountId}/transactions", accountId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void fallback(String accountId, AccountTransactionRequest request, Throwable t) {
        if (t instanceof RestClientResponseException responseException
                && responseException.getStatusCode().is4xxClientError()) {
            throw responseException;
        }

        throw new AccountServiceUnavailableException("Account service is unavailable", t);
    }
}

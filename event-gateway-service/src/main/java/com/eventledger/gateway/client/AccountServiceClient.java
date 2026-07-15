package com.eventledger.gateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${account-service.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

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
}

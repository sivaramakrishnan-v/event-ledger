package com.eventledger.account.controller;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.AccountTransactionRequest;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody AccountTransactionRequest request
    ) {
        TransactionResponse response = accountService.applyTransaction(accountId, request);
        logger.info("Account transaction applied");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }
}

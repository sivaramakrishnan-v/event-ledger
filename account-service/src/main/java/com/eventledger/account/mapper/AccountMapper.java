package com.eventledger.account.mapper;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.AccountTransactionRequest;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.entity.AccountEntity;
import com.eventledger.account.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountMapper {

    public TransactionEntity toTransactionEntity(
            String accountId,
            AccountTransactionRequest request
    ) {
        return new TransactionEntity(
                request.eventId(),
                accountId,
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp()
        );
    }

    public TransactionResponse toTransactionResponse(
            TransactionEntity entity
    ) {
        return new TransactionResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                entity.getCreatedAt()
        );
    }

    public BalanceResponse toBalanceResponse(
            AccountEntity entity
    ) {
        return new BalanceResponse(
                entity.getAccountId(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getUpdatedAt()
        );
    }

    public AccountResponse toAccountResponse(
            AccountEntity account,
            List<TransactionEntity> transactions
    ) {
        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());

        return new AccountResponse(
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                transactionResponses
        );
    }
}

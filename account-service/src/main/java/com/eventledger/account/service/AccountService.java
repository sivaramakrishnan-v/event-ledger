package com.eventledger.account.service;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.AccountTransactionRequest;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.entity.AccountEntity;
import com.eventledger.account.entity.TransactionEntity;
import com.eventledger.account.exception.AccountConflictException;
import com.eventledger.account.mapper.AccountMapper;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountMapper accountMapper
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public TransactionResponse applyTransaction(
            String accountId,
            AccountTransactionRequest request
    ) {
        Optional<TransactionEntity> existingTransaction = transactionRepository.findByEventId(request.eventId());
        if (existingTransaction.isPresent()) {
            if (matchesExistingTransaction(existingTransaction.get(), accountId, request)) {
                return accountMapper.toTransactionResponse(existingTransaction.get());
            } else {
                throw new AccountConflictException(
                        "Event ID already exists with different transaction data: "
                                + request.eventId()
                );
            }
        }

        AccountEntity account = accountRepository.findByAccountId(accountId)
                .orElseGet(() -> new AccountEntity(accountId, request.currency()));

        if (!account.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new AccountConflictException("Currency mismatch for account: " + accountId);
        }

        switch (request.type()) {
            case CREDIT:
                account.credit(request.amount());
                break;
            case DEBIT:
                account.debit(request.amount());
                break;
        }

        accountRepository.save(account);

        TransactionEntity transactionEntity = accountMapper.toTransactionEntity(accountId, request);
        TransactionEntity savedTransaction = transactionRepository.save(transactionEntity);

        return accountMapper.toTransactionResponse(savedTransaction);
    }

    public BalanceResponse getBalance(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .map(accountMapper::toBalanceResponse)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
    }

    public AccountResponse getAccount(String accountId) {
        AccountEntity account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));

        List<TransactionEntity> transactions = transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId);

        return accountMapper.toAccountResponse(account, transactions);
    }

    private boolean matchesExistingTransaction(
            TransactionEntity existing,
            String accountId,
            AccountTransactionRequest request
    ) {
        return Objects.equals(existing.getAccountId(), accountId) &&
                Objects.equals(existing.getType(), request.type()) &&
                existing.getAmount().compareTo(request.amount()) == 0 &&
                Objects.equals(existing.getCurrency(), request.currency()) &&
                Objects.equals(existing.getEventTimestamp(), request.eventTimestamp());
    }
}

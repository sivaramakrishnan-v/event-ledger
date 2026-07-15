package com.eventledger.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions",
       indexes = {
           @Index(name = "idx_transactionentity_event_id", columnList = "event_id"),
           @Index(name = "idx_transactionentity_account_id", columnList = "accountId"),
           @Index(name = "idx_transactionentity_event_timestamp", columnList = "eventTimestamp")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uc_transactionentity_event_id", columnNames = {"event_id"})
       })
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant eventTimestamp;

    @Column(nullable = false)
    private Instant createdAt;

    protected TransactionEntity() {
    }

    public TransactionEntity(String eventId, String accountId, TransactionType type, BigDecimal amount, String currency, Instant eventTimestamp) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

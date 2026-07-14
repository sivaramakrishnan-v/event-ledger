package com.eventledger.gateway.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_events", uniqueConstraints = {
    @UniqueConstraint(name = "uc_evententity_event_id", columnNames = {"event_id"})
})
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Lob
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EventEntity() {
    }

    public EventEntity(String eventId, String accountId, EventType type, BigDecimal amount, String currency, Instant eventTimestamp, String metadata) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.metadata = metadata;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
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

    public EventType getType() {
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

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

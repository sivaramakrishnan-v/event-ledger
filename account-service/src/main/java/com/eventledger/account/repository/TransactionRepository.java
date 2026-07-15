package com.eventledger.account.repository;

import com.eventledger.account.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<TransactionEntity> findByAccountIdOrderByEventTimestampAsc(String accountId);
}

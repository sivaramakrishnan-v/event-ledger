package com.eventledger.gateway.repository;

import com.eventledger.gateway.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByEventId(String eventId);

    //boolean existsByEventId(String eventId);

    List<EventEntity> findByAccountIdOrderByEventTimestampAsc(String accountId);
}

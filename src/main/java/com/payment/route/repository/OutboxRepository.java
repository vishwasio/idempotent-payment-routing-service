package com.payment.route.repository;

import com.payment.route.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    Optional<OutboxEvent> findFirstByProcessedFalseOrderByCreatedAtAsc();
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
//    List<OutboxEvent> findTop10ByProcessedFalseOrderByCreatedAtAsc();
}

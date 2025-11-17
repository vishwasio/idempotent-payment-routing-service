package com.payment.route.repository;

import com.payment.route.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByClientIdAndIdempotencyKey(String clientId, UUID idempotencyKey);
    Optional<IdempotencyKey> findByTransactionId(Long transactionId);
}


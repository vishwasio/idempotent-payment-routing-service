package com.payment.route.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.route.dto.PaymentRequest;
import com.payment.route.dto.PaymentResponse;
import com.payment.route.model.IdempotencyKey;
import com.payment.route.model.OutboxEvent;
import com.payment.route.model.PaymentTransaction;
import com.payment.route.repository.IdempotencyKeyRepository;
import com.payment.route.repository.OutboxRepository;
import com.payment.route.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String clientId, UUID idempotencyKey) {
        //  Check existing idempotency key
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();
            if (record.getStatus() == IdempotencyKey.Status.COMPLETED) {
                // Already processed — return stored response
                return new PaymentResponse(
                        record.getTransactionId(),
                        "ALREADY_PROCESSED",
                        HttpStatus.OK.value(),
                        record.getResponseBody()
                );
            } else if (record.getStatus() == IdempotencyKey.Status.IN_PROGRESS) {
                // Still being processed
                return new PaymentResponse(
                        record.getTransactionId(),
                        "PROCESSING",
                        HttpStatus.ACCEPTED.value(),
                        "Transaction is being processed"
                );
            } else if (record.getStatus() == IdempotencyKey.Status.FAILED) {
                return new PaymentResponse(
                        record.getTransactionId(),
                        "FAILED",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Previous attempt failed"
                );
            }
        }

        // Create new transaction entry
        PaymentTransaction transaction = PaymentTransaction.builder()
                .clientId(clientId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentTransaction.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        // Create idempotency key record
        IdempotencyKey idempotency = IdempotencyKey.builder()
                .clientId(clientId)
                .idempotencyKey(idempotencyKey)
                .status(IdempotencyKey.Status.IN_PROGRESS)
                .transactionId(transaction.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        idempotencyKeyRepository.save(idempotency);

        // Write Outbox event for async processing
        try {
            String payload = objectMapper.writeValueAsString(request);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("PaymentTransaction")
                    .aggregateId(transaction.getId())
                    .eventType("PAYMENT_CREATED")
                    .payload(payload)
                    .createdAt(LocalDateTime.now())
                    .processed(false)
                    .attempts(0)
                    .build();
            outboxRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing payment request: " + e.getMessage(), e);
        }

        // Return immediate response
        return new PaymentResponse(
                transaction.getId(),
                "ACCEPTED",
                HttpStatus.CREATED.value(),
                "Payment request accepted and queued for processing"
        );
    }

    public Optional<PaymentTransaction> getTransaction(Long id) {
        return transactionRepository.findById(id);
    }
}

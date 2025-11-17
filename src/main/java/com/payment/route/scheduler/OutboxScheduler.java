package com.payment.route.scheduler;

import com.payment.route.model.DeadLetter;
import com.payment.route.model.IdempotencyKey;
import com.payment.route.model.OutboxEvent;
import com.payment.route.model.PaymentTransaction;
import com.payment.route.repository.DeadLetterRepository;
import com.payment.route.repository.IdempotencyKeyRepository;
import com.payment.route.repository.OutboxRepository;
import com.payment.route.repository.PaymentTransactionRepository;
import com.payment.route.service.GatewaySimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final GatewaySimulatorService gatewaySimulatorService;
    private final PaymentTransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final DeadLetterRepository deadLetterRepository;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutboxEvents() {

        Optional<OutboxEvent> eventOpt =
                outboxRepository.findFirstByProcessedFalseOrderByCreatedAtAsc();

        if (eventOpt.isEmpty()) {
            log.debug("⏳ No pending outbox events to process.");
            return;
        }

        OutboxEvent event = eventOpt.get();
        int attempts = event.getAttempts() == null ? 0 : event.getAttempts();

        log.info("🌀 Processing outbox event");
        log.info("🔄 Processing outbox ID={} attempt={}", event.getId(), attempts);

        boolean success = gatewaySimulatorService.simulateGatewayCall(event);

        if (success) {
            handleSuccess(event);
        } else {
            handleFailure(event);
        }
    }

    private void handleSuccess(OutboxEvent event) {

        PaymentTransaction tx =
                transactionRepository.findById(event.getAggregateId()).orElse(null);

        if (tx != null) {
            tx.setStatus(PaymentTransaction.Status.SUCCESS);
            tx.setUpdatedAt(LocalDateTime.now());
            tx.setRetryCount(event.getAttempts());
            tx.setGatewayTransactionId("simulator/local-01");
            transactionRepository.save(tx);
        }

        event.setProcessed(true);
        event.setProcessedAt(LocalDateTime.now());
        outboxRepository.save(event);

        idempotencyKeyRepository.findByTransactionId(event.getAggregateId())
                .ifPresent(key -> {
                    key.setStatus(IdempotencyKey.Status.COMPLETED);
                    key.setResponseCode(200);
                    key.setResponseBody("Payment processed successfully");
                    key.setUpdatedAt(LocalDateTime.now());
                    idempotencyKeyRepository.save(key);
                });

        log.info("✅ Successfully processed event ID={}", event.getId());
    }

    private void handleFailure(OutboxEvent event) {

        int attempts = event.getAttempts() == null ? 0 : event.getAttempts();
        attempts += 1;
        event.setAttempts(attempts);

        int maxAttempts = 3;

        if (attempts < maxAttempts) {
            log.warn("⚠ Retrying event={} (attempt {}/{})", event.getId(), attempts, maxAttempts);
            outboxRepository.save(event);
            return;
        }

        log.error("☠ Event ID={} failed after {} attempts → moving to DLQ", event.getId(), attempts);

        // Move to DLQ
        DeadLetter dl = DeadLetter.builder()
                .eventId(event.getId())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .errorMessage("🚧 Gateway not working, tried ping all " + attempts + " attempts")
                .attempts(attempts)
                .createdAt(event.getCreatedAt())
                .lastAttemptAt(LocalDateTime.now())
                .build();

        deadLetterRepository.save(dl);

        // Delete outbox event after saving it to DLQ
        outboxRepository.delete(event);

        // Mark transaction FAILED
        transactionRepository.findById(event.getAggregateId())
                .ifPresent(tx -> {
                    tx.setStatus(PaymentTransaction.Status.FAILED);
                    tx.setUpdatedAt(LocalDateTime.now());
                    tx.setRetryCount(event.getAttempts());
                    tx.setGatewayTransactionId("simulator/local-01");
                    transactionRepository.save(tx);
                });

        // Mark idempotency FAILED
        idempotencyKeyRepository.findByTransactionId(event.getAggregateId())
                .ifPresent(key -> {
                    key.setStatus(IdempotencyKey.Status.FAILED);
                    key.setResponseCode(500);
                    key.setResponseBody("⛔ Processing failed and moved to DLQ");
                    key.setUpdatedAt(LocalDateTime.now());
                    idempotencyKeyRepository.save(key);
                });
    }
}

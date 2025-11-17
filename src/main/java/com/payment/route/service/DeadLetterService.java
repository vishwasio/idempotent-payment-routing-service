package com.payment.route.service;

import com.payment.route.model.DeadLetter;
import com.payment.route.model.OutboxEvent;
import com.payment.route.repository.DeadLetterRepository;
import com.payment.route.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final DeadLetterRepository deadLetterRepository;
    private final OutboxRepository outboxRepository;

    public List<DeadLetter> findAll() {
        return deadLetterRepository.findAll();
    }

    public Optional<DeadLetter> findById(Long id) {
        return deadLetterRepository.findById(id);
    }

    /**
     * Retry (requeue) a dead letter event back into the outbox.
     * Behavior:
     *  - Loads dead letter entry
     *  - Creates a new OutboxEvent using data from DLQ
     *  - Deletes the DLQ entry only after the Outbox row is saved
     * Returns the newly created OutboxEvent id on success.
     */
    @Transactional
    public Long retryDeadLetter(Long deadLetterId) {
        DeadLetter dlq = deadLetterRepository.findById(deadLetterId)
                .orElseThrow(() -> new IllegalArgumentException("❌ Dead letter id not found: " + deadLetterId));

        OutboxEvent outbox = OutboxEvent.builder()
                .aggregateType(dlq.getAggregateType())
                .aggregateId(dlq.getAggregateId())
                .eventType(dlq.getEventType())
                .payload(dlq.getPayload())
                .createdAt(LocalDateTime.now())
                .attempts(0)
                .processed(false)
                .build();

        OutboxEvent saved = outboxRepository.save(outbox);

        // delete DLQ entry only after successful save to outbox
        deadLetterRepository.deleteById(deadLetterId);

        log.info("🔁 Requeued DLQ id={} -> outbox id={}", deadLetterId, saved.getId());
        return saved.getId();
    }

    @Transactional
    public void deleteDeadLetter(Long deadLetterId) {
        if (!deadLetterRepository.existsById(deadLetterId)) {
            throw new IllegalArgumentException("❌ Dead letter id not found: " + deadLetterId);
        }
        deadLetterRepository.deleteById(deadLetterId);
        log.info("🗑️ Deleted DLQ id={}", deadLetterId);
    }
}

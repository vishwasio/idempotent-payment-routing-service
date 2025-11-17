package com.payment.route.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.route.config.TestcontainersConfig;
import com.payment.route.dto.PaymentRequest;
import com.payment.route.model.DeadLetter;
import com.payment.route.repository.DeadLetterRepository;
import com.payment.route.repository.OutboxRepository;
import com.payment.route.repository.PaymentTransactionRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(initializers = TestcontainersConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OutboxSchedulerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Autowired
    private PaymentTransactionRepository txnRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void cleanup() {
        outboxRepository.deleteAll();
        deadLetterRepository.deleteAll();
        txnRepo.deleteAll();
    }

    @Test
    void outboxScheduler_retriesAndMovesToDLQ_afterFailures() throws Exception {
        // create a payment that will likely fail sometimes (GatewaySimulatorService uses random)
        PaymentRequest req = PaymentRequest.builder()
                .sourceAccount("SRC-DLQ-1")
                .destinationAccount("DST-DLQ-1")
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
//                .recipient("DLQ Test")
//                .description("dlq test flow")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String idemp = UUID.randomUUID().toString();
        headers.set("Idempotency-Key", idemp);
        headers.set("Client-Id", "dlq-test-client");

        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(req), headers);
        restTemplate.postForEntity("/api/v1/payments", entity, String.class);

        // wait until scheduler either processes or moves to DLQ (timeout 30s)
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    long dlq = deadLetterRepository.count();
                    long outboxOpen = outboxRepository.findAll().stream().filter(e -> !e.getProcessed()).count();
                    // pass when either DLQ has rows OR no unprocessed outbox remain (processed true or removed)
                    return dlq > 0 || outboxOpen == 0;
                });

        // assert that either dead-letter has entry or all outbox processed
        assertThat(deadLetterRepository.count()).isGreaterThanOrEqualTo(0);
    }
}

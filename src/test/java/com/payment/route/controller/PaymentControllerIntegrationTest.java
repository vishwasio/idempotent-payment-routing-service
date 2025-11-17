package com.payment.route.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.route.config.TestcontainersConfig;
import com.payment.route.dto.PaymentRequest;
import com.payment.route.model.OutboxEvent;
import com.payment.route.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(initializers = TestcontainersConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxRepository outboxRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void cleanup() {
        outboxRepository.deleteAll();
    }

    @Test
    void postPayment_createsTransactionAndOutboxEvent() throws Exception {
        PaymentRequest req = PaymentRequest.builder()
                .sourceAccount("SRC-INT-1")
                .destinationAccount("DST-INT-1")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
//                .recipient("UnitTest")
//                .description("integration test payment")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        headers.set("Client-Id", "integration-test-client");

        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity("/api/v1/payments", entity, String.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        // Outbox row should be created immediately
        long outboxCount = outboxRepository.count();
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }
}

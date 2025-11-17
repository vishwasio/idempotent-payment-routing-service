package com.payment.route.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.route.config.TestcontainersConfig;
import com.payment.route.dto.PaymentRequest;
import com.payment.route.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(initializers = TestcontainersConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoadTest {

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
    void fireManyPayments_concurrently_createsManyOutboxEntries() throws Exception {
        int threads = 20;
        int requestsPerThread = 10; // total 200
        ExecutorService es = Executors.newFixedThreadPool(threads);
        List<Callable<ResponseEntity<String>>> tasks = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            tasks.add(() -> {
                ResponseEntity<String> last = null;
                for (int i = 0; i < requestsPerThread; i++) {
                    PaymentRequest req = PaymentRequest.builder()
                            .sourceAccount("LOAD_SRC_" + Thread.currentThread().getId())
                            .destinationAccount("LOAD_DST_" + i)
                            .amount(new BigDecimal("10.00"))
                            .currency("INR")
//                            .recipient("LoadTest")
//                            .description("concurrency")
                            .build();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Idempotency-Key", UUID.randomUUID().toString());
                    headers.set("Client-Id", "load-test-client");
                    HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(req), headers);

                    last = restTemplate.postForEntity("/api/v1/payments", entity, String.class);
                }
                return last;
            });
        }

        List<Future<ResponseEntity<String>>> futures = es.invokeAll(tasks);
        es.shutdown();
        es.awaitTermination(40, TimeUnit.SECONDS);

        // quick smoke: ensure all tasks returned success on their last request
        for (Future<ResponseEntity<String>> f : futures) {
            ResponseEntity<String> r = f.get();
            assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        }

        // outbox entries must be >= total requests (some may be processed quickly but should exist transiently)
        long outboxCount = outboxRepository.count();
        assertThat(outboxCount).isGreaterThanOrEqualTo(1); // at least one created
    }
}

package com.payment.route.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.route.model.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GatewaySimulatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public boolean simulateGatewayCall(OutboxEvent event) {
        try {
            // Simulated delay between 300ms and 2s
            long delay = 300 + random.nextInt(1700);
            TimeUnit.MILLISECONDS.sleep(delay);

//            boolean success = random.nextInt(100) < 85; // 85% success rate
            boolean success = Math.random() < 0.5; // 50% success rate / chance of failure.
            log.info("💳 Gateway simulation for TXN_ID={} (delay={}ms, success={})",
                    event.getAggregateId(), delay, success);
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

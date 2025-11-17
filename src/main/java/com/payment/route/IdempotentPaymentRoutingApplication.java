package com.payment.route;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdempotentPaymentRoutingApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdempotentPaymentRoutingApplication.class, args);
	}
}

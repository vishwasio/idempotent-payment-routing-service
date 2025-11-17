package com.payment.route.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;

    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String gatewayTransactionId;

    @Builder.Default
    private Integer retryCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }
}

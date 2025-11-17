package com.payment.route.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"clientId", "idempotencyKey"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;

    @Column(nullable = false)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer responseCode;

    @Lob
    private String responseBody;

    private Long transactionId;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        IN_PROGRESS, COMPLETED, FAILED
    }
}

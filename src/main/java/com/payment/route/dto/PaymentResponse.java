package com.payment.route.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long transactionId;
    private String status;
    private int httpCode;
    private String message;
}

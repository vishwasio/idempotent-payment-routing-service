package com.payment.route.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    @NotNull
    @Min(value = 1, message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String sourceAccount;

    @NotBlank
    private String destinationAccount;
}

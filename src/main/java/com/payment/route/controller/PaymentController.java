package com.payment.route.controller;

import com.payment.route.dto.PaymentRequest;
import com.payment.route.dto.PaymentResponse;
import com.payment.route.model.PaymentTransaction;
import com.payment.route.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Routing API", description = "Endpoints for idempotent payment creation and status retrieval.")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Create a new payment transaction",
            description = """
                    Creates a new payment transaction in an idempotent way.
                    Requires a unique 'Idempotency-Key' header per client to prevent duplicate transactions.
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Payment created successfully",
                            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
                    @ApiResponse(responseCode = "200", description = "Duplicate request (idempotent replay)",
                            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key conflict")
            }
    )
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @Parameter(description = "Unique idempotency key for this client request", required = true)
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Client identifier (default = demo-client)")
            @RequestHeader(value = "Client-Id", defaultValue = "demo-client") String clientId
    ) {
        try {
            UUID key = UUID.fromString(idempotencyKey);
            PaymentResponse response = paymentService.processPayment(request, clientId, key);
            return ResponseEntity.status(response.getHttpCode()).body(response);
        } catch (IllegalArgumentException e) {
            PaymentResponse resp = PaymentResponse.builder()
                    .transactionId(null)
                    .status("INVALID_IDEMPOTENCY_KEY")
                    .httpCode(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid Idempotency-Key format. Must be a valid UUID.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        } catch (Exception e) {
            PaymentResponse resp = PaymentResponse.builder()
                    .transactionId(null)
                    .status("INTERNAL_ERROR")
                    .httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Unexpected error: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    @Operation(
            summary = "Get payment transaction status",
            description = "Fetches the current state of a transaction by its ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment found",
                            content = @Content(schema = @Schema(implementation = PaymentTransaction.class))),
                    @ApiResponse(responseCode = "404", description = "Payment not found")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentStatus(
            @Parameter(description = "Payment transaction ID", required = true)
            @PathVariable Long id
    ) {
        Optional<PaymentTransaction> txn = paymentService.getTransaction(id);
        return txn.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
            summary = "Health check endpoint",
            description = "Used to verify that the Payment Routing API is up and running."
    )
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("✅ Payment Routing Service is running fine!");
    }
}

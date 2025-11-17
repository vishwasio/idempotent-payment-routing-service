package com.payment.route.controller;

import com.payment.route.model.DeadLetter;
import com.payment.route.service.DeadLetterService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dead-letter")
@RequiredArgsConstructor
@Slf4j
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    @GetMapping
    public ResponseEntity<List<DeadLetter>> listAll() {
        return ResponseEntity.ok(deadLetterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeadLetter> getById(@PathVariable("id") @Min(1) Long id) {
        return deadLetterService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/retry/{id}")
    public ResponseEntity<?> retry(@PathVariable("id") @Min(1) Long id) {
        try {
            Long outboxId = deadLetterService.retryDeadLetter(id);
            return ResponseEntity.accepted().body(
                    new SimpleResponse("requeued", "Requeued to outbox with id: " + outboxId)
            );
        } catch (IllegalArgumentException ex) {
            log.warn("🔁 Retry failed for DLQ id={}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(new SimpleResponse("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("❓ Unexpected error retrying DLQ id={}", id, ex);
            return ResponseEntity.internalServerError().body(new SimpleResponse("error", "Internal error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") @Min(1) Long id) {
        try {
            deadLetterService.deleteDeadLetter(id);
            return ResponseEntity.ok(new SimpleResponse("deleted", "Deleted DLQ id: " + id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new SimpleResponse("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("❓ Unexpected error deleting DLQ id={}", id, ex);
            return ResponseEntity.internalServerError().body(new SimpleResponse("error", "Internal error"));
        }
    }

    // Small inner DTO for responses
    private record SimpleResponse(String status, String message) {
    }
}

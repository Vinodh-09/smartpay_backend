package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Processing payment for user: {}", userId);

        try {
            Map<String, Object> result = paymentService.processPayment(userId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            // ✅ ADD THIS LOG (so you can see "Cart is empty" / "Insufficient wallet balance" etc.)
            log.error("Payment failed for user {}: {}", userId, e.getMessage());

            return ResponseEntity.badRequest().body(
                    Map.of("status", "failed", "message", e.getMessage())
            );

        } catch (Exception e) {
            log.error("Unexpected error during payment for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "failed",
                    "message", e.getMessage()
            ));
        }
    }
}
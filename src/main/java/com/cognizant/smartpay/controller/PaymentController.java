<<<<<<< HEAD

=======
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

<<<<<<< HEAD
=======
/**
 * REST Controller for payment operations
 */
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

<<<<<<< HEAD
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Processing payment for user: {}", userId);
=======
    /**
     * Process payment for user's cart
     * POST /api/payment/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");

        log.info("Processing payment request for user: {}", userId);

>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
        try {
            Map<String, Object> result = paymentService.processPayment(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
<<<<<<< HEAD
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "status", "failed"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Payment failed", "status", "failed"));
        }
    }
}
=======
            log.error("Payment validation error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "status", "failed"
            ));
        } catch (Exception e) {
            log.error("Payment processing error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Payment processing failed",
                "status", "failed"
            ));
        }
    }
}
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982

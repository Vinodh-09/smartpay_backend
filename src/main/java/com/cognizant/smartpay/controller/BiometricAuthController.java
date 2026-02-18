package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.dto.*;
import com.cognizant.smartpay.entity.User;
import com.cognizant.smartpay.service.BiometricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for biometric authentication
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")

@RequiredArgsConstructor
//@Slf4j
public class BiometricAuthController {

    private final JdbcTemplate jdbcTemplate;
    private final BiometricService biometricService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BiometricAuthController.class);

    /**
     * Authenticate user with fingerprint
     *
     * POST /api/auth/fingerprint
     */
    @PostMapping("/fingerprint")
    public ResponseEntity<UserDTO> authenticateFingerprint(
            @Valid @RequestBody FingerprintAuthRequest request) {

        log.info("Received fingerprint authentication request");

        try {
            User user = biometricService.authenticateFingerprint(request);

            UserDTO userDTO = convertToDTO(user);

            return ResponseEntity.ok(userDTO);

        } catch (Exception e) {
            log.error("Authentication failed", e);
            throw e;
        }
    }

    /**
     * Enroll new fingerprint
     *
     * POST /api/auth/enroll
     */
    @PostMapping("/enroll")
    public ResponseEntity<EnrollmentResponse> enrollFingerprint(
            @Valid @RequestBody EnrollmentRequest request) {

        log.info("Received enrollment request for user: {}", request);

        EnrollmentResponse response = biometricService.enrollFingerprint(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Register new user with biometric enrollment
     *
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<com.cognizant.smartpay.dto.RegistrationResponse> registerUser(
            @Valid @RequestBody com.cognizant.smartpay.dto.RegistrationRequest request) {

        log.info("Received registration request for: {}", request);

        try {
            User user = biometricService.registerNewUser(request);

            com.cognizant.smartpay.dto.RegistrationResponse response = com.cognizant.smartpay.dto.RegistrationResponse.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .walletBalance(user.getWalletBalance())
                    .biometricEnabled(user.getBiometricEnabled())
                    .status(user.getStatus())
                    .message("User registered successfully")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Registration validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Get all registered credential IDs for WebAuthn authentication
     *
     * GET /api/auth/credentials
     */
    @GetMapping("/credentials")
    public ResponseEntity<?> getCredentials() {
        try {
            java.util.List<String> credentials = biometricService.getAllActiveCredentials();
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            log.error("Failed to get credentials", e);
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    /**
     * Health check endpoint
     *
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("SmartPay Backend is running!");
    }

    /**
     * Convert User entity to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setWalletBalance(user.getWalletBalance());
        dto.setBiometricEnabled(user.getBiometricEnabled());
        dto.setEnabled(user.getEnabled());
        dto.setStatus(user.getStatus());
        System.out.println("-----------------------------------------"+user.getName());
        return dto;
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<String> logoutUser(@PathVariable Long userId) {
        log.info("Received logout request for user ID: {}", userId);
        System.out.println("----------------------------------------------"+userId);

        try {
            biometricService.logoutUser(userId);
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed for user ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }
}

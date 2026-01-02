package com.cognizant.smartpay.service;

<<<<<<< HEAD
import com.twilio.Twilio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
=======
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
<<<<<<< HEAD
 * Service for payment processing with Email and SMS notifications
 */
@Service
=======
 * Service for payment processing
 */
@Service
@Slf4j
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
@RequiredArgsConstructor
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;
<<<<<<< HEAD
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public Map<String, Object> processPayment(Long userId) {
        log.info("Processing payment and notifications for user: {}", userId);

        // 1. Fetch User Contact Details (Using correct column names from your logs)
        String userSql = "SELECT email, phone, name FROM users WHERE user_id = ?";
        Map<String, Object> userData;
        try {
            userData = jdbcTemplate.queryForMap(userSql, userId);
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage());
            throw new IllegalArgumentException("User not found or database error");
        }

        String userEmail = (String) userData.get("email");
        String userPhone = (String) userData.get("phone");
        String userName = (String) userData.get("name");

        // 2. Get user's active cart items
=======

    /**
     * Process payment for user's cart
     */
    @Transactional
    public Map<String, Object> processPayment(Long userId) {
        log.info("Processing payment for user: {}", userId);

        // Get user's cart
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
        String cartSql = """
            SELECT c.cart_id, ci.cart_item_id, ci.product_id, ci.quantity, ci.subtotal, p.selling_price, p.name, p.brand
            FROM cart c
            JOIN cart_items ci ON c.cart_id = ci.cart_id
            JOIN products p ON ci.product_id = p.product_id
            WHERE c.user_id = ? AND c.is_active = 1
            """;
<<<<<<< HEAD

        List<Map<String, Object>> cartItems = jdbcTemplate.query(cartSql, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("cartId", rs.getLong("cart_id"));
=======
        List<Map<String, Object>> cartItems = jdbcTemplate.query(cartSql, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("cartId", rs.getLong("cart_id"));
            item.put("cartItemId", rs.getLong("cart_item_id"));
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
            item.put("productId", rs.getLong("product_id"));
            item.put("quantity", rs.getInt("quantity"));
            item.put("subtotal", rs.getBigDecimal("subtotal"));
            item.put("unitPrice", rs.getBigDecimal("selling_price"));
            item.put("productName", rs.getString("name"));
            item.put("productBrand", rs.getString("brand"));
            return item;
        }, userId);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

<<<<<<< HEAD
        // 3. Calculate total
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> (BigDecimal) item.get("subtotal"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Check wallet balance
        String walletSql = "SELECT balance FROM wallet WHERE user_id = ?";
        BigDecimal walletBalance = jdbcTemplate.queryForObject(walletSql, BigDecimal.class, userId);

        if (walletBalance == null || walletBalance.compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        // 5. Generate Transaction
        Long cartId = (Long) cartItems.get(0).get("cartId");
=======
        // Calculate total
        BigDecimal totalAmount = cartItems.stream()
            .map(item -> (BigDecimal) item.get("subtotal"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check wallet balance
        String walletSql = "SELECT balance FROM wallet WHERE user_id = ?";
        BigDecimal walletBalance = jdbcTemplate.queryForObject(walletSql, BigDecimal.class, userId);

        if (walletBalance.compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        // Get cart ID for transaction
        Long cartId = (Long) cartItems.get(0).get("cartId");

        // Generate transaction reference
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
        String transactionReference = "TXN" + System.currentTimeMillis();

        // Create transaction record
        String transactionSql = """
<<<<<<< HEAD
            INSERT INTO transactions (transaction_reference, user_id, cart_id, total_amount, final_amount, 
            payment_method, payment_status, wallet_balance_before, wallet_balance_after, items_count, transaction_date)
            VALUES (?, ?, ?, ?, ?, 'WALLET', 'SUCCESS', ?, ?, ?, NOW())
            """;

        jdbcTemplate.update(transactionSql,
                transactionReference, userId, cartId, totalAmount, totalAmount,
                walletBalance, walletBalance.subtract(totalAmount), cartItems.size());

        // Get generated ID
        Long transactionId = jdbcTemplate.queryForObject(
                "SELECT transaction_id FROM transactions WHERE transaction_reference = ?", Long.class, transactionReference);

        // 6. Update Inventory and Wallet
        for (Map<String, Object> item : cartItems) {
            // Insert into transaction_items
            jdbcTemplate.update("""
                INSERT INTO transaction_items (transaction_id, product_id, product_name, product_brand, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    transactionId, item.get("productId"), item.get("productName"), item.get("productBrand"),
                    item.get("quantity"), item.get("unitPrice"), item.get("subtotal"));

            // Decrease product stock
            jdbcTemplate.update("UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?",
                    item.get("quantity"), item.get("productId"));
        }

        // Update Wallet
        jdbcTemplate.update("UPDATE wallet SET balance = balance - ? WHERE user_id = ?", totalAmount, userId);

        // 7. Clear Cart
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
        jdbcTemplate.update("UPDATE cart SET is_active = 0 WHERE cart_id = ?", cartId);

        // 8. SEND NOTIFICATIONS
        sendEmailInvoice(userEmail, userName, transactionReference, totalAmount);
       // sendSMSNotification(userPhone, transactionReference, totalAmount);

        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", transactionReference);
        result.put("status", "success");
        result.put("newBalance", walletBalance.subtract(totalAmount));
        result.put("amount", totalAmount);
        result.put("timestamp", LocalDateTime.now().toString());

        return result;
    }

    private void sendEmailInvoice(String toEmail, String name, String txnId, BigDecimal amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Payment Receipt - Cognizant SmartPay");

            String htmlBody = String.format(
                    "<div style='font-family: Arial, sans-serif; border: 1px solid #eee; padding: 20px;'>" +
                            "<h2 style='color: #2e7d32;'>Payment Successful!</h2>" +
                            "<p>Hi %s,</p>" +
                            "<p>Thank you for your purchase. Here is your transaction summary:</p>" +
                            "<table style='width: 100%%; border-collapse: collapse;'>" +
                            "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><strong>Transaction ID:</strong></td><td>%s</td></tr>" +
                            "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><strong>Amount Paid:</strong></td><td style='color: #2e7d32; font-weight: bold;'>₹%.2f</td></tr>" +
                            "</table>" +
                            "<p>The amount has been debited from your SmartPay Wallet.</p>" +
                            "</div>", name, txnId, amount);

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Receipt email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }

//    private void sendSMSNotification(String phone, String txnId, BigDecimal amount) {
//        try {
//            // 1. YOUR CREDENTIALS
//            String ACCOUNT_SID = "ACd9987b88c796e6e576058dd5dd257e45";
//            String AUTH_TOKEN = "ade41fc759855b0d98e50d8fe6717621";
//            String FROM_NUMBER = "+13158733994"; // Your Twilio Number
//
//
//
//            // 2. BYPASS SSL GLOBALLY FOR THIS REQUEST
//            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
//                    new javax.net.ssl.X509TrustManager() {
//                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
//                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                    }
//            };
//            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
//
//            // 3. PREPARE DATA
//            String toPhone = phone.startsWith("+") ? phone : "+91" + phone;
//            String msg = "SmartPay: Payment of Rs." + amount + " successful. Txn: " + txnId;
//            String urlString = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";
//
//            // 4. SEND VIA HTTP POST
//            java.net.URL url = new java.net.URL(urlString);
//            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
//
//            String auth = ACCOUNT_SID.trim() + ":" + AUTH_TOKEN.trim(); // Added .trim() to remove hidden spaces
//            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
//            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
//            conn.setRequestMethod("POST");
//            conn.setDoOutput(true);
//
//            String postData = "To=" + java.net.URLEncoder.encode(toPhone, "UTF-8") +
//                    "&From=" + java.net.URLEncoder.encode(FROM_NUMBER, "UTF-8") +
//                    "&Body=" + java.net.URLEncoder.encode(msg, "UTF-8");
//
//            try (java.io.OutputStream os = conn.getOutputStream()) {
//                os.write(postData.getBytes());
//            }
//
//            if (conn.getResponseCode() == 201 || conn.getResponseCode() == 200) {
//                log.info("SMS Sent Successfully via Native Java! Response: {}", conn.getResponseCode());
//            } else {
//                log.error("Twilio API Error Code: {}", conn.getResponseCode());
//            }
//
//        } catch (Exception e) {
//            log.error("Native SMS Bypass Failed: {}", e.getMessage());
//        }
//    }
=======
            INSERT INTO transactions (transaction_reference, user_id, cart_id, total_amount, final_amount, payment_method, payment_status, wallet_balance_before, wallet_balance_after, items_count, transaction_date)
            VALUES (?, ?, ?, ?, ?, 'WALLET', 'SUCCESS', ?, ?, ?, NOW())
            """;
        jdbcTemplate.update(transactionSql,
            transactionReference,
            userId,
            cartId,
            totalAmount,
            totalAmount,
            walletBalance,
            walletBalance.subtract(totalAmount),
            cartItems.size());

        // Get the transaction ID that was just inserted
        String getTransactionIdSql = "SELECT transaction_id FROM transactions WHERE transaction_reference = ?";
        Long transactionId = jdbcTemplate.queryForObject(getTransactionIdSql, Long.class, transactionReference);

        // Create transaction items
        for (Map<String, Object> item : cartItems) {
            String itemSql = """
                INSERT INTO transaction_items (transaction_id, product_id, product_name, product_brand, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            jdbcTemplate.update(itemSql,
                transactionId,
                item.get("productId"),
                item.get("productName"),
                item.get("productBrand"),
                item.get("quantity"),
                item.get("unitPrice"),
                item.get("subtotal"));
        }

        // Update wallet balance
        String updateWalletSql = "UPDATE wallet SET balance = balance - ? WHERE user_id = ?";
        jdbcTemplate.update(updateWalletSql, totalAmount, userId);

        // Update product stock
        for (Map<String, Object> item : cartItems) {
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
            jdbcTemplate.update(updateStockSql, item.get("quantity"), item.get("productId"));
        }

        // Clear cart
        String clearCartSql = "DELETE FROM cart_items WHERE cart_id = ?";
        jdbcTemplate.update(clearCartSql, cartId);

        String deactivateCartSql = "UPDATE cart SET is_active = 0 WHERE cart_id = ?";
        jdbcTemplate.update(deactivateCartSql, cartId);

        // Get new wallet balance
        BigDecimal newBalance = jdbcTemplate.queryForObject(walletSql, BigDecimal.class, userId);

        log.info("Payment processed successfully for user: {}, transaction: {}, amount: {}",
            userId, transactionReference, totalAmount);

        // Return payment result
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", transactionReference);
        result.put("status", "success");
        result.put("receipt", "Payment receipt generated");
        result.put("newBalance", newBalance);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("amount", totalAmount);

        return result;
    }
>>>>>>> cd0856f68e2a012ba936bb2fdc3fbd15eb442982
}
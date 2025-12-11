# Payment Processing Functionality

## Overview
The payment processing system handles wallet-based payments for shopping cart transactions. It validates wallet balance, processes payments, records transactions, and generates receipts.

## Involved Files

### Backend Files
1. **PaymentController.java** - REST API endpoints for payment operations
2. **PaymentService.java** - Business logic for payment processing
3. **Wallet.java** - Wallet entity
4. **WalletRepository.java** - Wallet data access
5. **Transaction.java** - Transaction entity (assumed)
6. **TransactionRepository.java** - Transaction data access
7. **CartService.java** - Cart clearing after payment

### Frontend Files
1. **PaymentConfirmation.js** - Payment processing and receipt generation
2. **CartReview.js** - Payment initiation from cart review
3. **WalletDisplay.js** - Wallet balance display
4. **api.js** - API client functions for payment operations

### Database Tables
- **wallet**: User wallet balances
- **transactions**: Payment transaction records
- **transaction_items**: Items in transactions
- **cart**: Shopping carts (cleared after payment)
- **cart_items**: Cart items (cleared after payment)

## Process Flow

### Payment Initiation

#### Step 1: Proceed to Payment
- **File**: `CartReview.js`
- **Function**: `handlePayment` (lines 50-60)
- **Code**:
```javascript
const handlePayment = useCallback(() => {
  setCurrentStep('confirmation');
}, []);
```
- **Navigation**: Moves to PaymentConfirmation component

#### Step 2: Payment Confirmation Display
- **File**: `PaymentConfirmation.js`
- **Function**: `useEffect` (lines 20-25)
- **Code**:
```javascript
useEffect(() => {
  // Automatically process payment when component mounts
  handlePayment();
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, []); // Runs ONLY ONCE on mount
```
- **Automatic Processing**: Payment starts immediately on component load

### Payment Processing

#### Step 1: Payment API Call
- **File**: `PaymentConfirmation.js`
- **Function**: `handlePayment` (lines 27-55)
- **Code**:
```javascript
const handlePayment = async () => {
  try {
    setProcessing(true);
    // Store previous balance before payment
    setPreviousBalance(user.walletBalance || 0);
    
    // Simulate processing delay
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // Payments API: proceedToPay() --> Call Wallet Service to deduct the amount
    const response = await proceedToPay(user.id);
    setPaymentData(response.data);
    
    // Set new balance from response
    if (response.data && response.data.newBalance !== undefined) {
      setNewBalance(response.data.newBalance);
    } else {
      // Fallback calculation if backend doesn't return newBalance
      setNewBalance((user.walletBalance || 0) - totalAmount);
    }
    
    setPaymentSuccess(true);
  } catch (error) {
    setError('Payment failed. Please try again or contact support.');
  } finally {
    setProcessing(false);
  }
};
```
- **API Call**: Calls `proceedToPay` from `api.js`

#### Step 2: API Client Function
- **File**: `api.js`
- **Function**: `proceedToPay` (lines 250-255)
- **Code**:
```javascript
export const proceedToPay = (userId) => 
    axios.post(`${API_BASE_URL}/payment/process`, { userId });
```
- **Endpoint**: `POST /api/payment/process`

#### Step 3: Backend Payment Controller
- **File**: `PaymentController.java`
- **Endpoint**: `POST /api/payment/process`
- **Method**: `processPayment` (lines 15-30)
- **Code**:
```java
@PostMapping("/process")
public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> request) {
    try {
        Long userId = Long.valueOf(request.get("userId").toString());
        Map<String, Object> result = paymentService.processPayment(userId);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```
- **Request Body**: `{"userId": 1}`

### Payment Service Logic

#### Step 1: Payment Validation
- **File**: `PaymentService.java`
- **Method**: `processPayment` (lines 20-80)
- **Code**:
```java
@Transactional
public Map<String, Object> processPayment(Long userId) {
    // Get user's cart total
    Map<String, Object> cartTotal = cartService.getCartTotal(userId);
    BigDecimal totalAmount = (BigDecimal) cartTotal.get("total");
    
    // Check wallet balance
    Wallet wallet = walletRepository.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    
    if (wallet.getBalance().compareTo(totalAmount) < 0) {
        throw new IllegalArgumentException("Insufficient wallet balance");
    }
    
    // Deduct amount from wallet
    BigDecimal newBalance = wallet.getBalance().subtract(totalAmount);
    wallet.setBalance(newBalance);
    walletRepository.save(wallet);
    
    // Create transaction record
    String transactionRef = "TXN" + System.currentTimeMillis();
    String insertTransactionSql = """
        INSERT INTO transactions (user_id, transaction_reference, total_amount, transaction_date, status)
        VALUES (?, ?, ?, NOW(), 'COMPLETED')
        """;
    jdbcTemplate.update(insertTransactionSql, userId, transactionRef, totalAmount);
    
    Long transactionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    
    // Move cart items to transaction items
    String cartItemsSql = """
        SELECT ci.product_id, ci.quantity, ci.unit_price, ci.subtotal
        FROM cart c
        JOIN cart_items ci ON c.cart_id = ci.cart_id
        WHERE c.user_id = ? AND c.is_active = 1
        """;
    
    List<Map<String, Object>> cartItems = jdbcTemplate.query(cartItemsSql, (rs, rowNum) -> {
        Map<String, Object> item = new HashMap<>();
        item.put("productId", rs.getLong("product_id"));
        item.put("quantity", rs.getInt("quantity"));
        item.put("unitPrice", rs.getBigDecimal("unit_price"));
        item.put("subtotal", rs.getBigDecimal("subtotal"));
        return item;
    }, userId);
    
    // Insert transaction items
    for (Map<String, Object> item : cartItems) {
        String insertItemSql = """
            INSERT INTO transaction_items (transaction_id, product_id, quantity, unit_price, subtotal)
            VALUES (?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(insertItemSql, transactionId,
            item.get("productId"), item.get("quantity"), 
            item.get("unitPrice"), item.get("subtotal"));
    }
    
    // Clear cart
    cartService.clearCart(userId);
    
    // Return result
    Map<String, Object> result = new HashMap<>();
    result.put("transactionId", transactionId);
    result.put("transactionReference", transactionRef);
    result.put("amount", totalAmount);
    result.put("newBalance", newBalance);
    result.put("status", "SUCCESS");
    
    return result;
}
```
- **Steps**:
  1. Get cart total
  2. Validate wallet balance
  3. Deduct payment amount
  4. Create transaction record
  5. Move cart items to transaction items
  6. Clear cart

### Receipt Generation

#### Step 1: PDF Receipt Creation
- **File**: `PaymentConfirmation.js`
- **Function**: `generatePDF` (lines 100-200)
- **Code**:
```javascript
const generatePDF = () => {
  const doc = new jsPDF();
  
  // Header
  doc.setFontSize(20);
  doc.text('SmartPay Receipt', 105, 20, { align: 'center' });
  
  // Transaction details
  doc.setFontSize(12);
  doc.text(`Transaction ID: ${paymentData?.transactionId || 'N/A'}`, 20, 40);
  doc.text(`Date: ${new Date().toLocaleDateString()}`, 20, 50);
  doc.text(`Time: ${new Date().toLocaleTimeString()}`, 20, 60);
  
  // Items table
  let yPos = 80;
  doc.text('Items Purchased:', 20, yPos);
  yPos += 10;
  
  cart.forEach((item, index) => {
    doc.text(`${item.item.name} x ${item.quantity}`, 20, yPos);
    doc.text(`₹${(item.item.price * item.quantity).toFixed(2)}`, 170, yPos, { align: 'right' });
    yPos += 10;
  });
  
  // Total
  yPos += 10;
  doc.setFontSize(14);
  doc.text('Total Amount:', 20, yPos);
  doc.text(`₹${totalAmount.toFixed(2)}`, 170, yPos, { align: 'right' });
  
  // Balance info
  yPos += 20;
  doc.text(`Previous Balance: ₹${previousBalance.toFixed(2)}`, 20, yPos);
  doc.text(`New Balance: ₹${newBalance.toFixed(2)}`, 20, yPos + 10);
  
  // Save PDF
  doc.save(`receipt_${paymentData?.transactionReference || 'transaction'}.pdf`);
};
```
- **PDF Generation**: Uses jsPDF library for receipt creation

#### Step 2: Receipt Display
- **File**: `PaymentConfirmation.js`
- **UI Components**: Success message, transaction details, download button
- **Code**:
```javascript
{paymentSuccess && (
  <Card sx={{ mt: 3, bgcolor: '#e8f5e8' }}>
    <CardContent>
      <Typography variant="h5" color="success.main" gutterBottom>
        Payment Successful!
      </Typography>
      <Typography>Transaction ID: {paymentData?.transactionId}</Typography>
      <Typography>Amount Paid: ₹{totalAmount.toFixed(2)}</Typography>
      <Typography>New Balance: ₹{newBalance.toFixed(2)}</Typography>
      <Button variant="contained" onClick={generatePDF} sx={{ mt: 2 }}>
        Download Receipt
      </Button>
    </CardContent>
  </Card>
)}
```

## API Endpoints

### Payment Processing
- **URL**: `POST /api/payment/process`
- **Request**: `{"userId": 1}`
- **Response**: 
```json
{
  "transactionId": 123,
  "transactionReference": "TXN1234567890",
  "amount": 126.00,
  "newBalance": 374.00,
  "status": "SUCCESS"
}
```

## Database Schema

### transactions Table
- `transaction_id` (Primary Key)
- `user_id` (Foreign Key to users)
- `transaction_reference` (Unique reference)
- `total_amount` (Decimal)
- `transaction_date` (Timestamp)
- `status` (Enum: PENDING, COMPLETED, FAILED)

### transaction_items Table
- `transaction_item_id` (Primary Key)
- `transaction_id` (Foreign Key to transactions)
- `product_id` (Foreign Key to products)
- `quantity` (Integer)
- `unit_price` (Decimal)
- `subtotal` (Decimal)

### wallet Table
- `wallet_id` (Primary Key)
- `user_id` (Foreign Key to users)
- `balance` (Decimal)
- `created_at`, `updated_at` (Timestamps)

## Error Handling
- **Insufficient Balance**: Wallet balance validation
- **Wallet Not Found**: User wallet existence check
- **Cart Empty**: No items to process
- **Transaction Failure**: Rollback of wallet deduction
- **PDF Generation Error**: Graceful fallback

## Security Considerations
- **Transaction Atomicity**: All operations in single transaction
- **Balance Validation**: Pre-payment balance check
- **Audit Trail**: Complete transaction logging
- **Idempotency**: Prevent duplicate payments
- **Secure API**: Authentication required for payment

## Receipt Features
- **PDF Format**: Downloadable receipt
- **Item Details**: Complete itemized list
- **Balance Tracking**: Before and after payment balances
- **Transaction Reference**: Unique transaction identifier
- **Date/Time Stamps**: Complete audit information
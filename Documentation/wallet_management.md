# Wallet Management Functionality

## Overview
The wallet management system handles user wallet balance tracking, payment deductions, and balance display throughout the shopping experience.

## Involved Files

### Backend Files
1. **Wallet.java** - Wallet entity
2. **WalletRepository.java** - Wallet data access
3. **PaymentService.java** - Balance deduction logic
4. **UserService.java** - Wallet creation during registration

### Frontend Files
1. **WalletDisplay.js** - Wallet balance display
2. **PaymentConfirmation.js** - Balance updates after payment
3. **WelcomeKiosk.js** - Initial balance display
4. **api.js** - API client for wallet operations

### Database Tables
- **wallet**: User wallet balances
- **wallet_transactions**: Transaction history (future)

## Process Flow

### Wallet Creation

#### Step 1: User Registration
- **File**: `UserService.java`
- **Method**: `registerUser` (lines 35-45)
- **Code**:
```java
// Create wallet for user
Wallet wallet = new Wallet();
wallet.setUser(savedUser);
wallet.setBalance(new BigDecimal("500.00")); // Default balance
walletRepository.save(wallet);
```
- **Default Balance**: ₹500.00 for new users

#### Step 2: Wallet Entity
- **File**: `Wallet.java`
- **Entity Mapping**:
```java
@Entity
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal balance;
    
    // Getters and setters
}
```

### Balance Display

#### Step 1: Wallet Display Component
- **File**: `WalletDisplay.js`
- **Component**: `WalletDisplay` (lines 10-60)
- **Code**:
```javascript
const WalletDisplay = ({ user, onContinueShopping }) => {
  return (
    <Box sx={{ textAlign: 'center', p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Welcome, {user.name}!
      </Typography>
      
      <Card sx={{ maxWidth: 400, mx: 'auto', mt: 3, bgcolor: '#000048', color: 'white' }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Wallet Balance
          </Typography>
          <Typography variant="h3" sx={{ fontWeight: 'bold' }}>
            ₹{user.walletBalance?.toFixed(2) || '0.00'}
          </Typography>
        </CardContent>
      </Card>
      
      <Button 
        variant="contained" 
        size="large" 
        onClick={onContinueShopping}
        sx={{ mt: 3, bgcolor: '#000048', '&:hover': { bgcolor: '#000080' } }}
      >
        Start Shopping
      </Button>
    </Box>
  );
};
```
- **Display**: User greeting and current balance

#### Step 2: Balance Updates
- **File**: `PaymentConfirmation.js`
- **State Management**: `newBalance` state (lines 15-20)
- **Code**:
```javascript
const [newBalance, setNewBalance] = useState(0);

// After payment
if (response.data && response.data.newBalance !== undefined) {
  setNewBalance(response.data.newBalance);
} else {
  // Fallback calculation
  setNewBalance((user.walletBalance || 0) - totalAmount);
}
```
- **Real-time Updates**: Balance updated after successful payment

### Payment Deduction

#### Step 1: Balance Validation
- **File**: `PaymentService.java`
- **Method**: `processPayment` (lines 25-35)
- **Code**:
```java
// Check wallet balance
Wallet wallet = walletRepository.findByUserId(userId)
    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

if (wallet.getBalance().compareTo(totalAmount) < 0) {
    throw new IllegalArgumentException("Insufficient wallet balance");
}
```
- **Validation**: Ensure sufficient balance before payment

#### Step 2: Balance Deduction
- **File**: `PaymentService.java`
- **Method**: `processPayment` (lines 35-40)
- **Code**:
// Deduct amount from wallet
BigDecimal newBalance = wallet.getBalance().subtract(totalAmount);
wallet.setBalance(newBalance);
walletRepository.save(wallet);
```
- **Atomic Operation**: Balance update within transaction

#### Step 3: Return New Balance
- **File**: `PaymentService.java`
- **Method**: `processPayment` (lines 70-80)
- **Code**:
```java
// Return result
Map<String, Object> result = new HashMap<>();
result.put("transactionId", transactionId);
result.put("transactionReference", transactionRef);
result.put("amount", totalAmount);
result.put("newBalance", newBalance);
result.put("status", "SUCCESS");

return result;
```
- **Response**: Includes updated balance for frontend

### Wallet API Operations

#### Step 1: Get Wallet Balance
- **File**: `api.js`
- **Function**: `getWalletBalance` (assumed)
- **Code**:
```javascript
export const getWalletBalance = (userId) => 
    axios.get(`${API_BASE_URL}/wallet/${userId}/balance`);
```
- **Purpose**: Retrieve current wallet balance

#### Step 2: Backend Wallet Controller
- **File**: `WalletController.java` (assumed)
- **Endpoint**: `GET /api/wallet/{userId}/balance`
- **Code**:
```java
@GetMapping("/{userId}/balance")
public ResponseEntity<?> getWalletBalance(@PathVariable Long userId) {
    Wallet wallet = walletRepository.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    
    return ResponseEntity.ok(Map.of("balance", wallet.getBalance()));
}
```

## Database Schema

### wallet Table
- `wallet_id` (Primary Key)
- `user_id` (Foreign Key to users, Unique)
- `balance` (Decimal, precision 10, scale 2)
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

### Future: wallet_transactions Table
- `transaction_id` (Primary Key)
- `wallet_id` (Foreign Key to wallet)
- `amount` (Decimal)
- `type` (Enum: CREDIT, DEBIT)
- `description` (String)
- `transaction_date` (Timestamp)

## Error Handling
- **Insufficient Balance**: Payment validation
- **Wallet Not Found**: User wallet existence check
- **Concurrent Access**: Transaction-level locking
- **Negative Balance**: Prevent invalid states

## Security Considerations
- **Balance Validation**: Pre-payment checks
- **Transaction Atomicity**: All wallet operations in transactions
- **Audit Trail**: Balance change logging
- **Access Control**: User-specific wallet access

## User Experience Features
- **Balance Display**: Prominent balance visibility
- **Real-time Updates**: Immediate balance changes
- **Payment Validation**: Clear insufficient balance messages
- **Transaction History**: Future transaction tracking
- **Default Balance**: New user onboarding

## Future Enhancements
- **Top-up Functionality**: Add money to wallet
- **Transaction History**: Complete transaction log
- **Balance Alerts**: Low balance notifications
- **Multi-currency Support**: Different currencies
- **Wallet Transfer**: User-to-user transfers
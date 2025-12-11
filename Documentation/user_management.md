# User Management Functionality

## Overview
The user management system handles user registration, profile management, and user data persistence. It supports both biometric and manual user registration processes.

## Involved Files

### Backend Files
1. **UserController.java** - REST API endpoints for user operations
2. **UserService.java** - Business logic for user management
3. **User.java** - User entity
4. **UserRepository.java** - User data access
5. **Wallet.java** - Wallet entity (created with user)
6. **WalletRepository.java** - Wallet data access

### Frontend Files
1. **UserRegistration.js** - User registration form
2. **WelcomeKiosk.js** - User selection/login
3. **WalletDisplay.js** - User profile display
4. **api.js** - API client functions for user operations

### Database Tables
- **users**: User account information
- **wallet**: User wallet balances (auto-created)

## Process Flow

### User Registration

#### Step 1: Registration Form
- **File**: `UserRegistration.js`
- **Function**: `handleSubmit` (lines 30-60)
- **Code**:
```javascript
const handleSubmit = async (e) => {
  e.preventDefault();
  try {
    setLoading(true);
    const userData = {
      name: formData.name,
      email: formData.email,
      phone: formData.phone
    };
    
    const response = await registerUser(userData);
    setSuccess('User registered successfully!');
    
    // Optionally proceed to biometric registration
    if (formData.registerBiometric) {
      await handleBiometricRegistration(response.data.userId);
    }
  } catch (error) {
    setError('Registration failed. Please try again.');
  } finally {
    setLoading(false);
  }
};
```
- **Form Fields**: Name, email, phone, biometric option

#### Step 2: API Registration Call
- **File**: `api.js`
- **Function**: `registerUser` (lines 50-55)
- **Code**:
```javascript
export const registerUser = (userData) => 
    axios.post(`${API_BASE_URL}/users/register`, userData);
```
- **Endpoint**: `POST /api/users/register`

#### Step 3: Backend User Controller
- **File**: `UserController.java`
- **Endpoint**: `POST /api/users/register`
- **Method**: `registerUser` (lines 20-35)
- **Code**:
```java
@PostMapping("/register")
public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
    try {
        UserDTO user = userService.registerUser(request);
        return ResponseEntity.ok(Map.of("success", true, "user", user));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```
- **Request Body**: `RegistrationRequest` DTO

#### Step 4: User Service Registration
- **File**: `UserService.java`
- **Method**: `registerUser` (lines 20-50)
- **Code**:
```java
@Transactional
public UserDTO registerUser(RegistrationRequest request) {
    // Check if user already exists
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("User already exists with this email");
    }
    
    // Create user entity
    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPhone(request.getPhone());
    user.setEnabled(true);
    user.setCreatedAt(LocalDateTime.now());
    
    User savedUser = userRepository.save(user);
    
    // Create wallet for user
    Wallet wallet = new Wallet();
    wallet.setUser(savedUser);
    wallet.setBalance(new BigDecimal("500.00")); // Default balance
    walletRepository.save(wallet);
    
    // Convert to DTO
    UserDTO userDTO = new UserDTO();
    userDTO.setId(savedUser.getId());
    userDTO.setName(savedUser.getName());
    userDTO.setEmail(savedUser.getEmail());
    userDTO.setEnabled(savedUser.getEnabled());
    
    return userDTO;
}
```
- **Operations**: User creation, duplicate check, wallet initialization

### User Login/Selection

#### Step 1: User Selection
- **File**: `WelcomeKiosk.js`
- **Function**: `handleUserSelect` (lines 40-50)
- **Code**:
```javascript
const handleUserSelect = async (selectedUser) => {
  try {
    setLoading(true);
    // Set user in application state
    onLogin(selectedUser);
  } catch (error) {
    setError('Failed to select user');
  } finally {
    setLoading(false);
  }
};
```
- **User List**: Pre-defined users for demo purposes

#### Step 2: Biometric Login
- **File**: `WelcomeKiosk.js`
- **Function**: `handleBiometricLogin` (lines 80-100)
- **Code**:
```javascript
const handleBiometricLogin = async () => {
  try {
    setLoading(true);
    // Capture fingerprint for authentication
    const biometricData = await authenticateFingerprint();
    // Send to backend for verification
    const response = await authenticateBiometric(biometricData);
    // Set user session
    onLogin(response.data.user);
  } catch (error) {
    setError('Biometric authentication failed');
  } finally {
    setLoading(false);
  }
};
```
- **Biometric Flow**: Fingerprint capture → backend verification → user login

### User Profile Display

#### Step 1: Wallet Display
- **File**: `WalletDisplay.js`
- **Function**: `WalletDisplay` component (lines 10-50)
- **Code**:
```javascript
const WalletDisplay = ({ user, onContinueShopping }) => {
  return (
    <Box>
      <Typography variant="h4">Welcome, {user.name}!</Typography>
      <Typography>Email: {user.email}</Typography>
      <Typography>Wallet Balance: ₹{user.walletBalance?.toFixed(2) || '0.00'}</Typography>
      <Button variant="contained" onClick={onContinueShopping}>
        Start Shopping
      </Button>
    </Box>
  );
};
```
- **Display**: User name, email, wallet balance

#### Step 2: Balance Updates
- **File**: `WalletDisplay.js`
- **Function**: Real-time balance updates after payments
- **Props**: `user` object with updated `walletBalance`

## API Endpoints

### User Registration
- **URL**: `POST /api/users/register`
- **Request**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890"
}
```
- **Response**:
```json
{
  "success": true,
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "enabled": true
  }
}
```

### Get User Details
- **URL**: `GET /api/users/{userId}`
- **Response**: User details with wallet balance

## Database Schema

### users Table
- `user_id` (Primary Key)
- `name` (String)
- `email` (String, Unique)
- `phone` (String)
- `enabled` (Boolean)
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

### wallet Table
- `wallet_id` (Primary Key)
- `user_id` (Foreign Key to users)
- `balance` (Decimal)
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

## Error Handling
- **Duplicate Email**: Registration validation
- **User Not Found**: Login validation
- **Invalid Data**: Form validation
- **Database Errors**: Transaction rollback

## Security Considerations
- **Email Uniqueness**: Prevents duplicate accounts
- **Data Validation**: Input sanitization
- **Wallet Initialization**: Secure default balance assignment
- **Session Management**: User state tracking

## User Experience Features
- **Form Validation**: Real-time input validation
- **Loading States**: UI feedback during operations
- **Error Messages**: Clear error communication
- **Success Feedback**: Confirmation messages
- **Biometric Integration**: Seamless biometric registration flow
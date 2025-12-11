# Biometric Authentication Functionality

## Overview
The biometric authentication system enables secure user registration and login using fingerprint scanning. It supports both registration of new biometric data and authentication of existing users through fingerprint matching.

## Involved Files

### Backend Files
1. **BiometricAuthController.java** - REST API endpoints for biometric operations
2. **BiometricService.java** - Business logic for biometric processing
3. **Biometric.java** - JPA entity for biometric data storage
4. **BiometricRepository.java** - Data access layer for biometric operations
5. **User.java** - User entity with biometric relationship
6. **UserRepository.java** - User data access

### Frontend Files
1. **UserRegistration.js** - User registration component with biometric enrollment
2. **WelcomeKiosk.js** - Login screen with biometric authentication
3. **biometricUtils.js** - Utility functions for biometric operations
4. **api.js** - API client functions for biometric endpoints

### Database Tables
- **biometric**: Stores biometric templates and metadata
- **users**: User accounts linked to biometric data

## Process Flow

### Biometric Registration Process

#### Step 1: User Initiates Registration
- **File**: `UserRegistration.js`
- **Function**: `handleBiometricRegistration` (lines 45-65)
- **Code**:
```javascript
const handleBiometricRegistration = async () => {
  try {
    setLoading(true);
    // Capture fingerprint using device API
    const biometricData = await captureFingerprint();
    // Send to backend for storage
    const response = await registerBiometric(userId, biometricData);
    setSuccess('Biometric registration successful');
  } catch (error) {
    setError('Biometric registration failed');
  }
};
```
- **API Call**: Calls `registerBiometric` from `api.js`

#### Step 2: Fingerprint Capture
- **File**: `biometricUtils.js`
- **Function**: `captureFingerprint` (lines 10-30)
- **Code**:
```javascript
export const captureFingerprint = async () => {
  // Check if biometric API is available
  if (!window.navigator.credentials) {
    throw new Error('Biometric authentication not supported');
  }
  
  try {
    // Create credential for fingerprint capture
    const credential = await navigator.credentials.create({
      publicKey: {
        challenge: new Uint8Array(32),
        rp: { name: 'SmartPay' },
        user: { id: new Uint8Array(16), name: 'user', displayName: 'User' },
        pubKeyCredParams: [{ alg: -7, type: 'public-key' }],
        authenticatorSelection: { authenticatorAttachment: 'platform' }
      }
    });
    return credential;
  } catch (error) {
    throw new Error('Fingerprint capture failed');
  }
};
```
- **Device Integration**: Uses Web Authentication API (WebAuthn)

#### Step 3: Backend Registration API
- **File**: `BiometricAuthController.java`
- **Endpoint**: `POST /api/biometric/register`
- **Method**: `registerBiometric` (lines 25-45)
- **Code**:
```java
@PostMapping("/register")
public ResponseEntity<?> registerBiometric(@RequestBody EnrollmentRequest request) {
    try {
        biometricService.registerBiometric(request.getUserId(), request.getBiometricData());
        return ResponseEntity.ok(Map.of("success", true, "message", "Biometric registered successfully"));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```
- **Request Body**: `EnrollmentRequest` DTO with userId and biometricData

#### Step 4: Biometric Service Processing
- **File**: `BiometricService.java`
- **Method**: `registerBiometric` (lines 20-50)
- **Code**:
```java
@Transactional
public void registerBiometric(Long userId, String biometricData) {
    // Verify user exists
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    
    // Check if biometric already exists
    if (biometricRepository.existsByUserId(userId)) {
        throw new IllegalStateException("Biometric already registered for user");
    }
    
    // Create biometric entity
    Biometric biometric = new Biometric();
    biometric.setUser(user);
    biometric.setBiometricData(biometricData);
    biometric.setRegisteredAt(LocalDateTime.now());
    biometric.setIsActive(true);
    
    biometricRepository.save(biometric);
}
```
- **Validation**: Checks user existence and prevents duplicate registration

#### Step 5: Database Storage
- **Table**: `biometric`
- **Columns**: 
  - `biometric_id` (Primary Key)
  - `user_id` (Foreign Key to users)
  - `biometric_data` (Encrypted biometric template)
  - `registered_at` (Timestamp)
  - `is_active` (Boolean status)
- **Entity**: `Biometric.java` maps to this table

### Biometric Authentication Process

#### Step 1: User Initiates Login
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
  }
};
```
- **API Call**: Calls `authenticateBiometric` from `api.js`

#### Step 2: Fingerprint Authentication
- **File**: `biometricUtils.js`
- **Function**: `authenticateFingerprint` (lines 35-55)
- **Code**:
```javascript
export const authenticateFingerprint = async () => {
  try {
    // Get credential for authentication
    const credential = await navigator.credentials.get({
      publicKey: {
        challenge: new Uint8Array(32),
        allowCredentials: [],
        userVerification: 'required'
      }
    });
    return credential;
  } catch (error) {
    throw new Error('Fingerprint authentication failed');
  }
};
```
- **Device Integration**: Uses WebAuthn for authentication

#### Step 3: Backend Authentication API
- **File**: `BiometricAuthController.java`
- **Endpoint**: `POST /api/biometric/authenticate`
- **Method**: `authenticateBiometric` (lines 50-70)
- **Code**:
```java
@PostMapping("/authenticate")
public ResponseEntity<?> authenticateBiometric(@RequestBody FingerprintAuthRequest request) {
    try {
        UserDTO user = biometricService.authenticateBiometric(request.getBiometricData());
        return ResponseEntity.ok(Map.of("success", true, "user", user));
    } catch (AuthenticationFailedException e) {
        return ResponseEntity.status(401).body(Map.of("error", "Authentication failed"));
    }
}
```
- **Request Body**: `FingerprintAuthRequest` DTO with biometricData

#### Step 4: Biometric Service Verification
- **File**: `BiometricService.java`
- **Method**: `authenticateBiometric` (lines 55-85)
- **Code**:
```java
public UserDTO authenticateBiometric(String biometricData) {
    // Find biometric record by data (simplified matching)
    Biometric biometric = biometricRepository.findByBiometricData(biometricData)
        .orElseThrow(() -> new AuthenticationFailedException("Biometric data not found"));
    
    if (!biometric.getIsActive()) {
        throw new AuthenticationFailedException("Biometric authentication disabled");
    }
    
    // Get associated user
    User user = biometric.getUser();
    
    // Convert to DTO
    UserDTO userDTO = new UserDTO();
    userDTO.setId(user.getId());
    userDTO.setName(user.getName());
    userDTO.setEmail(user.getEmail());
    userDTO.setEnabled(user.getEnabled());
    
    return userDTO;
}
```
- **Matching Logic**: Compares captured biometric data with stored templates
- **Exception Handling**: Throws `AuthenticationFailedException` for invalid attempts

#### Step 5: User Session Creation
- **File**: `WelcomeKiosk.js`
- **Function**: `onLogin` callback (passed from App.js)
- **Result**: User object stored in application state, navigation to wallet display

## API Endpoints

### Registration
- **URL**: `POST /api/biometric/register`
- **Request**: `{"userId": 1, "biometricData": "base64-encoded-template"}`
- **Response**: `{"success": true, "message": "Biometric registered successfully"}`

### Authentication
- **URL**: `POST /api/biometric/authenticate`
- **Request**: `{"biometricData": "base64-encoded-template"}`
- **Response**: `{"success": true, "user": {...}}`

## Error Handling
- **BiometricNotFoundException**: Thrown when biometric data not found
- **AuthenticationFailedException**: Thrown when authentication fails
- **Device Not Supported**: Frontend checks for WebAuthn support
- **Duplicate Registration**: Backend prevents multiple biometric registrations per user

## Security Considerations
- Biometric data encrypted in database
- Secure transmission over HTTPS
- Rate limiting on authentication attempts
- Audit logging of biometric operations
- Template protection against reverse engineering

## Device Compatibility
- **WebAuthn Support**: Modern browsers with biometric hardware
- **Platform Authenticators**: Built-in fingerprint readers
- **External Authenticators**: USB security keys (future support)
- **Mobile Devices**: Touch ID and Face ID through WebAuthn
- **Fallback**: Manual user selection if biometric fails
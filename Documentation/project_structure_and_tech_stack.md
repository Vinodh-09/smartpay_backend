# SmartPay Application - Project Structure and Tech Stack

## Overview
SmartPay is a RFID-based shopping application with biometric authentication, wallet payments, and seamless checkout experience. The application consists of a Spring Boot backend, React frontend, and MySQL database.

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: MySQL 8.0
- **ORM**: Hibernate/JPA
- **Build Tool**: Maven
- **Security**: Spring Security (for future enhancements)
- **API**: RESTful APIs with JSON

### Frontend
- **Framework**: React 18
- **UI Library**: Material-UI (MUI)
- **State Management**: React Hooks (useState, useEffect)
- **HTTP Client**: Axios
- **PDF Generation**: jsPDF
- **Build Tool**: Create React App
- **Styling**: Material-UI theme system

### Database
- **RDBMS**: MySQL
- **Schema**: Normalized relational schema
- **Key Tables**: users, products, rfid_tags, cart, cart_items, transactions, transaction_items, wallet, biometric

### Infrastructure
- **Web Server**: Tomcat (embedded in Spring Boot)
- **Development Server**: Node.js development server
- **Version Control**: Git
- **IDE**: VS Code

## Device Support and Process Flows

### Supported Devices
1. **RFID Scanners**: For product identification during shopping
2. **Fingerprint Scanners**: For biometric authentication
3. **Mobile Devices**: PWA support for handheld shopping experience
4. **Kiosk Systems**: Touch-screen interfaces for self-service

### Process Flows by Device

#### RFID Scanner Integration
- **Device**: RFID reader hardware
- **Process**:
  1. RFID tag scanned → Frontend detects via hardware interface
  2. Tag data sent to backend via `/api/cart/add-by-rfid`
  3. Backend queries `rfid_tags` table to get `product_id`
  4. Product added to user's cart in `cart` and `cart_items` tables
  5. Cart total updated with price calculations

#### Fingerprint Scanner Integration
- **Device**: Biometric fingerprint scanner
- **Registration Process**:
  1. User initiates registration → Frontend calls device API
  2. Fingerprint captured → Converted to template via biometric library
  3. Template sent to backend via `/api/biometric/register`
  4. Stored in `biometric` table linked to user
- **Authentication Process**:
  1. User taps fingerprint → Device captures biometric data
  2. Template compared with stored data via `/api/biometric/authenticate`
  3. On match, user logged in and session created

#### Mobile/PWA Shopping Experience
- **Device**: Smartphone or tablet with touch interface
- **Process**:
  1. User accesses PWA → Service worker enables offline capabilities
  2. Manual product addition via UI buttons
  3. Cart management with quantity controls
  4. Payment via wallet deduction
  5. Receipt generation and download

#### Kiosk Touch-Screen Systems
- **Device**: Large touch-screen kiosks
- **Process**:
  1. Welcome screen with user identification
  2. Biometric login or manual user selection
  3. RFID scanning or manual product selection
  4. Cart review and payment confirmation
  5. Receipt printing and session cleanup

## Application Architecture

### Backend Architecture
- **Controller Layer**: REST endpoints (`*Controller.java`)
- **Service Layer**: Business logic (`*Service.java`)
- **Repository Layer**: Data access (JPA repositories)
- **Entity Layer**: Database mappings (`*Entity.java`)
- **Configuration**: CORS, security configs

### Frontend Architecture
- **Components**: Reusable UI components (`*.js` in components/)
- **Pages**: Main application screens
- **API Layer**: HTTP client functions (`api.js`)
- **Utils**: Helper functions (`utils/`)
- **Assets**: Static files (`public/`)

### Database Schema Relationships
- `users` → `biometric` (1:1), `wallet` (1:1), `cart` (1:many)
- `products` → `rfid_tags` (1:1)
- `cart` → `cart_items` (1:many)
- `transactions` → `transaction_items` (1:many)

## Key Integration Points

### Biometric Integration
- **Frontend Files**: `UserRegistration.js`, `WelcomeKiosk.js`, `biometricUtils.js`
- **Backend Files**: `BiometricAuthController.java`, `BiometricService.java`
- **Database**: `biometric` table
- **Device APIs**: Browser Web APIs or native device interfaces

### RFID Integration
- **Frontend Files**: `ShoppingHandheld.js`
- **Backend Files**: `CartController.java`, `CartService.java`
- **Database**: `rfid_tags`, `products`, `cart`, `cart_items`
- **Hardware**: RFID reader devices

### Payment Integration
- **Frontend Files**: `PaymentConfirmation.js`, `WalletDisplay.js`
- **Backend Files**: `PaymentController.java`, `PaymentService.java`
- **Database**: `wallet`, `transactions`, `transaction_items`
- **Processing**: Wallet balance deduction and transaction recording

## Development and Deployment

### Development Environment
- **Backend**: `mvnw spring-boot:run`
- **Frontend**: `npm start`
- **Database**: MySQL server running locally
- **Ports**: Backend (8080), Frontend (3000)

### Build Process
- **Backend**: Maven compile and package
- **Frontend**: npm build for production
- **Combined**: Frontend build copied to backend resources

### Testing
- **Unit Tests**: JUnit for backend services
- **Integration Tests**: API endpoint testing
- **UI Tests**: Manual testing with mock data

## Security Considerations
- Biometric data encryption in database
- Secure API endpoints with authentication
- Wallet balance validation before payments
- Session management for user state
- CORS configuration for cross-origin requests

## Performance Optimizations
- Optimistic UI updates for cart operations
- Lazy loading of product data
- Database indexing on frequently queried columns
- React memoization for component re-renders
- Background sync for cart operations

## Technology Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **Web Framework**: Spring Web (REST APIs)
- **ORM**: Spring Data JPA with Hibernate
- **Database**: MySQL 8.4.4 with MySQL Connector/J
- **Validation**: Spring Boot Starter Validation
- **Biometric Auth**: Yubico WebAuthn Server Library 2.5.0
- **Cryptography**: Bouncy Castle 1.77
- **JSON Processing**: Jackson
- **Code Generation**: Lombok
- **Development**: Spring Boot DevTools
- **Testing**: Spring Boot Starter Test with JUnit

### Frontend
- **Library**: React 18.2.0
- **Build Tool**: Create React App (React Scripts 5.0.1)
- **UI Framework**: Material-UI (MUI) 5.14.0 with Emotion styling
- **HTTP Client**: Axios 1.4.0
- **PDF Generation**: jsPDF 3.0.4
- **Linting**: ESLint with React app configuration
- **PWA**: Service Worker, Web App Manifest

### Database
- **RDBMS**: MySQL 8.4.4
- **Character Set**: UTF8MB4 with Unicode collation
- **Features**: Foreign key constraints, indexes, triggers, and views

### Development Environment
- **OS**: Windows (PowerShell)
- **IDE**: Visual Studio Code
- **Version Control**: Git (repository: invisiblepayments_frontend, branch: Dec05)

## Architecture Patterns
- **Layered Architecture**: Clear separation between presentation, business logic, and data access layers
- **RESTful APIs**: Stateless HTTP APIs for frontend-backend communication
- **Entity-Relationship Model**: Normalized database schema with proper relationships
- **Progressive Web App**: Offline-capable frontend with native app-like experience
- **Biometric Security**: WebAuthn standard for secure authentication

## Key Features
- User registration and biometric enrollment
- Fingerprint-based authentication
- Product browsing and shopping cart management
- Wallet-based payment system
- Transaction history and receipts
- RFID tag integration for quick identification
- System logging and audit trails
- PWA for mobile and desktop usage

This structure ensures maintainability, scalability, and security for the SmartPay biometric payment system.
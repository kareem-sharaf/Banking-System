# Banking System - Code Summary

## Overview

This is a comprehensive Spring Boot 3.x banking system built with Java 17, implementing modern banking operations including account management, transactions, interest calculations, notifications, and customer support. The system follows clean architecture principles with proper separation of concerns.

## Architecture & Technology Stack

### Core Technologies
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: Keycloak OAuth2 integration
- **Build Tool**: Maven
- **Logging**: SLF4J with Logback

### Design Patterns Implemented
- **Observer Pattern**: For account event notifications
- **Composite Pattern**: For interest calculations and notification handling (recently added)
- **Repository Pattern**: Data access layer
- **DTO Pattern**: Data transfer objects
- **Factory Pattern**: Service instantiation

## Class Hierarchies and Relationships

### Exception Hierarchy
```
RuntimeException (Java)
├── AccountNotFoundException
├── InsufficientBalanceException
├── InvalidAccountStateException
├── InterestCalculationException
├── NoSuitableStrategyException
└── GlobalExceptionHandler (@RestControllerAdvice)
    ├── handleAccountNotFoundException()
    ├── handleInsufficientBalanceException()
    ├── handleInvalidAccountStateException()
    ├── handleInterestCalculationException()
    ├── handleValidationExceptions()
    └── handleGenericException()
```

### Design Pattern Interfaces and Implementations

#### Interest Calculator Hierarchy (Strategy + Composite Pattern)
```
InterestCalculator (Interface)
├── CompositeInterestCalculator (Composite Pattern)
│   ├── addCalculator()
│   ├── removeCalculator()
│   ├── getCalculators()
│   └── calculateInterest() [sums all child calculators]
├── SavingsInterestCalculator (Leaf)
├── CheckingInterestCalculator (Leaf)
├── LoanInterestCalculator (Leaf)
├── InvestmentInterestCalculator (Leaf)
├── BonusInterestCalculator (Leaf - Recently Added)
│   └── High balance and loyalty bonuses
└── PenaltyInterestCalculator (Leaf - Recently Added)
    └── Low balance and inactivity penalties
```

#### Observer Pattern Hierarchy
```
AccountSubject (Interface)
└── AccountSubjectManager (Concrete Subject)
    ├── attach()
    ├── detach()
    ├── notifyObservers()
    ├── attachUrgentNotificationHandler() [Composite]
    └── attachStandardNotificationHandler() [Composite]

AccountObserver (Interface)
├── CompositeNotificationHandler (Composite Pattern)
│   ├── addHandler()
│   ├── removeHandler()
│   ├── getHandlers()
│   └── update() [delegates to all child handlers]
├── EmailNotifier (Concrete Observer)
├── SMSNotifier (Concrete Observer)
├── InAppNotifier (Concrete Observer)
└── AuditLogger (Concrete Observer)
```

### Service Layer Dependencies

#### AccountService Dependencies
```
AccountService
├── AccountRepository (Data Access)
├── TransactionRepository (Data Access)
├── AccountSubjectManager (Observer Pattern)
└── Dependent Operations:
    ├── deposit() → TransactionService.createTransaction()
    ├── withdraw() → TransactionService.createTransaction()
    └── transfer() → TransactionService.createTransaction()
```

#### InterestCalculationService Dependencies
```
InterestCalculationService
├── AccountRepository
├── InterestCalculationRepository
├── List<InterestCalculator> [Strategy Pattern]
├── BonusInterestCalculator [Composite Component]
├── PenaltyInterestCalculator [Composite Component]
├── AccountSubjectManager [Observer Pattern]
└── Operations:
    ├── calculateDailyInterest() [Scheduled @ 2 AM]
    ├── calculateInterestForAccount()
    └── findSuitableCalculator() [Creates Composite]
```

### Entity Relationships (JPA/Hibernate)

#### Core Entity Relationships
```
User (Auth Module)
└── Customer (1:1)
    ├── Accounts (1:N)
    │   ├── Transactions (From) (1:N)
    │   ├── Transactions (To) (1:N)
    │   ├── InterestCalculations (1:N)
    │   ├── Notifications (1:N)
    │   └── AuditLogs (1:N)
    └── SupportTickets (1:N)

Account
├── ParentAccount (Self-referencing N:1)
├── ChildAccounts (Self-referencing 1:N)
├── AccountType (N:1)
└── Customer (N:1)

Transaction
├── FromAccount (N:1)
├── ToAccount (N:1)
└── AuditLogs (1:N)
```

## Module Structure

### 1. Core Module (`com.banking.core`)
Central module containing shared entities, enums, exceptions, and utilities.

#### Key Components:
- **Enums**: Account states, transaction types, notification priorities, etc.
- **Exceptions**: Custom exception hierarchy with global exception handling
- **Entities**: Audit logs, notifications
- **Controllers**: Audit log management, notification management

### Controller Hierarchy
```
@RestController (Spring)
├── AccountController
│   ├── @PostMapping("/{accountNumber}/deposit")
│   ├── @PostMapping("/{accountNumber}/withdraw")
│   ├── @PostMapping("/transfer")
│   └── @GetMapping("/{accountNumber}")
├── AccountTypeController
├── InterestCalculationController
├── AuthController
│   ├── @PostMapping("/register")
│   ├── @PostMapping("/login")
│   ├── @PostMapping("/refresh")
│   └── @GetMapping("/validate")
├── UserController
├── RoleController
├── PermissionController
├── CustomerController
├── TransactionController
├── ScheduledTransactionController
├── SupportTicketController
├── ReportController
├── NotificationController
└── AuditLogController
```

### Repository Layer (Data Access Pattern)
```
JpaRepository (Spring Data)
├── AccountRepository
│   ├── findByState(AccountState state)
│   ├── findByCustomerId(Long customerId)
│   └── findByAccountNumber(String accountNumber)
├── CustomerRepository
├── TransactionRepository
│   ├── findByFromAccount(Account account)
│   ├── findByToAccount(Account account)
│   └── findByTransactionDateBetween(...)
├── InterestCalculationRepository
├── SupportTicketRepository
├── ReportRepository
├── NotificationRepository
├── AuditLogRepository
├── UserRepository
├── RoleRepository
├── PermissionRepository
├── RolePermissionRepository
└── RefreshTokenRepository
```

### DTO Layer (Data Transfer Objects)
```
Base DTOs
├── DepositRequest
├── WithdrawalRequest
├── TransferRequest
├── TransactionResponse
├── AuthResponse
├── LoginRequest
├── RegisterRequest
├── InterestCalculationResponse
├── InterestCalculationSummary
└── TransferRequest
```

### 2. Authentication Module (`com.banking.core.auth`)
Handles user authentication and authorization using Keycloak.

#### Key Components:
- **Entities**:
  ```
  User
  ├── Role (N:1)
  ├── Permissions (N:N via RolePermission)
  └── LoginHistory (1:N)
  ```
- **Services**:
  ```
  KeycloakAdminService (Keycloak Admin API)
  KeycloakTokenService (JWT handling)
  UserService (User management)
  ```
- **Controllers**: AuthController, UserController, RoleController, PermissionController

#### Key Features:
- JWT token-based authentication
- Role-based access control (Customer, Teller, Manager, Admin)
- User registration and login
- Permission management
- Login history tracking

### 3. Customer Module (`com.banking.customer`)
Customer management and profile operations.

#### Key Features:
- Customer registration and profile management
- Customer status tracking (Active, Suspended, Closed)
- Account associations

### Service Layer Architecture

#### Service Dependencies and Relationships
```
@Service Classes
├── AccountService
│   ├── AccountRepository
│   ├── TransactionRepository
│   ├── AccountSubjectManager
│   └── TransactionService (calls)
├── InterestCalculationService
│   ├── AccountRepository
│   ├── InterestCalculationRepository
│   ├── List<InterestCalculator>
│   ├── BonusInterestCalculator
│   ├── PenaltyInterestCalculator
│   └── AccountSubjectManager
├── TransactionService
│   ├── TransactionRepository
│   ├── AccountRepository
│   └── AccountSubjectManager
├── UserService
│   ├── UserRepository
│   ├── KeycloakAdminService
│   └── KeycloakTokenService
├── CustomerService
│   ├── CustomerRepository
│   └── AccountRepository
├── NotificationService
├── SupportTicketService
└── ReportService
```

#### Configuration Classes
```
@Configuration Classes
├── SecurityConfig
│   ├── @Order(1) publicSecurityFilterChain()
│   └── @Order(2) securityFilterChain()
├── ObserverConfig
│   └── @Bean AccountSubjectManager
├── SchedulerConfig
│   └── @Bean TaskScheduler
├── DataSeeder
│   └── CommandLineRunner for initial data
└── KeycloakSecurityProperties
    └── External configuration properties
```

### 4. Account Module (`com.banking.account`)
Core banking operations for account management.

#### Service Architecture:
```
Account Module Services
├── AccountService (Main business logic)
│   ├── deposit() → creates Transaction + notifies observers
│   ├── withdraw() → creates Transaction + notifies observers
│   └── transfer() → coordinates multi-account transaction
├── InterestCalculationService (Composite Pattern)
│   ├── calculateDailyInterest() [@Scheduled cron]
│   ├── calculateInterestForAccount()
│   └── findSuitableCalculator() [Factory for Composites]
└── Interest Calculators (Strategy Pattern)
    ├── CompositeInterestCalculator (New)
    ├── SavingsInterestCalculator
    ├── CheckingInterestCalculator
    ├── LoanInterestCalculator
    ├── InvestmentInterestCalculator
    ├── BonusInterestCalculator (New)
    └── PenaltyInterestCalculator (New)
```

#### Key Features:
- Account creation and management
- Deposit and withdrawal operations
- **Interest Calculation System** (with Composite Pattern):
  - Base interest calculators (Savings, Checking, Loan, Investment)
  - Bonus interest calculator (high balance + loyalty bonuses)
  - Penalty interest calculator (low balance + inactivity penalties)
  - Composite calculator combining multiple strategies
- Account state management (Active, Frozen, Closed)

#### Recent Composite Pattern Implementation:
- `CompositeInterestCalculator`: Combines multiple calculation strategies
- `BonusInterestCalculator`: Adds bonus interest for high balances and loyalty
- `PenaltyInterestCalculator`: Applies penalties for low balances and inactivity

### 5. Transaction Module (`com.banking.transaction`)
Transaction processing and management.

#### Key Features:
- Transaction types: Deposit, Withdrawal, Transfer, Fee
- Transaction statuses: Pending, Completed, Failed, Cancelled
- Scheduled transactions for recurring payments
- Transaction validation and processing
- Audit trail for all transactions

### 6. Support Module (`com.banking.support`)
Customer support ticket system.

#### Key Features:
- Support ticket creation and management
- Ticket categories and priorities
- Ticket status tracking (Open, In Progress, Resolved, Closed)
- Customer support interactions

### 7. Report Module (`com.banking.report`)
Reporting and analytics functionality.

#### Key Features:
- Report generation (PDF, Excel formats)
- Account statements
- Transaction history reports
- Financial summaries

### 8. Notification Module (`com.banking.core.notification`)
Multi-channel notification system with Observer pattern.

#### Key Features:
- **Notification Handlers**: Email, SMS, In-App notifications
- **Composite Notification Handler**: Groups multiple notification channels
- Event-driven notifications for account activities
- Notification priority management

#### Recent Composite Pattern Implementation:
- `CompositeNotificationHandler`: Combines multiple notification channels
- Factory methods for urgent vs. standard notifications

## Security Implementation

### Authentication & Authorization
- **Keycloak Integration**: OAuth2 resource server configuration
- **JWT Tokens**: Stateless authentication
- **Role-Based Access Control**: Method-level security with `@PreAuthorize`
- **CORS Configuration**: Cross-origin resource sharing setup
- **Session Management**: Stateless sessions

### Security Configuration Layers
1. **Public Endpoints**: Registration, login, health checks (no JWT required)
2. **Protected Endpoints**: All banking operations (JWT required)
3. **Role-Based Permissions**: Different access levels for different user roles

## Database Design

### Entity Relationship Diagram (ERD)

#### Core Business Entities
```
User (Authentication)
├── id (PK)
├── username (Unique)
├── email (Unique)
├── password (Hashed)
├── firstName
├── lastName
├── roles (Many-to-Many)
└── enabled

Customer (Profile)
├── id (PK)
├── user_id (FK → User, Unique)
├── customerNumber (Unique)
├── joinDate
├── status (Active/Suspended/Closed)
├── createdAt
├── updatedAt
└── accounts (One-to-Many)

Account (Banking)
├── id (PK)
├── accountNumber (Unique)
├── accountType_id (FK → AccountType)
├── customer_id (FK → Customer)
├── parentAccount_id (FK → Account, Self-reference)
├── state (Active/Frozen/Closed)
├── balance (Decimal)
├── currency (USD/EUR/GBP)
├── interestRate (Decimal)
├── lastInterestCalculation (Date)
├── openedDate
├── closedDate
├── createdAt
├── updatedAt
└── Relationships:
    ├── childAccounts (One-to-Many, self)
    ├── fromTransactions (One-to-Many)
    ├── toTransactions (One-to-Many)
    ├── interestCalculations (One-to-Many)
    ├── notifications (One-to-Many)
    └── auditLogs (One-to-Many)

AccountType (Reference)
├── id (PK)
├── code (SAVINGS/CHECKING/LOAN/INVESTMENT)
├── name
├── description
└── isActive

Transaction (Financial)
├── id (PK)
├── transactionNumber (Unique)
├── fromAccount_id (FK → Account)
├── toAccount_id (FK → Account, Nullable)
├── transactionType (DEPOSIT/WITHDRAWAL/TRANSFER/FEE)
├── amount (Decimal)
├── currency
├── transactionDate
├── description
├── status (PENDING/COMPLETED/FAILED/CANCELLED)
├── createdAt
├── updatedAt
└── auditLogs (One-to-Many)
```

#### Support & Notification Entities
```
SupportTicket
├── id (PK)
├── customer_id (FK → Customer)
├── title
├── description
├── category (TECHNICAL/ACCOUNT/BILLING)
├── priority (LOW/MEDIUM/HIGH/URGENT)
├── status (OPEN/IN_PROGRESS/RESOLVED/CLOSED)
├── createdAt
├── updatedAt
└── assignedTo (User reference)

Notification (Communication)
├── id (PK)
├── user_id (FK → User)
├── account_id (FK → Account, Nullable)
├── notificationType (SYSTEM_NOTIFICATION/ALERT/REMINDER)
├── title
├── message
├── priority (LOW/MEDIUM/HIGH/URGENT)
├── isRead
├── createdDate
└── readDate
```

#### Audit & Reporting Entities
```
AuditLog
├── id (PK)
├── user_id (FK → User, Nullable)
├── account_id (FK → Account, Nullable)
├── transaction_id (FK → Transaction, Nullable)
├── actionType (LOGIN/LOGOUT/ACCOUNT_ACCESS/TRANSACTION)
├── actionDescription
├── ipAddress
├── userAgent
├── timestamp
└── metadata (JSON)

InterestCalculation (Business Logic)
├── id (PK)
├── account_id (FK → Account)
├── interestAmount
├── calculationDate
├── strategyUsed (Calculator class name)
├── status (SUCCESS/FAILED)
├── previousBalance
├── newBalance
├── interestRate
├── calculationDurationMs
├── errorMessage (if failed)
├── createdAt
└── Relationships to Account

Report (Analytics)
├── id (PK)
├── name
├── type (ACCOUNT_STATEMENT/TRANSACTION_HISTORY)
├── format (PDF/EXCEL/CSV)
├── parameters (JSON)
├── generatedBy (User reference)
├── generatedAt
├── filePath
└── status
```

### Database Constraints & Indexes

#### Unique Constraints
- User: username, email
- Customer: user_id, customerNumber
- Account: accountNumber
- Transaction: transactionNumber
- SupportTicket: (auto-generated)

#### Foreign Key Constraints
- Customer.user_id → User.id (CASCADE)
- Account.customer_id → Customer.id (RESTRICT)
- Account.accountType_id → AccountType.id (RESTRICT)
- Account.parentAccount_id → Account.id (SET NULL)
- Transaction.fromAccount_id → Account.id (RESTRICT)
- Transaction.toAccount_id → Account.id (SET NULL)
- All audit relationships → respective entities (SET NULL)

#### Performance Indexes
```sql
-- Account lookups
CREATE INDEX idx_account_number ON accounts(account_number);
CREATE INDEX idx_customer_id ON accounts(customer_id);
CREATE INDEX idx_account_state ON accounts(state);

-- Transaction queries
CREATE INDEX idx_transaction_number ON transactions(transaction_number);
CREATE INDEX idx_from_account_id ON transactions(from_account_id);
CREATE INDEX idx_to_account_id ON transactions(to_account_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_transaction_status ON transactions(status);

-- Customer relationships
CREATE INDEX idx_customer_user_id ON customers(user_id);
CREATE INDEX idx_customer_number ON customers(customer_number);

-- Audit trail
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_account_id ON audit_logs(account_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
```

### Database Features
- **Indexing**: Optimized queries with proper indexes on frequently accessed fields
- **Audit Fields**: Created/updated timestamps on all entities with @PrePersist/@PreUpdate
- **Soft Deletes**: Logical deletion using status fields where applicable
- **Constraints**: Data integrity with foreign keys and unique constraints
- **JPA Features**: Lazy loading, cascading operations, entity relationships

## API Design

### REST API Structure
- **Base Path**: `/api`
- **Versioning**: Not implemented (can be added as `/api/v1`)
- **HTTP Methods**: Standard REST operations (GET, POST, PUT, DELETE)
- **Response Format**: JSON with consistent structure
- **Error Handling**: Global exception handler with proper HTTP status codes

### Key API Endpoints

#### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Token refresh

#### Account Operations
- `POST /api/accounts/{accountNumber}/deposit` - Deposit money
- `POST /api/accounts/{accountNumber}/withdraw` - Withdraw money
- `POST /api/accounts/transfer` - Transfer between accounts
- `GET /api/accounts/{accountNumber}` - Get account details
- `GET /api/accounts/customer/{customerId}` - Get customer accounts

#### Transaction History
- `GET /api/transactions/account/{accountNumber}` - Account transactions
- `GET /api/transactions/{transactionNumber}` - Transaction details

#### Interest Calculations
- `POST /api/accounts/{accountNumber}/interest/calculate` - Manual interest calculation
- `GET /api/accounts/interest/summary` - Interest calculation summary

## Design Patterns & Best Practices

### Observer Pattern Implementation
- **Subject**: `AccountSubjectManager` manages account observers
- **Observers**: `EmailNotifier`, `SMSNotifier`, `InAppNotifier`, `AuditLogger`
- **Events**: Account activities trigger notifications to all registered observers

### Composite Pattern Implementation (Recently Added)
- **Interest Calculations**: Combine base + bonus + penalty calculators
- **Notifications**: Group multiple notification channels for different scenarios

### Additional Patterns
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Clean data transfer between layers
- **Builder Pattern**: Complex object construction (AccountEvent)
- **Strategy Pattern**: Different interest calculation strategies

## Configuration & Setup

### Application Configuration
- **Profiles**: Development, production environment support
- **Externalized Configuration**: Properties for database, Keycloak, etc.
- **Bean Configuration**: Custom service configurations

### Scheduler Configuration
- **Interest Calculation**: Daily interest calculation at 2 AM
- **Scheduled Transactions**: Automated recurring transactions

## Recent Enhancements

### Composite Pattern Integration
1. **Interest Calculation System**:
   - `CompositeInterestCalculator` for combining calculation strategies
   - `BonusInterestCalculator` for loyalty and high-balance bonuses
   - `PenaltyInterestCalculator` for low-balance and inactivity penalties
   - Updated `InterestCalculationService` to use composite calculators

2. **Notification System**:
   - `CompositeNotificationHandler` for multi-channel notifications
   - Factory methods for urgent vs. standard notification combinations
   - Enhanced `AccountSubjectManager` with composite handler support

## Performance Considerations

### Database Optimization
- **Indexing Strategy**: Proper indexes on frequently queried fields
- **Lazy Loading**: JPA lazy loading for related entities
- **Query Optimization**: Efficient repository queries

### Application Performance
- **Caching**: Spring caching where applicable
- **Async Processing**: Non-blocking operations for notifications
- **Connection Pooling**: Database connection optimization

## Monitoring & Logging

### Logging Strategy
- **SLF4J**: Unified logging interface
- **Structured Logging**: Consistent log formats across the application
- **Log Levels**: DEBUG, INFO, WARN, ERROR appropriately used

### Audit Trail
- **Audit Logs**: All critical operations logged
- **Transaction History**: Complete transaction audit trail
- **User Activity**: Login history and user actions tracking

## Future Enhancements

### Potential Improvements
- **API Versioning**: Version management for API evolution
- **Microservices**: Potential split into separate services
- **Event Sourcing**: Complete event-driven architecture
- **API Gateway**: Centralized API management
- **Distributed Caching**: Redis integration for performance
- **Message Queue**: Async processing with RabbitMQ/Kafka

## Package Structure and Dependencies

### Complete Package Hierarchy
```
com.banking
├── BankingSystemApplication.java
├── config/
│   ├── DataSeeder.java
│   ├── KeycloakSecurityProperties.java
│   ├── ObserverConfig.java
│   ├── SchedulerConfig.java
│   └── SecurityConfig.java
├── core/
│   ├── auth/
│   │   ├── controller/ (AuthController, UserController, etc.)
│   │   ├── dto/ (AuthResponse, LoginRequest, etc.)
│   │   ├── module/entity/ (User, Role, Permission, etc.)
│   │   ├── repository/ (UserRepository, RoleRepository, etc.)
│   │   └── service/ (KeycloakAdminService, UserService)
│   ├── controller/ (AuditLogController)
│   ├── enums/ (AccountEventType, AccountState, etc.)
│   ├── exception/ (GlobalExceptionHandler, custom exceptions)
│   ├── module/entity/ (AuditLog)
│   ├── notification/
│   │   ├── controller/ (NotificationController)
│   │   └── module/entity/ (Notification)
│   └── repository/ (AuditLogRepository)
├── customer/
│   ├── controller/ (CustomerController)
│   ├── module/entity/ (Customer)
│   └── repository/ (CustomerRepository)
├── account/
│   ├── controller/ (AccountController, etc.)
│   ├── dto/ (DepositRequest, WithdrawalRequest, etc.)
│   ├── module/entity/ (Account, AccountType, InterestCalculation)
│   ├── repository/ (AccountRepository, etc.)
│   └── service/
│       ├── AccountService.java
│       ├── interest/
│       │   ├── InterestCalculationService.java
│       │   └── calculator/ (All InterestCalculator implementations)
│       └── notification/
│           ├── AccountEvent.java
│           ├── AccountObserver.java
│           ├── AccountSubject.java
│           ├── AccountSubjectManager.java
│           └── handler/ (All AccountObserver implementations)
├── transaction/
│   ├── controller/ (TransactionController, ScheduledTransactionController)
│   ├── dto/ (TransactionResponse, TransferRequest)
│   ├── module/entity/ (Transaction, ScheduledTransaction)
│   └── repository/ (TransactionRepository, ScheduledTransactionRepository)
├── support/
│   ├── controller/ (SupportTicketController)
│   ├── module/entity/ (SupportTicket)
│   └── repository/ (SupportTicketRepository)
└── report/
    ├── controller/ (ReportController)
    ├── module/entity/ (Report)
    └── repository/ (ReportRepository)
```

### Dependency Flow Architecture

#### Layer Dependencies (Clean Architecture)
```
Presentation Layer (Controllers)
    ↓ HTTP Requests/Responses
Service Layer (Business Logic)
    ↓ Business Rules & Orchestration
Repository Layer (Data Access)
    ↓ SQL/NoSQL Queries
Database Layer (Persistence)
```

#### Module Interdependencies
```
Core Module (Shared)
    ↙     ↘
Auth ← Customer ← Account → Transaction
    ↙         ↙      ↘
Security  Interest   Support
           Calc.     Tickets
              ↓        ↓
         Notification Reports
```

#### Key Dependency Relationships
```
AccountController
├── AccountService (business logic)
├── AccountSubjectManager (notifications)
└── DTOs (data transfer)

AccountService
├── AccountRepository (data access)
├── TransactionRepository (cross-module)
├── AccountSubjectManager (observer pattern)
└── TransactionService (creates transactions)

InterestCalculationService
├── List<InterestCalculator> (strategy pattern)
├── CompositeInterestCalculator (new feature)
├── AccountRepository
└── InterestCalculationRepository

AccountSubjectManager
├── List<AccountObserver> (observer pattern)
├── CompositeNotificationHandler (new feature)
└── AccountRepository (for account-specific observers)
```

## Design Patterns Implementation Details

### Strategy Pattern (Interest Calculators)
```java
// Interface
public interface InterestCalculator {
    BigDecimal calculateInterest(Account account, LocalDate date);
    boolean supports(AccountType type);
    String getCalculatorName();
    BigDecimal getDefaultInterestRate();
}

// Concrete Implementations
@Component
public class SavingsInterestCalculator implements InterestCalculator {
    // Implementation for savings accounts
}

@Component
public class CheckingInterestCalculator implements InterestCalculator {
    // Implementation for checking accounts
}
```

### Composite Pattern (Interest & Notifications)
```java
// Composite Interest Calculator
@Component
public class CompositeInterestCalculator implements InterestCalculator {
    private final List<InterestCalculator> calculators = new ArrayList<>();

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate date) {
        return calculators.stream()
                .map(calc -> calc.calculateInterest(account, date))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// Composite Notification Handler
@Component
public class CompositeNotificationHandler implements AccountObserver {
    private final List<AccountObserver> handlers = new ArrayList<>();

    @Override
    public void update(AccountEvent event) {
        handlers.forEach(handler -> handler.update(event));
    }
}
```

### Observer Pattern (Account Events)
```java
// Subject Interface
public interface AccountSubject {
    void attach(AccountObserver observer);
    void detach(AccountObserver observer);
    void notifyObservers(AccountEvent event);
}

// Concrete Subject
@Service
public class AccountSubjectManager implements AccountSubject {
    private final Map<Long, List<AccountObserver>> accountObservers = new ConcurrentHashMap<>();

    @Override
    public void notifyObservers(AccountEvent event) {
        List<AccountObserver> observers = accountObservers.get(event.getAccount().getId());
        if (observers != null) {
            observers.forEach(observer -> observer.update(event));
        }
    }
}
```

## Conclusion

This banking system demonstrates a well-architected Spring Boot application with modern design patterns, comprehensive security, and scalable architecture. The recent addition of composite patterns for interest calculations and notifications enhances the system's flexibility and maintainability. The codebase follows SOLID principles and clean architecture, making it suitable for production banking operations.

### Key Architecture Highlights:
- **Layered Architecture**: Clear separation between presentation, business, and data access layers
- **Design Patterns**: Strategic use of Observer, Strategy, and Composite patterns
- **Entity Relationships**: Well-designed JPA entities with proper relationships and constraints
- **Exception Hierarchy**: Comprehensive error handling with custom exceptions
- **Security Integration**: Keycloak-based authentication with role-based authorization
- **Scalable Design**: Modular structure allowing for easy extension and maintenance

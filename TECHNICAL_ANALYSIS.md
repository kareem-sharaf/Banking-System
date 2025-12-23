# Banking System - Comprehensive Technical Analysis

**Project:** Spring Boot Banking System  
**Port:** 8081  
**Database:** PostgreSQL  
**Authentication:** Keycloak (OAuth2 Resource Server with JWT)

---

## 1. Service Architecture

The project follows a modular architecture organized by domain/feature. Each module contains its own controllers, services, repositories, DTOs, and entities.

### 1.1 Core Modules

#### **`com.banking.core`** - Core Infrastructure
**Location:** `src/main/java/com/banking/core/`

**Responsibilities:**
- **Authentication & Authorization** (`core.auth`): User management, role/permission management, Keycloak integration
- **Configuration** (`core.config`): Security configuration, scheduler configuration, approval workflow configuration
- **Exception Handling** (`core.exception`): Global exception handlers and custom exceptions
- **Enums** (`core.enums`): Shared enumerations (AccountState, TransactionType, ApprovalStatus, etc.)
- **Audit Logging** (`core.controller`): Audit log management
- **Notifications** (`core.notification`): Notification system

**Key Services:**
- `UserService` - Local user management
- `KeycloakAdminService` - Keycloak operations
- `KeycloakTokenService` - Token management
- `PermissionService` - Permission CRUD
- `RolePermissionService` - Role-Permission mapping

**Key Repositories:**
- `UserRepository`
- `RoleRepository`
- `PermissionRepository`
- `RolePermissionRepository`
- `LoginHistoryRepository`
- `AuditLogRepository`
- `NotificationRepository`

---

#### **`com.banking.account`** - Account Management
**Location:** `src/main/java/com/banking/account/`

**Responsibilities:**
- Account CRUD operations
- Account type management
- Account linking (parent/child relationships)
- Interest calculation

**Key Services:**
- `AccountService` - Account business logic
- `AccountTypeService` - Account type management
- `InterestCalculationService` - Interest calculation logic

**Key Repositories:**
- `AccountRepository`
- `AccountTypeRepository`
- `InterestCalculationRepository`

---

#### **`com.banking.customer`** - Customer Management
**Location:** `src/main/java/com/banking/customer/`

**Responsibilities:**
- Customer CRUD operations
- Customer dashboard data aggregation

**Key Services:**
- `CustomerService` - Customer business logic
- `CustomerDashboardService` - Dashboard data aggregation

**Key Repositories:**
- `CustomerRepository`

---

#### **`com.banking.transaction`** - Transaction Management
**Location:** `src/main/java/com/banking/transaction/`

**Responsibilities:**
- Transaction processing (deposit, withdrawal, transfer)
- Transaction approval workflow
- Scheduled transactions

**Key Services:**
- `TransactionService` - Transaction processing
- `TransactionApprovalService` - Approval workflow

**Key Repositories:**
- `TransactionRepository`
- `ScheduledTransactionRepository`

---

#### **`com.banking.support`** - Support Ticket System
**Location:** `src/main/java/com/banking/support/`

**Responsibilities:**
- Support ticket management (placeholder - no endpoints yet)

**Key Repositories:**
- `SupportTicketRepository`

---

#### **`com.banking.report`** - Reporting System
**Location:** `src/main/java/com/banking/report/`

**Responsibilities:**
- Report generation (placeholder - no endpoints yet)

**Key Repositories:**
- `ReportRepository`

---

## 2. Endpoints Discovery

### 2.1 Authentication Endpoints (`/api/auth`)

**Controller:** `com.banking.core.auth.controller.AuthController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user (Public) |
| POST | `/api/auth/login` | User login (Public) |
| POST | `/api/auth/refresh` | Refresh access token (Public) |
| GET | `/api/auth/validate` | Validate token (Public) |
| POST | `/api/auth/logout` | Logout user (Authenticated) |
| GET | `/api/auth/user-info` | Get authenticated user info (Authenticated) |
| GET | `/api/auth/me` | Get current user details (CUSTOMER, TELLER, MANAGER, ADMIN) |
| GET | `/api/auth/test` | Test authentication (Authenticated) |

---

### 2.2 User Management Endpoints (`/api/users`)

**Controller:** `com.banking.core.auth.controller.UserController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create user (ADMIN) |
| GET | `/api/users` | Get all users (ADMIN) |
| GET | `/api/users/{id}` | Get user by ID (ADMIN) |
| GET | `/api/users/username/{username}` | Get user by username (ADMIN) |
| GET | `/api/users/email/{email}` | Get user by email (ADMIN) |
| GET | `/api/users/role/{roleId}` | Get users by role ID (ADMIN) |
| PUT | `/api/users/{id}` | Update user (ADMIN) |
| DELETE | `/api/users/{id}` | Delete user (ADMIN) |
| GET | `/api/users/exists/username/{username}` | Check if username exists (ADMIN) |
| GET | `/api/users/exists/email/{email}` | Check if email exists (ADMIN) |

---

### 2.3 Role Management Endpoints (`/api/roles`)

**Controller:** `com.banking.core.auth.controller.RoleController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/roles` | Get all roles (ADMIN) |
| POST | `/api/roles` | Create role (ADMIN) |
| POST | `/api/roles/assign` | Assign role to user (ADMIN) |
| POST | `/api/roles/remove` | Remove role from user (ADMIN) |
| GET | `/api/roles/user/{username}` | Get user roles (ADMIN or self) |
| POST | `/api/roles/ensure-defaults` | Ensure default roles exist (ADMIN) |

---

### 2.4 Permission Management Endpoints (`/api/permissions`)

**Controller:** `com.banking.core.auth.controller.PermissionController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/permissions` | Create permission (ADMIN) |
| GET | `/api/permissions` | Get all permissions (ADMIN) |
| GET | `/api/permissions/{id}` | Get permission by ID (ADMIN) |
| GET | `/api/permissions/name/{name}` | Get permission by name (ADMIN) |
| GET | `/api/permissions/resource/{resource}` | Get permissions by resource (ADMIN) |
| PUT | `/api/permissions/{id}` | Update permission (ADMIN) |
| DELETE | `/api/permissions/{id}` | Delete permission (ADMIN) |
| GET | `/api/permissions/exists/name/{name}` | Check if permission exists (ADMIN) |

---

### 2.5 Role-Permission Management Endpoints (`/api/role-permissions`)

**Controller:** `com.banking.core.auth.controller.RolePermissionController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/role-permissions` | Create role-permission mapping (ADMIN) |
| GET | `/api/role-permissions` | Get all role-permissions (ADMIN) |
| GET | `/api/role-permissions/{id}` | Get role-permission by ID (ADMIN) |
| GET | `/api/role-permissions/role/{roleId}` | Get role-permissions by role ID (ADMIN) |
| GET | `/api/role-permissions/permission/{permissionId}` | Get role-permissions by permission ID (ADMIN) |
| DELETE | `/api/role-permissions/{id}` | Delete role-permission (ADMIN) |
| DELETE | `/api/role-permissions/role/{roleId}/permission/{permissionId}` | Delete by role and permission (ADMIN) |
| GET | `/api/role-permissions/exists/role/{roleId}/permission/{permissionId}` | Check if mapping exists (ADMIN) |

---

### 2.6 Admin Endpoints (`/api/admin`)

**Controller:** `com.banking.core.auth.controller.AdminController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/dashboard` | Admin dashboard (ADMIN) |
| GET | `/api/admin/users` | Get all users (ADMIN) |
| GET | `/api/admin/system` | System information (ADMIN) |
| GET | `/api/admin/config` | System configuration (ADMIN) |

---

### 2.7 Manager Endpoints (`/api/manager`)

**Controller:** `com.banking.core.auth.controller.ManagerController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/manager/reports` | Manager reports (MANAGER, ADMIN) |
| GET | `/api/manager/analytics` | Analytics dashboard (MANAGER, ADMIN) |
| GET | `/api/manager/users` | User management (MANAGER, ADMIN) |

---

### 2.8 Teller Endpoints (`/api/teller`)

**Controller:** `com.banking.core.auth.controller.TellerController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/teller/transactions` | Teller transactions (TELLER, MANAGER, ADMIN) |
| POST | `/api/teller/process-transaction` | Process transaction (TELLER, MANAGER, ADMIN) |
| GET | `/api/teller/dashboard` | Teller dashboard (TELLER, MANAGER, ADMIN) |

---

### 2.9 Customer Endpoints (`/api/customers`)

**Controller:** `com.banking.customer.controller.CustomerController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customers` | Create customer (TELLER, MANAGER, ADMIN) |
| GET | `/api/customers` | Get all customers (TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/{id}` | Get customer by ID (CUSTOMER, TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/user/{userId}` | Get customer by user ID (CUSTOMER, TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/number/{customerNumber}` | Get customer by customer number (CUSTOMER, TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/status/{status}` | Get customers by status (TELLER, MANAGER, ADMIN) |
| PUT | `/api/customers/{id}` | Update customer (TELLER, MANAGER, ADMIN) |
| DELETE | `/api/customers/{id}` | Delete customer (ADMIN) |
| GET | `/api/customers/exists/user/{userId}` | Check if customer exists by user ID (TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/exists/number/{customerNumber}` | Check if customer exists by customer number (TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/customer/accounts` | Get customer accounts (legacy) (CUSTOMER, TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/customer/transactions` | Get customer transactions (legacy) (CUSTOMER, TELLER, MANAGER, ADMIN) |
| GET | `/api/customers/customer/profile` | Get customer profile (legacy) (CUSTOMER, TELLER, MANAGER, ADMIN) |

---

### 2.10 Customer Dashboard Endpoints (`/api/customer/dashboard`)

**Controller:** `com.banking.customer.controller.CustomerDashboardController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/customer/dashboard` | Get customer dashboard data (CUSTOMER, TELLER, MANAGER, ADMIN) |

---

### 2.11 Account Endpoints (`/api/accounts`)

**Controller:** `com.banking.account.controller.AccountController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/accounts` | Get all accounts (TELLER, MANAGER, ADMIN) |
| GET | `/api/accounts/customer/{customerId}` | Get accounts by customer ID (CUSTOMER, TELLER, MANAGER, ADMIN) |
| POST | `/api/accounts` | Create account (TELLER, MANAGER, ADMIN) |
| GET | `/api/accounts/{accountNumber}` | Get account by account number (CUSTOMER, TELLER, MANAGER, ADMIN) |
| PUT | `/api/accounts/{accountNumber}` | Update account (MANAGER, ADMIN) |
| DELETE | `/api/accounts/{accountNumber}` | Close account (MANAGER, ADMIN) |
| POST | `/api/accounts/{parentAccountNumber}/link/{childAccountNumber}` | Link accounts (MANAGER, ADMIN) |

---

### 2.12 Account Type Endpoints (`/api/account-types`)

**Controller:** `com.banking.account.controller.AccountTypeController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/account-types` | Create account type (ADMIN) |
| GET | `/api/account-types` | Get all account types (TELLER, MANAGER, ADMIN) |
| GET | `/api/account-types/{id}` | Get account type by ID (TELLER, MANAGER, ADMIN) |
| GET | `/api/account-types/code/{code}` | Get account type by code (TELLER, MANAGER, ADMIN) |
| PUT | `/api/account-types/{id}` | Update account type (ADMIN) |
| DELETE | `/api/account-types/{id}` | Delete account type (ADMIN) |
| GET | `/api/account-types/exists/code/{code}` | Check if account type exists by code (TELLER, MANAGER, ADMIN) |
| GET | `/api/account-types/exists/name/{name}` | Check if account type exists by name (TELLER, MANAGER, ADMIN) |

---

### 2.13 Interest Calculation Endpoints (`/api/interest`)

**Controller:** `com.banking.account.controller.InterestCalculationController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/interest/calculate-now` | Trigger manual interest calculation (ADMIN) |
| POST | `/api/interest/calculate/{accountNumber}` | Calculate interest for specific account (ADMIN, MANAGER) |
| GET | `/api/interest/last-calculation` | Get last calculation summary (Authenticated) |
| GET | `/api/interest/history?startDate={date}&endDate={date}` | Get calculation history (ADMIN, MANAGER) |
| GET | `/api/interest/account/{accountNumber}?startDate={date}&endDate={date}` | Get calculations for account (Authenticated) |

---

### 2.14 Transaction Endpoints (`/api/transactions`)

**Controller:** `com.banking.transaction.controller.TransactionController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions/{accountNumber}/deposit` | Deposit money (CUSTOMER, TELLER, MANAGER, ADMIN) |
| POST | `/api/transactions/{accountNumber}/withdraw` | Withdraw money (CUSTOMER, TELLER, MANAGER, ADMIN) |
| POST | `/api/transactions/transfer` | Transfer money between accounts (CUSTOMER, TELLER, MANAGER, ADMIN) |

---

### 2.15 Transaction Approval Endpoints (`/api/transactions`)

**Controller:** `com.banking.transaction.controller.TransactionApprovalController`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions/{transactionId}/approve` | Approve transaction (MANAGER, ADMIN) |
| POST | `/api/transactions/{transactionId}/reject` | Reject transaction (MANAGER, ADMIN) |
| GET | `/api/transactions/pending-approvals` | Get pending approval transactions (MANAGER, ADMIN) |
| GET | `/api/transactions/{transactionId}/approval-status` | Get approval status (Authenticated) |

---

### 2.16 Placeholder Controllers (No Endpoints Yet)

- **`/api/support-tickets`** - `SupportTicketController`
- **`/api/reports`** - `ReportController`
- **`/api/scheduled-transactions`** - `ScheduledTransactionController`
- **`/api/audit-logs`** - `AuditLogController`
- **`/api/notifications`** - `NotificationController`

---

## 3. Request/Response Analysis

### 3.1 Authentication Endpoints

#### **POST `/api/auth/register`** (Public)
**Request Body:** `RegisterRequest`
```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 6 chars, required)",
  "firstName": "string (required)",
  "lastName": "string (required)",
  "phoneNumber": "string (optional)",
  "role": "string (default: CUSTOMER)"
}
```

**Response:** `AuthResponse` (201 Created)
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "expiresIn": "number",
  "tokenType": "Bearer",
  "scope": "string",
  "message": "string",
  "userInfo": {
    "id": "number",
    "username": "string",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "keycloakId": "string"
  }
}
```

---

#### **POST `/api/auth/login`** (Public)
**Request Body:** `LoginRequest`
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Response:** `AuthResponse` (200 OK)
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "expiresIn": "number",
  "tokenType": "Bearer",
  "scope": "string",
  "message": "Login successful",
  "userInfo": {
    "id": "number",
    "username": "string",
    "email": "string",
    "firstName": "string",
    "lastName": "string"
  }
}
```

---

#### **POST `/api/auth/refresh`** (Public)
**Request Body:**
```json
{
  "refresh_token": "string (required)"
}
```

**Response:** `AuthResponse` (200 OK)

---

#### **GET `/api/auth/validate`** (Public)
**Headers:**
- `Authorization: Bearer {token}` (required)

**Response:** (200 OK)
```json
{
  "valid": true,
  "message": "Token is valid",
  "user": {
    "id": "number",
    "username": "string",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "active": "boolean"
  }
}
```

---

#### **POST `/api/auth/logout`** (Authenticated)
**Request Body:** (optional)
```json
{
  "refresh_token": "string (optional)"
}
```

**Response:** (200 OK)
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

### 3.2 User Management Endpoints

#### **POST `/api/users`** (ADMIN)
**Request Body:** `CreateUserRequest`
```json
{
  "username": "string (required)",
  "email": "string (required)",
  "firstName": "string (required)",
  "lastName": "string (required)",
  "phoneNumber": "string (optional)",
  "dateOfBirth": "date (optional)",
  "roleId": "number (required)",
  "isActive": "boolean (optional, default: true)"
}
```

**Response:** `UserResponse` (201 Created)

---

#### **PUT `/api/users/{id}`** (ADMIN)
**Path Variables:**
- `id` (Long) - User ID

**Request Body:** `UpdateUserRequest`
```json
{
  "username": "string (optional)",
  "email": "string (optional)",
  "firstName": "string (optional)",
  "lastName": "string (optional)",
  "phoneNumber": "string (optional)",
  "dateOfBirth": "date (optional)",
  "roleId": "number (optional)",
  "isActive": "boolean (optional)"
}
```

**Response:** `UserResponse` (200 OK)

---

### 3.3 Customer Endpoints

#### **POST `/api/customers`** (TELLER, MANAGER, ADMIN)
**Request Body:** `CreateCustomerRequest`
```json
{
  "userId": "number (required)",
  "status": "ACTIVE | INACTIVE | SUSPENDED (optional, default: ACTIVE)"
}
```

**Response:** `CustomerResponse` (201 Created)

---

#### **PUT `/api/customers/{id}`** (TELLER, MANAGER, ADMIN)
**Path Variables:**
- `id` (Long) - Customer ID

**Request Body:** `UpdateCustomerRequest`
```json
{
  "status": "ACTIVE | INACTIVE | SUSPENDED (optional)",
  // Other fields as needed
}
```

**Response:** `CustomerResponse` (200 OK)

---

### 3.4 Account Endpoints

#### **POST `/api/accounts`** (TELLER, MANAGER, ADMIN)
**Request Body:** `CreateAccountRequest`
```json
{
  "customerId": "number (required)",
  "accountTypeCode": "string (required, e.g., SAVINGS, CHECKING)",
  "initialDeposit": "decimal (optional)",
  "currency": "string (optional, e.g., USD, EUR)"
}
```

**Response:** `AccountResponse` (201 Created)

---

#### **PUT `/api/accounts/{accountNumber}`** (MANAGER, ADMIN)
**Path Variables:**
- `accountNumber` (String) - Account number

**Request Body:** `UpdateAccountRequest`
```json
{
  // Account update fields
}
```

**Response:** `AccountResponse` (200 OK)

---

#### **POST `/api/accounts/{parentAccountNumber}/link/{childAccountNumber}`** (MANAGER, ADMIN)
**Path Variables:**
- `parentAccountNumber` (String) - Parent account number
- `childAccountNumber` (String) - Child account number

**Response:** 200 OK (no body)

---

### 3.5 Transaction Endpoints

#### **POST `/api/transactions/{accountNumber}/deposit`** (CUSTOMER, TELLER, MANAGER, ADMIN)
**Path Variables:**
- `accountNumber` (String) - Account number

**Request Body:** `DepositRequest`
```json
{
  "amount": "decimal (required)",
  "description": "string (optional)",
  "currency": "string (optional)"
}
```

**Response:** `TransactionResponse` (200 OK)

---

#### **POST `/api/transactions/{accountNumber}/withdraw`** (CUSTOMER, TELLER, MANAGER, ADMIN)
**Path Variables:**
- `accountNumber` (String) - Account number

**Request Body:** `WithdrawalRequest`
```json
{
  "amount": "decimal (required)",
  "description": "string (optional)",
  "currency": "string (optional)"
}
```

**Response:** `TransactionResponse` (200 OK)

---

#### **POST `/api/transactions/transfer`** (CUSTOMER, TELLER, MANAGER, ADMIN)
**Request Body:** `MoneyTransferRequest`
```json
{
  "fromAccountNumber": "string (required)",
  "toAccountNumber": "string (required)",
  "amount": "decimal (required)",
  "description": "string (optional)",
  "currency": "string (optional)"
}
```

**Response:** `TransactionResponse` (200 OK)

---

### 3.6 Transaction Approval Endpoints

#### **POST `/api/transactions/{transactionId}/approve`** (MANAGER, ADMIN)
**Path Variables:**
- `transactionId` (Long) - Transaction ID

**Request Body:** `ApprovalRequest` (optional)
```json
{
  "comment": "string (optional)"
}
```

**Response:** `ApprovalResponse` (200 OK)
```json
{
  "success": true,
  "message": "string",
  "approvalStatus": "APPROVED | REJECTED | PENDING",
  "transactionId": "number",
  "transactionNumber": "string",
  "approvedBy": "number",
  "approvedAt": "datetime",
  "comment": "string",
  "handlerUsed": "string"
}
```

---

#### **POST `/api/transactions/{transactionId}/reject`** (MANAGER, ADMIN)
**Path Variables:**
- `transactionId` (Long) - Transaction ID

**Request Body:** `ApprovalRequest`
```json
{
  "comment": "string (required)"
}
```

**Response:** `ApprovalResponse` (200 OK)

---

#### **GET `/api/transactions/pending-approvals`** (MANAGER, ADMIN)
**Response:** `List<Transaction>` (200 OK)

---

### 3.7 Interest Calculation Endpoints

#### **POST `/api/interest/calculate-now`** (ADMIN)
**Response:** `InterestCalculationResponse` (200 OK)
```json
{
  "success": true,
  "message": "string",
  "calculationTime": "datetime",
  "totalAccounts": "number",
  "successfulCalculations": "number",
  "failedCalculations": "number",
  "skippedAccounts": "number",
  "totalInterestCalculated": "decimal",
  "calculationDate": "date"
}
```

---

#### **GET `/api/interest/history?startDate={date}&endDate={date}`** (ADMIN, MANAGER)
**Query Parameters:**
- `startDate` (LocalDate, ISO format: YYYY-MM-DD) - Start date (required)
- `endDate` (LocalDate, ISO format: YYYY-MM-DD) - End date (required)

**Response:** `List<InterestCalculation>` (200 OK)

---

#### **GET `/api/interest/account/{accountNumber}?startDate={date}&endDate={date}`** (Authenticated)
**Path Variables:**
- `accountNumber` (String) - Account number

**Query Parameters:**
- `startDate` (LocalDate, optional) - Defaults to 1 month ago
- `endDate` (LocalDate, optional) - Defaults to today

**Response:** `List<InterestCalculation>` (200 OK)

---

### 3.8 Role Management Endpoints

#### **POST `/api/roles`** (ADMIN)
**Request Body:**
```json
{
  "name": "string (required)",
  "description": "string (optional)"
}
```

**Response:** (201 Created)
```json
{
  "success": true,
  "message": "Role created successfully",
  "roleName": "string"
}
```

---

#### **POST `/api/roles/assign`** (ADMIN)
**Request Body:**
```json
{
  "username": "string (required)",
  "roleName": "string (required)"
}
```

**Response:** (200 OK)
```json
{
  "success": true,
  "message": "Role assigned successfully",
  "username": "string",
  "roleName": "string"
}
```

---

## 4. Location Guide

### 4.1 Business Logic (Services)

#### **Authentication & User Management**
- **UserService:** `src/main/java/com/banking/core/auth/service/UserService.java`
- **KeycloakAdminService:** `src/main/java/com/banking/core/auth/service/KeycloakAdminService.java`
- **KeycloakTokenService:** `src/main/java/com/banking/core/auth/service/KeycloakTokenService.java`
- **PermissionService:** `src/main/java/com/banking/core/auth/service/PermissionService.java`
- **RolePermissionService:** `src/main/java/com/banking/core/auth/service/RolePermissionService.java`

#### **Account Management**
- **AccountService:** `src/main/java/com/banking/account/service/AccountService.java`
- **AccountTypeService:** `src/main/java/com/banking/account/service/AccountTypeService.java`
- **InterestCalculationService:** `src/main/java/com/banking/account/service/interest/InterestCalculationService.java`

#### **Customer Management**
- **CustomerService:** `src/main/java/com/banking/customer/service/CustomerService.java`
- **CustomerDashboardService:** `src/main/java/com/banking/customer/service/CustomerDashboardService.java`

#### **Transaction Management**
- **TransactionService:** `src/main/java/com/banking/transaction/service/TransactionService.java`
- **TransactionApprovalService:** `src/main/java/com/banking/transaction/service/TransactionApprovalService.java`

---

### 4.2 Data Access Layer (Repositories)

#### **Authentication & User Management**
- **UserRepository:** `src/main/java/com/banking/core/auth/repository/UserRepository.java`
- **RoleRepository:** `src/main/java/com/banking/core/auth/repository/RoleRepository.java`
- **PermissionRepository:** `src/main/java/com/banking/core/auth/repository/PermissionRepository.java`
- **RolePermissionRepository:** `src/main/java/com/banking/core/auth/repository/RolePermissionRepository.java`
- **LoginHistoryRepository:** `src/main/java/com/banking/core/auth/repository/LoginHistoryRepository.java`

#### **Account Management**
- **AccountRepository:** `src/main/java/com/banking/account/repository/AccountRepository.java`
- **AccountTypeRepository:** `src/main/java/com/banking/account/repository/AccountTypeRepository.java`
- **InterestCalculationRepository:** `src/main/java/com/banking/account/repository/InterestCalculationRepository.java`

#### **Customer Management**
- **CustomerRepository:** `src/main/java/com/banking/customer/repository/CustomerRepository.java`

#### **Transaction Management**
- **TransactionRepository:** `src/main/java/com/banking/transaction/repository/TransactionRepository.java`
- **ScheduledTransactionRepository:** `src/main/java/com/banking/transaction/repository/ScheduledTransactionRepository.java`

#### **Other Modules**
- **AuditLogRepository:** `src/main/java/com/banking/core/repository/AuditLogRepository.java`
- **NotificationRepository:** `src/main/java/com/banking/core/notification/repository/NotificationRepository.java`
- **SupportTicketRepository:** `src/main/java/com/banking/support/repository/SupportTicketRepository.java`
- **ReportRepository:** `src/main/java/com/banking/report/repository/ReportRepository.java`

---

### 4.3 Controllers Location

All controllers are located in their respective module's `controller` package:
- `src/main/java/com/banking/{module}/controller/{ControllerName}.java`

---

### 4.4 DTOs Location

All DTOs are located in their respective module's `dto` package:
- `src/main/java/com/banking/{module}/dto/{DtoName}.java`

---

### 4.5 Entities Location

All entities are located in their respective module's `module/entity` package:
- `src/main/java/com/banking/{module}/module/entity/{EntityName}.java`

---

## 5. Security Mapping

### 5.1 Public Endpoints (No Authentication Required)

These endpoints are configured in `SecurityConfig.java` with `@Order(1)` and permit all requests:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | User registration |
| `/api/auth/login` | POST | User login |
| `/api/auth/refresh` | POST | Refresh token |
| `/api/auth/validate` | GET | Validate token |
| `/h2-console/**` | ALL | H2 Console (dev only) |
| `/actuator/health` | GET | Health check |

---

### 5.2 Role-Based Access Control

#### **ADMIN Only**
- `/api/roles/**` - All role management endpoints
- `/api/admin/**` - All admin endpoints
- `/api/users/**` - All user management endpoints
- `/api/permissions/**` - All permission endpoints
- `/api/role-permissions/**` - All role-permission endpoints
- `/api/account-types` (POST, PUT, DELETE) - Account type management
- `/api/accounts/{accountNumber}` (PUT, DELETE) - Account updates/deletion
- `/api/customers/{id}` (DELETE) - Customer deletion
- `/api/interest/calculate-now` - Manual interest calculation

#### **MANAGER or ADMIN**
- `/api/manager/**` - Manager endpoints
- `/api/accounts/{accountNumber}` (PUT, DELETE) - Account management
- `/api/accounts/{parentAccountNumber}/link/{childAccountNumber}` - Account linking
- `/api/transactions/{transactionId}/approve` - Approve transactions
- `/api/transactions/{transactionId}/reject` - Reject transactions
- `/api/transactions/pending-approvals` - View pending approvals
- `/api/interest/calculate/{accountNumber}` - Calculate interest for account
- `/api/interest/history` - Interest calculation history

#### **TELLER, MANAGER, or ADMIN**
- `/api/teller/**` - Teller endpoints
- `/api/accounts` (GET, POST) - Account listing and creation
- `/api/account-types` (GET) - Account type listing
- `/api/customers` (GET, POST, PUT) - Customer management (except delete)
- `/api/customers/exists/**` - Customer existence checks

#### **CUSTOMER, TELLER, MANAGER, or ADMIN**
- `/api/customer/**` - Customer dashboard endpoints
- `/api/customers/{id}` (GET) - View customer details
- `/api/customers/user/{userId}` (GET) - Get customer by user ID
- `/api/customers/number/{customerNumber}` (GET) - Get customer by number
- `/api/accounts/{accountNumber}` (GET) - View account details
- `/api/accounts/customer/{customerId}` (GET) - Get accounts by customer
- `/api/transactions/{accountNumber}/deposit` - Deposit money
- `/api/transactions/{accountNumber}/withdraw` - Withdraw money
- `/api/transactions/transfer` - Transfer money

#### **Authenticated Users (Any Role)**
- `/api/auth/**` (except public endpoints) - Auth endpoints
- `/api/transactions/{transactionId}/approval-status` - View approval status
- `/api/interest/last-calculation` - View last calculation summary
- `/api/interest/account/{accountNumber}` - View account interest calculations

---

### 5.3 Security Configuration

**File:** `src/main/java/com/banking/core/config/SecurityConfig.java`

**Key Features:**
- **OAuth2 Resource Server** with JWT validation
- **Keycloak Integration** via JWK Set URI: `http://localhost:8180/realms/banking-system/protocol/openid-connect/certs`
- **Method Security** enabled with `@PreAuthorize` annotations
- **CORS Configuration** for `http://localhost:3000` and `http://localhost:8080`
- **Stateless Sessions** (no session management)

**Security Filter Chains:**
1. **Public Chain** (`@Order(1)`): Handles public endpoints without JWT validation
2. **Protected Chain** (`@Order(2)`): Handles all protected endpoints with JWT validation

---

### 5.4 Keycloak Configuration

**Configuration File:** `src/main/resources/application.properties`

**Key Properties:**
- `keycloak.admin.server-url=http://localhost:8080`
- `keycloak.admin.realm=master`
- `keycloak.realm=banking-realm`
- `keycloak.admin.client-id=admin-cli`

**Roles:**
- `ADMIN` - Full system access
- `MANAGER` - Management and approval access
- `TELLER` - Customer service access
- `CUSTOMER` - Customer self-service access

---

## Summary

This Banking System is a comprehensive Spring Boot application with:
- **20 Controllers** with 100+ endpoints
- **12+ Service Classes** handling business logic
- **15+ Repository Interfaces** for data access
- **Role-Based Access Control** with 4 main roles
- **Keycloak Integration** for authentication and authorization
- **Modular Architecture** organized by domain (core, account, customer, transaction, support, report)

The system supports full CRUD operations for users, customers, accounts, and transactions, with an approval workflow for high-value transactions and interest calculation capabilities.


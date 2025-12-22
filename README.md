# Banking System

A comprehensive Spring Boot banking system with Keycloak authentication, role-based access control, and design pattern implementations.

## 🚀 Features

- **Account Management**: Multiple account types (savings, checking, loan, investment)
- **Transaction Processing**: Deposits, withdrawals, transfers with approval workflows
- **Customer Service**: Notification system, support ticket management
- **Administrative**: Role-based access control, reporting, audit logging
- **Design Patterns**: Observer, Strategy, Chain of Responsibility, Factory, Repository
- **Scheduled Transactions**: Recurring payments and transfers
- **Report Generation**: PDF/Excel reports with various formats
- **Spring Boot Actuator**: Health checks, metrics, and monitoring

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.x, Java 17
- **Database**: PostgreSQL
- **Authentication**: Keycloak (OAuth2/JWT)
- **Build Tool**: Maven
- **Testing**: JUnit, Mockito, JMeter
- **Code Quality**: SpotBugs

## 📋 Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (local or Render hosted)

## 🚀 Quick Start

### Option 1: Local Development (Docker Compose)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd banking-system
   ```

2. **Start services with Docker Compose**
   ```bash
   # For local development with PostgreSQL + Keycloak
   docker-compose -f docker-compose.dev.yml up -d

   # Or for Render deployment
   docker-compose up -d
   ```

3. **Run the application**
   ```bash
   # Local profile (default)
   mvn spring-boot:run

   # Or Render profile
   mvn spring-boot:run -Dspring-boot.run.profiles=render
   ```

4. **Access the application**
   - Banking API: http://localhost:8080
   - Keycloak: http://localhost:8180
   - PostgreSQL: localhost:5432

### Option 2: Render Deployment

1. **Update configuration**
   - The application.properties and docker-compose.yml are already configured for Render
   - Database connection details are set for your Render PostgreSQL instance

2. **Deploy to Render**
   ```bash
   # Build and deploy using the provided Dockerfile
   # Set environment variables in Render:
   # SPRING_PROFILES_ACTIVE=render
   ```

## 🔧 Configuration

### Database Options

#### Local PostgreSQL (Development)
```yaml
# docker-compose.dev.yml
spring.datasource.url=jdbc:postgresql://localhost:5432/banking_system
spring.datasource.username=postgres
spring.datasource.password=postgres
```

#### Render PostgreSQL (Production)
```yaml
# application-render.properties
spring.datasource.url=jdbc:postgresql://dpg-d54qqcmr433s73d7dqq0-a.frankfurt-postgres.render.com:5432/banking_1o4s
spring.datasource.username=kareem
spring.datasource.password=OhPrDvaljxrN8xdc3qXKQQseuwzTTBOr
```

### Keycloak Configuration

- **Realm**: banking-system
- **Client ID**: banking-client
- **Admin User**: admin/admin

## 📊 API Documentation

### Postman Collection

Import `postman_collection.json` for complete API testing with:
- Authentication endpoints
- CRUD operations for all entities
- Observer pattern demonstrations
- Role-based access testing

### Key Endpoints

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/accounts/{id}/deposit` - Deposit with observer notifications
- `POST /api/accounts/{id}/withdraw` - Withdrawal with observer notifications
- `POST /api/accounts/{id}/transfer` - Transfer with observer notifications
- `POST /api/interest/calculate-now` - Manual interest calculation
- `GET /api/actuator/health` - Health check (public)

## 🏗️ Design Patterns Implemented

1. **Observer Pattern**: Account transaction notifications (Email, SMS, In-App, Audit)
2. **Strategy Pattern**: Interest calculation algorithms by account type
3. **Chain of Responsibility**: Transaction approval workflows
4. **Factory Pattern**: Service and repository instantiation
5. **Repository Pattern**: Data access abstraction
6. **Builder Pattern**: Complex object construction
7. **Composite Pattern**: Notification handler combinations

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Load Testing
```bash
mvn clean verify -Pjmeter
```

### Code Quality
```bash
mvn clean compile spotbugs:check
```

## 🔐 Security Features

- **JWT Authentication**: Keycloak integration
- **Role-Based Access Control**: CUSTOMER, TELLER, MANAGER, ADMIN
- **Transaction Approvals**: Multi-level approval chains
- **Audit Logging**: Complete transaction and user activity tracking

## 📈 Monitoring

### Spring Boot Actuator Endpoints

- `GET /actuator/health` - Application health
- `GET /actuator/info` - Application info
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/env` - Environment properties

## 🗃️ Database Schema

The application uses JPA/Hibernate for automatic schema generation. Key entities:

- **User**: System users with Keycloak integration
- **Customer**: Customer information linked to users
- **Account**: Bank accounts with types and balances
- **Transaction**: Financial transactions with approval workflows
- **Notification**: User notifications (Email, SMS, In-App)
- **AuditLog**: System audit trail
- **SupportTicket**: Customer support tickets
- **Report**: Generated reports
- **ScheduledTransaction**: Recurring transactions

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

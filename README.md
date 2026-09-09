# Subscription Intelligence Platform

A backend application for managing digital subscriptions, tracking subscription usage, analyzing spending patterns, and identifying potential subscription-related risks.

The platform is built using Spring Boot and provides secure REST APIs with JWT authentication, PostgreSQL persistence, Redis caching, and business decision logic for subscription intelligence.

## Features

* User registration and authentication
* JWT-based authentication and authorization
* Subscription CRUD operations
* Subscription status and category management
* Billing cycle management
* Subscription usage tracking
* Subscription spending analysis
* Overlapping subscription detection
* Price hike risk evaluation
* Usage efficiency evaluation
* Redis-based caching
* Cache warming
* Renewal notification processing
* Global exception handling
* Request validation
* RESTful API architecture

## Technology Stack

| Technology        | Purpose                          |
| ----------------- | -------------------------------- |
| Java 21           | Programming language             |
| Spring Boot 3.3.4 | Backend framework                |
| Spring Web        | REST API development             |
| Spring Data JPA   | Database persistence             |
| Hibernate         | ORM                              |
| Spring Security   | Authentication and authorization |
| JWT               | Stateless authentication         |
| PostgreSQL        | Relational database              |
| Redis             | Caching                          |
| Maven             | Dependency and build management  |
| Lombok            | Boilerplate reduction            |
| MapStruct         | DTO and entity mapping           |
| Postman           | API testing                      |
| Git & GitHub      | Version control                  |

## Architecture

The application follows a layered architecture:

```text
Client
   |
   v
REST Controllers
   |
   v
Services
   |
   v
Decision Engine / Business Logic
   |
   v
Repositories
   |
   v
PostgreSQL
```

Redis is used as a caching layer alongside the application services.

```text
                    +----------------+
                    |    Client      |
                    |    Postman     |
                    +-------+--------+
                            |
                            v
                    +---------------+
                    | REST API      |
                    | Controllers   |
                    +-------+-------+
                            |
                            v
                    +---------------+
                    | Service Layer |
                    +-------+-------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
      +---------------+           +---------------+
      | Decision      |           | Redis Cache   |
      | Engine        |           +---------------+
      +-------+-------+
              |
              v
      +---------------+
      | Repository    |
      +-------+-------+
              |
              v
      +---------------+
      | PostgreSQL     |
      +---------------+
```

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── aathithiyan/
│   │           └── subscription/
│   │               ├── config/
│   │               ├── controller/
│   │               ├── decision/
│   │               ├── domain/
│   │               ├── dto/
│   │               ├── exception/
│   │               ├── mapper/
│   │               ├── repository/
│   │               ├── security/
│   │               ├── service/
│   │               └── scheduler/
│   │
│   └── resources/
│       └── application.properties/
│
└── test/
```

## Core Domain Entities

The application currently contains the following primary entities:

### User

Represents an authenticated application user.

### Subscription

Represents a subscription owned by a user.

Typical subscription information includes:

* Name
* Price
* Billing cycle
* Category
* Status
* Last used timestamp

### PriceHistory

Stores historical subscription pricing information.

### NotificationLog

Stores information related to subscription renewal notifications.

## Authentication

The application uses Spring Security with JWT-based stateless authentication.

Authentication flow:

```text
Register
   |
   v
Login
   |
   v
JWT Token
   |
   v
Authorization Header
   |
   v
Protected API
```

Authenticated requests use:

```text
Authorization: Bearer <JWT_TOKEN>
```

## API Endpoints

Base URL:

```text
http://localhost:8080
```

### Authentication

#### Register

```http
POST /api/v1/auth/register
```

Example request:

```json
{
  "name": "Aathithiyan",
  "email": "aathi@example.com",
  "password": "Password@123"
}
```

#### Login

```http
POST /api/v1/auth/login
```

Example request:

```json
{
  "email": "aathi@example.com",
  "password": "Password@123"
}
```

The login response provides a JWT token that can be used for protected endpoints.

## Subscription APIs

All subscription APIs are associated with a user.

### Create Subscription

```http
POST /api/v1/users/{userId}/subscriptions
```

Example:

```http
POST http://localhost:8080/api/v1/users/1/subscriptions
```

Request:

```json
{
  "name": "Netflix",
  "price": 649.00,
  "billingCycle": "MONTHLY",
  "category": "ENTERTAINMENT"
}
```

### Get All Subscriptions

```http
GET /api/v1/users/{userId}/subscriptions
```

Example:

```http
GET http://localhost:8080/api/v1/users/1/subscriptions
```

### Get Subscription by ID

```http
GET /api/v1/users/{userId}/subscriptions/{subscriptionId}
```

Example:

```http
GET http://localhost:8080/api/v1/users/1/subscriptions/1
```

### Update Subscription

```http
PUT /api/v1/users/{userId}/subscriptions/{subscriptionId}
```

Example:

```http
PUT http://localhost:8080/api/v1/users/1/subscriptions/1
```

Request:

```json
{
  "name": "Netflix Premium",
  "price": 799.00,
  "billingCycle": "MONTHLY",
  "category": "ENTERTAINMENT",
  "status": "ACTIVE",
  "lastUsedAt": "2026-09-09T18:30:00"
}
```

### Delete Subscription

```http
DELETE /api/v1/users/{userId}/subscriptions/{subscriptionId}
```

Example:

```http
DELETE http://localhost:8080/api/v1/users/1/subscriptions/1
```

The DELETE request does not require a JSON request body.

## Environment Configuration

Sensitive configuration values are not stored directly in the repository.

Create environment variables for local development:

```text
DB_URL=jdbc:postgresql://localhost:5432/subscription_intelligence
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password

JWT_SECRET=your_jwt_secret
JWT_EXPIRATION_MS=86400000

REDIS_HOST=localhost
REDIS_PORT=6379

CACHE_WARMING_CRON=0 0 2 * * SUN
NOTIFICATION_RENEWAL_CRON=0 0 8 * * *
NOTIFICATION_RENEWAL_WINDOW_DAYS=7
```

The project includes an `.env.example` file as a reference for the required configuration.

Do not commit real database credentials, JWT secrets, API keys, or other sensitive values to GitHub.

## Database

The application uses PostgreSQL.

Default local configuration:

```text
Host: localhost
Port: 5432
Database: subscription_intelligence
```

Hibernate is configured to automatically update the database schema during development.

```properties
spring.jpa.hibernate.ddl-auto=update
```

For production environments, a migration-based approach such as Flyway or Liquibase can be considered.

## Redis

Redis is used as the caching layer.

Default local configuration:

```text
Host: localhost
Port: 6379
```

The application uses Spring Cache with Redis to reduce repeated database access for frequently requested data.

## Running the Application

### Prerequisites

Make sure the following are installed:

* Java
* Maven
* PostgreSQL
* Redis
* Git

### 1. Clone the Repository

```bash
git clone https://github.com/aathithiyan45/subscription-intelligence-platform.git
```

```bash
cd subscription-intelligence-platform
```

### 2. Configure Environment Variables

Set the required environment variables for PostgreSQL, JWT, and Redis.

### 3. Start PostgreSQL

Make sure PostgreSQL is running and the `subscription_intelligence` database exists.

### 4. Start Redis

For a local Homebrew installation:

```bash
brew services start redis
```

Verify Redis:

```bash
redis-cli ping
```

Expected:

```text
PONG
```

### 5. Start Spring Boot

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

The application will run at:

```text
http://localhost:8080
```

## Testing with Postman

Recommended testing sequence:

```text
1. Register User
       |
       v
2. Login
       |
       v
3. Copy JWT Token
       |
       v
4. Create Subscription
       |
       v
5. Get All Subscriptions
       |
       v
6. Get Subscription by ID
       |
       v
7. Update Subscription
       |
       v
8. Delete Subscription
       |
       v
9. Verify Deletion
```

Protected endpoints should include the JWT token:

```text
Authorization: Bearer <JWT_TOKEN>
```

## Validation

The application validates incoming subscription requests.

Examples include:

* Subscription name cannot be blank.
* Price cannot be null.
* Price must be positive.
* Billing cycle cannot be null.
* Category cannot be null.
* Status is required during subscription updates.

Example validation failure:

```json
{
  "name": "",
  "price": -100,
  "billingCycle": null,
  "category": null
}
```

The API should reject invalid input instead of persisting invalid subscription data.

## Decision Engine

The project contains a decision engine designed to provide intelligence beyond basic CRUD operations.

Current decision components include:

### Overlap Detection

Identifies potentially overlapping or redundant subscriptions.

### Price Hike Risk Evaluation

Evaluates subscription pricing information to identify potential price-related risks.

### Usage Efficiency Evaluation

Uses subscription usage information to determine whether a subscription is being effectively utilized.

These components provide the foundation for turning the application from a simple subscription manager into a subscription intelligence platform.

## Caching Strategy

Redis caching is integrated into the application to improve performance for frequently accessed data.

General flow:

```text
Request
   |
   v
Check Redis Cache
   |
   +---- Cache Hit ----> Return Cached Data
   |
   +---- Cache Miss
              |
              v
        Query PostgreSQL
              |
              v
        Store in Redis
              |
              v
        Return Response
```

Cache invalidation is required when subscription data is created, updated, or deleted to prevent stale data.

## Scheduled Processing

The application contains scheduled functionality for:

* Cache warming
* Subscription renewal notification processing

These operations are configured through environment variables so that schedules can be changed without modifying application code.

## Error Handling

The application uses centralized exception handling to provide consistent API responses for errors such as:

* Resource not found
* Invalid request data
* Authentication failures
* Authorization failures
* Business rule violations

## Security

Security-related configuration is externalized from the source code.

The project follows these practices:

* JWT-based authentication
* Stateless authentication
* Password-based user authentication
* Environment-based secrets
* Protected API endpoints
* Validation of incoming requests
* No real credentials committed to Git

For production deployment, additional security measures such as HTTPS, secure secret management, rate limiting, and stricter CORS configuration should be applied.

## API Versioning

The API uses versioning through the `/api/v1` prefix.

Example:

```text
/api/v1/auth/login
/api/v1/users/{userId}/subscriptions
```

Using API versioning makes it possible to introduce future breaking changes through a new version such as `/api/v2` without immediately breaking existing clients.

## Future Improvements

Potential future improvements include:

* Subscription recommendation engine
* Advanced spending analytics
* Monthly and yearly spending dashboards
* Email notification integration
* Docker Compose for PostgreSQL and Redis
* Automated CI/CD pipeline
* API documentation using OpenAPI/Swagger
* Advanced test coverage
* Production monitoring and logging

## Project Status

The core backend functionality is implemented and the main subscription CRUD APIs have been tested using Postman.

Current focus areas include validating the application's intelligence, caching, analytics, notification, and security features.

## Author

**Aathithiyan P**

B.Tech Information Technology
Saranathan College of Engineering, Trichy

GitHub: `aathithiyan45`

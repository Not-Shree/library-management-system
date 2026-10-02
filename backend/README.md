# Backend — Spring Boot API

Java 21 · Spring Boot 3.3 · Spring Data JPA · Spring Security (JWT) · PostgreSQL · springdoc (Swagger)

## Run

```bash
cp .env.example .env     # set DB_PASSWORD and JWT_SECRET (32+ characters)
mvn spring-boot:run      # http://localhost:8080  ·  Swagger: http://localhost:8080/swagger-ui.html
mvn test                 # unit tests (FineCalculatorTest)
```

The database must already contain `../database/schema.sql` (and optionally `seed.sql`). See the main README, section 7.

Configuration is read from environment variables, `backend/.env`, or an optional `backend/application-local.properties`
(see `application-example.properties`). All three are git-ignored except the examples.

## Code map

| Package | What is in it |
|---|---|
| `controller` | REST endpoints. Thin: validate input, check role, call one service method. |
| `service` | All business rules. Start with `BorrowingService` (issue / return / renew) and `FineCalculator`. |
| `repository` | Spring Data repositories; `SearchSpecs` builds the search/filter queries. |
| `entity` | JPA entities mapped to the tables in `database/schema.sql`, plus enums. |
| `dto` | Request and response records sent over the API. |
| `mapper` | Converts entities to DTOs. |
| `security` | JWT creation/validation, the request filter, security rules, current-user helper. |
| `exception` | Custom exceptions and `GlobalExceptionHandler` (standard error JSON). |
| `config` | Swagger / OpenAPI setup. |
| `util` | CSV writer (formula-injection safe) and text helpers. |

## Where to find each rule

| Rule | Method |
|---|---|
| Borrowing limit, availability, unpaid fines, due date | `BorrowingService.issue` |
| Fine calculation | `FineCalculator.calculate` (pure, unit-tested) |
| Return and automatic fine | `BorrowingService.returnBook` |
| Renewal limits | `BorrowingService.renew` |
| Payments, no overpayment, waivers | `FineService.recordPayment`, `FineService.waive` |
| Reservation queue and holds | `ReservationService.create`, `offerCopyToQueue`, `expireUncollectedHolds` |
| Daily reminders | `ScheduledJobs.runDailyJobs` (08:00 every day and at start-up) |

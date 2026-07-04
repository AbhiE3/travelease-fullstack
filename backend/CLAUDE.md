# TravelEase Backend

Spring Boot REST API backend for TravelEase, a platform for travelers to plan trips, invite
companions, book hotels/transport, build itineraries, track budgets, and get personalized
recommendations.

## Tech Stack

- Java 17, Spring Boot 3.5.16
- Spring Web (REST), Spring Data JPA, Spring Security
- H2 (in-memory, current dev/test database)
- Lombok
- springdoc-openapi (Swagger UI)
- Maven (`./mvnw`)
- Base package: `com.travelease.backend`

## Where to look

- For agent-specific code generation rules → [AGENTS.md](AGENTS.md)
- For coding conventions (naming, layering, validation, error handling) → [docs/coding_guidelines.md](docs/coding_guidelines.md)
- For the layered architecture, auth flow, and response envelope spec → [docs/architecture.md](docs/architecture.md)
- For the full REST API contract (every endpoint, request/response shape) → [docs/api_contract.md](docs/api_contract.md)

## Project structure

```
src/main/java/com/travelease/backend/
  auth/            Login, register, me — controller/, dto/, entity/, repository/, service/
  admin/           Admin catalog CRUD — controller/ (stub)
  trip/            Trip management — controller/, dto/, entity/, repository/, service/
  invitation/      Invitation flow — controller/, dto/, entity/, repository/, service/
  hotel/           Traveler hotel booking — controller/, dto/, entity/, repository/, service/
  transport/       Traveler transport booking — controller/, dto/, entity/, repository/, service/
  itinerary/       Itinerary items — controller/, dto/, entity/, repository/, service/
  budget/          Budgets & expenses — controller/, dto/, entity/, repository/, service/
  settlement/      Expense settlement — controller/, dto/, entity/, repository/, service/
  recommendation/  Suggestions — controller/, dto/, service/
  notification/    Reminders — controller/, dto/, entity/, repository/, service/
  delay/           Delay detection — controller/, dto/, service/
  health/          GET /health only
  security/        JwtService, JwtAuthFilter, UserDetailsServiceImpl
  shared/          config/, dto/, entity/, exception/ — used by every feature
```

## Domains covered

Authentication & User Management, Admin Management (Hotels, Destinations, Transports,
Attractions, Activities), Trip Management, Traveler Trip Invitation Management, Hotel
Management, Transport Management, Personalized Recommendations, Itinerary Management,
Budget & Expense Management, Expense Settlement, Smart Reminders & Notifications, Delay
Impact Detection.

## Running

```
./mvnw spring-boot:run
./mvnw test
```

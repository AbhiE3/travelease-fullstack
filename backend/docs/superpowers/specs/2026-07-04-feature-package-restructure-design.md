# Feature-Based Package Restructure Design

**Date:** 2026-07-04
**Status:** Approved

## Goal

Migrate the TravelEase backend from a flat layer-by-layer package structure to a feature-based
package structure so that each domain (auth, admin, trip, etc.) owns all its own layers — controller,
dto, entity, repository, service — in one place. Cross-cutting infrastructure (JWT, security config,
shared utilities) stays in dedicated top-level packages.

## Chosen Approach

**Option B structure** (security infrastructure stays global) +
**Option A migration** (migrate existing auth + scaffold all future domains) +
**Option C domain split** (admin/ owns catalog CRUD, peer packages own traveler-facing logic)

## Final Package Map

```
com.travelease.backend/
│
├── auth/           Login, register, me, password reset — owns User & Role entities
├── admin/          Catalog CRUD (Hotel, Destination, Transport, Attraction, Activity) — ROLE_ADMIN only
├── trip/           Trip creation & management
├── invitation/     Traveler trip invitation flow
├── hotel/          Traveler hotel booking (reads admin's Hotel catalog)
├── transport/      Traveler transport booking
├── itinerary/      Day-by-day itinerary items per trip
├── budget/         Budgets & expenses per trip
├── settlement/     Expense settlement between travelers
├── recommendation/ Personalized destination/activity suggestions
├── notification/   Smart reminders & GET /api/notifications
├── delay/          Delay impact detection
├── health/         GET /health — no dependencies, no DB
│
├── security/       JWT infrastructure (cross-cutting — gates every request)
│   JwtService.java
│   JwtAuthFilter.java
│   UserDetailsServiceImpl.java
│
└── shared/         Utilities every feature depends on
    ├── config/     SecurityConfig.java, JpaAuditingConfig.java
    ├── dto/        ApiResponse.java, ApiError.java
    ├── entity/     BaseEntity.java
    └── exception/  GlobalExceptionHandler.java + all custom exception classes
```

## Inside Each Feature Package

Every feature follows the same internal shape:

```
<feature>/
  controller/   @RestController — one class per feature (e.g. TripController)
  dto/          Request/response records — CreateXRequest, UpdateXRequest, XResponse
  entity/       @Entity JPA classes — only if the feature owns DB rows
  repository/   JpaRepository interfaces — only if the feature has entities
  service/      XService (interface) + XServiceImpl (class)
```

Features without persistent state (recommendation, delay) skip `entity/` and `repository/`.
Features with multiple sub-resources (e.g. budget has Budget + Expense) may have multiple
entities, repositories, and services within the same feature package.

## Why Exceptions Stay in shared/

`GlobalExceptionHandler` (`@RestControllerAdvice`) must import every custom exception class to
map it to the correct HTTP status. If exceptions lived inside feature packages, `shared` would
depend on feature packages — a circular dependency. All exception classes therefore live in
`shared/exception/` regardless of which feature throws them.

## Why security/ Is Not Inside auth/

`JwtAuthFilter` runs on every HTTP request across all features. `SecurityConfig` configures path
rules for admin, trip, booking, and auth endpoints alike. Burying these inside `auth/` would make
auth a dependency of the entire application, creating misleading coupling. They stay at the top
level as infrastructure.

## File Moves — Existing Auth Code

| Current path | New path |
|---|---|
| `config/SecurityConfig.java` | `shared/config/SecurityConfig.java` |
| `config/JpaAuditingConfig.java` | `shared/config/JpaAuditingConfig.java` |
| `security/JwtService.java` | `security/JwtService.java` (unchanged) |
| `security/JwtAuthFilter.java` | `security/JwtAuthFilter.java` (unchanged) |
| `security/UserDetailsServiceImpl.java` | `security/UserDetailsServiceImpl.java` (unchanged) |
| `controller/AuthController.java` | `auth/controller/AuthController.java` |
| `controller/HealthController.java` | `health/HealthController.java` |
| `dtos/ApiResponse.java` | `shared/dto/ApiResponse.java` |
| `dtos/ApiError.java` | `shared/dto/ApiError.java` |
| `dtos/LoginRequest.java` | `auth/dto/LoginRequest.java` |
| `dtos/RegisterRequest.java` | `auth/dto/RegisterRequest.java` |
| `dtos/LoginResponse.java` | `auth/dto/LoginResponse.java` |
| `dtos/UserResponse.java` | `auth/dto/UserResponse.java` |
| `entity/User.java` | `auth/entity/User.java` |
| `entity/Role.java` | `auth/entity/Role.java` |
| `entity/BaseEntity.java` | `shared/entity/BaseEntity.java` |
| `repositories/UserRepository.java` | `auth/repository/UserRepository.java` |
| `services/AuthService.java` | `auth/service/AuthService.java` |
| `services/AuthServiceImpl.java` | `auth/service/AuthServiceImpl.java` |
| `services/UserService.java` | `auth/service/UserService.java` |
| `services/UserServiceImpl.java` | `auth/service/UserServiceImpl.java` |
| `exception/GlobalExceptionHandler.java` | `shared/exception/GlobalExceptionHandler.java` |
| `exception/DuplicateResourceException.java` | `shared/exception/DuplicateResourceException.java` |
| `exception/InvalidCredentialsException.java` | `shared/exception/InvalidCredentialsException.java` |
| `exception/ResourceNotFoundException.java` | `shared/exception/ResourceNotFoundException.java` |

All `package` declarations and `import` statements in moved files must be updated to match new paths.

## Scaffolding

After migrating auth, create empty `controller/`, `dto/`, `entity/`, `repository/`, `service/`
sub-packages (via `.gitkeep` files) under:
`admin/`, `trip/`, `invitation/`, `hotel/`, `transport/`, `itinerary/`, `budget/`, `settlement/`,
`recommendation/`, `notification/`, `delay/`

## Docs to Update

- `docs/coding_guidelines.md` — replace the flat-layer package table with the feature-based map;
  remove the "Do not create feature-sliced subpackages" rule; add the shared/ and security/
  rationale.
- `docs/architecture.md` — update the package structure section to reflect the new layout.
- `CLAUDE.md` — update the project structure section.

## Success Criteria

- `./mvnw spring-boot:run` starts without errors.
- `./mvnw test` passes — all existing auth/health tests green.
- No class is in the old flat packages (`controller`, `dtos`, `entity`, `repositories`, `services`,
  `config`, `exception` at the root level).
- Swagger UI at `/swagger-ui.html` still lists all existing endpoints correctly.

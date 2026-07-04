# Architecture

## Layered architecture

```
HTTP request
   │
   ▼
controller        ── @RestController, validates input (@Valid), maps to/from DTOs
   │
   ▼
services           ── business logic, transaction boundaries, throws domain exceptions
   │
   ▼
repositories       ── Spring Data JPA, talks to the database
   │
   ▼
entity             ── JPA-mapped tables (H2)
```

- Controllers never talk to repositories or entities directly.
- Services never know about HTTP (`HttpServletRequest`, status codes) — that's the controller's
  job.
- `security/` and `shared/` are cross-cutting: every feature layer can depend on them, but they
  don't depend on any feature package.

## Package layout

Each domain owns all its layers in one package. Cross-cutting infrastructure lives in dedicated
top-level packages.

```
com.travelease.backend/
├── auth/            controller/ · dto/ · entity/ · repository/ · service/
├── admin/           controller/ · dto/ · entity/ · repository/ · service/
├── trip/            controller/ · dto/ · entity/ · repository/ · service/
├── invitation/      controller/ · dto/ · entity/ · repository/ · service/
├── hotel/           controller/ · dto/ · entity/ · repository/ · service/
├── transport/       controller/ · dto/ · entity/ · repository/ · service/
├── itinerary/       controller/ · dto/ · entity/ · repository/ · service/
├── budget/          controller/ · dto/ · entity/ · repository/ · service/
├── settlement/      controller/ · dto/ · entity/ · repository/ · service/
├── recommendation/  controller/ · dto/ · service/
├── notification/    controller/ · dto/ · entity/ · repository/ · service/
├── delay/           controller/ · dto/ · service/
├── health/          HealthController.java
├── security/        JwtService · JwtAuthFilter · UserDetailsServiceImpl
└── shared/
    ├── config/      SecurityConfig, JpaAuditingConfig
    ├── dto/         ApiResponse<T>, ApiError
    ├── entity/      BaseEntity
    └── exception/   GlobalExceptionHandler + all custom exception classes
```

Features without persistent state (`recommendation/`, `delay/`) skip `entity/` and `repository/`.

## Authentication & authorization

- **JWT access + refresh tokens.** On login (`POST /api/auth/login`), the server issues a
  short-lived access token (e.g. 15 min) and a longer-lived refresh token (e.g. 7 days).
- `POST /api/auth/refresh-token` exchanges a valid refresh token for a new access token without
  requiring the user to log in again.
- `POST /api/auth/logout` invalidates the refresh token (server-side blacklist/store, or DB flag
  on the token record).
- The access token is sent as `Authorization: Bearer <token>` on every authenticated request.
- `security/JwtAuthFilter` runs once per request, validates the access token, and populates the
  Spring Security context with the authenticated user and their role.
- **Roles:** every user has exactly one role: `ROLE_ADMIN` or `ROLE_TRAVELER`.
  - Admin-only endpoints (hotel/destination/transport/attraction/activity management — the
    `US-ADMIN-*` stories) require `@PreAuthorize("hasRole('ADMIN')")`.
  - Traveler endpoints (trips, invitations, bookings, itineraries, budgets, etc.) require
    `@PreAuthorize("hasRole('TRAVELER')")` or `isAuthenticated()` where both roles are valid.
  - Public endpoints: `POST /api/auth/register`, `POST /api/auth/login`,
    `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`,
    `GET /api/auth/validate-reset-token/{token}`.

## Password reset flow

- `POST /api/auth/forgot-password` generates a single-use token with an expiry (e.g. 1 hour),
  stores it against the user, and (conceptually) emails a reset link containing the token.
- `GET /api/auth/validate-reset-token/{token}` checks the token exists and hasn't expired before
  the client shows the "set new password" form.
- `POST /api/auth/reset-password` validates the token again, updates the password hash, and
  invalidates the token.

## Standard API response envelope

Every controller method returns this shape — never a raw entity, raw list, or ad-hoc object.

**Success:**
```json
{
  "success": true,
  "data": { },
  "message": "Trip created successfully",
  "timestamp": "2026-06-28T14:32:00Z"
}
```

**Error:**
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Trip with id 42 not found",
    "details": []
  },
  "timestamp": "2026-06-28T14:32:00Z"
}
```

- `data` is `null` on error responses; `error` is omitted (or `null`) on success responses.
- `details` is a list of field-level validation errors when applicable (e.g.
  `[{ "field": "email", "message": "must be a valid email" }]`), otherwise an empty list.
- Implemented as `ApiResponse<T>` in `shared/dto`, with static factory methods
  `ApiResponse.success(data, message)` and `ApiResponse.error(code, message, details)`.
  `GlobalExceptionHandler` (in `shared/exception`) builds the error variant; controllers build
  the success variant.

## Cross-cutting concerns

- **Auditing:** `BaseEntity.createdAt`/`updatedAt` are populated via Spring Data JPA auditing
  (`@EnableJpaAuditing` in `shared/config/JpaAuditingConfig`).
- **OpenAPI/Swagger:** springdoc-openapi is already on the classpath; controllers should be
  documented with standard `@Operation`/`@ApiResponse` annotations so `/swagger-ui.html` stays
  accurate.
- **Notifications:** modeled as a `Notifications` entity/table written to by services (invitation
  sent, trip cancelled, activity reminder, departure suggestion) — no external email/push
  integration is in scope yet; notifications are read via `GET /api/notifications`.

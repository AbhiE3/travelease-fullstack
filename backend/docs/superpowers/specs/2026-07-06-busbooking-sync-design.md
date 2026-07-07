# BusBooking Module Sync — Design

## Purpose

Synchronize the latest, stabilized BusBooking implementation from the standalone project (`E:\bus_booking\backend`) into the `busbooking` module of `travelease-fullstack`, without disturbing other developers' modules (`accommodation`, `admin`, `budget`, `expense`, `health`, `itinerary`, `settlement`, `trip`, `frontend`).

Source of truth for BusBooking business logic: `E:\bus_booking\backend`.
Target: `travelease-fullstack/backend/src/{main,test}/java/com/travelease/backend/busbooking/**`, branch `feature-busbooking-v2` (currently clean).

## Background: what the standalone project's `AUTH_MERGE_REPORT.md` documents

The standalone backend previously merged a second source's JWT/auth implementation into itself, adding a `ROLE_PROVIDER` role and a full ownership model: bus operators (`ROLE_PROVIDER` users) are scoped to only their own buses, drivers, conductors, bookings, analytics, and reports via a `providerId` field carried on `User` and enforced through `SecurityUtil.getCurrentProviderId()` / `resolveEffectiveProviderId(Long)`. It also fixed several bugs along the way (a public-GET route-shadowing bug in schedule search, missing authorization on refunds, automatic booking completion replacing a manual endpoint).

The `travelease-fullstack` repo's `busbooking` module is a mechanical package-rename of an **older** snapshot of the standalone project, predating that merge: no `ROLE_PROVIDER`, stale reporting status-filter logic, a text-encoding bug, and (independently of any of this) a currently-live bug where busbooking's own `SecurityUtil.getCurrentUserId()` cannot resolve the current user at all under the monorepo's JWT wiring.

## Decision: user confirmed reintroducing the full ROLE_PROVIDER ownership model

This sync is not a pure bug-fix pass — it brings the monorepo's BusBooking module up to full parity with the standalone project's provider-ownership behavior, per explicit user confirmation.

## Critical architectural finding: do not change the JWT principal design

Five other files/modules — `budget.controller.BudgetController`, `accommodation.controller.{UserHotelBookingController,HotelController,HotelBookingController}`, `settlement.controller.SettlementController`, `expense.controller.ExpenseController`, and `auth.controller.AuthController` itself — call `authentication.getName()` expecting it to return the authenticated user's **email**, then look the user up by email. This is an established, load-bearing convention.

The standalone project's design (JWT principal = full `User` entity, so `SecurityUtil` can reflect `getId()` off it) is **not compatible** with this convention and must not be copied over — doing so would break all five call sites above, which is out of scope and forbidden by the team-safety rule.

**Resolution**: busbooking's own `security.SecurityUtil.getCurrentUserId()` is rewritten to resolve the same way every other module already does: `authentication.getName()` → email → `auth.repository.UserRepository.findByEmail()` → the user's UUID id. This is a busbooking-local file change. The shared `security.JwtAuthFilter` / `security.JwtService` are untouched.

This also resolves the `Long` vs `UUID` id mismatch: shared `auth.User` uses a `UUID` primary key; busbooking's `Booking.userId` (and similar fields) are currently a disconnected `Long`. Per user decision, these get converted to `UUID` and linked to `auth.User` for real referential correctness, rather than adding a Long↔UUID adapter layer.

Note: `providerId` (on `Bus`, `Driver`, `Conductor`, `Trip`, and throughout analytics/reports) is a separate, independent grouping key with **no** foreign-key relationship to `User` in either codebase today — it stays `Long` and is unaffected by the UUID conversion.

## Scope classification

| Category | Contents |
|---|---|
| A. BusBooking feature update | Reintroduce `ROLE_PROVIDER` ownership scoping into busbooking's own controllers/services/`SecurityUtil` |
| B. BusBooking bug fix | Reporting status-filter logic (booking/revenue/passenger/cancellation reports), `" â†’ "` mojibake encoding fix, `SecurityUtil.getCurrentUserId()` currently-broken resolution |
| C. BusBooking refactor | `Long`→`UUID` conversion of true user-reference fields |
| D. BusBooking architecture improvement | None identified — no provider/strategy interface pattern exists in either codebase; `providerId` remains a plain grouping attribute, not restructured |
| E. BusBooking test update | Any tests exercising the above; add `spring-security-test` dependency only if needed |
| F. BusBooking configuration change | `app.scheduler.default-provider-id` property |
| G. Shared auth-related change | `Role.ROLE_PROVIDER`, `User.providerId`, `UserResponse.providerId`, `SecurityConfig` permitAll+CORS fix, `app.cors.allowed-origins` property |
| H. Duplicate code already handled by shared auth | Standalone's `User`/`Role`/`UserRepository`/`AuthService`/`UserService`/`AuthController`/`LoginRequest`/`RegisterRequest`/`LoginResponse`/`UserResponse` — already correctly represented by target's `auth` module; **not** migrated |
| I. Integration-specific target code that must be preserved | `shared.exception.GlobalExceptionHandler` (already handles every busbooking exception type — verified, no change needed), `shared.config.SecurityConfig`'s existing rules for other modules, `auth` module's email-based principal convention |
| J. Unrelated/out-of-scope change | Any change to `accommodation`, `admin`, `budget`, `expense`, `health`, `itinerary`, `settlement`, `trip`, `frontend`; Spring Boot/POI/springdoc version bumps (target is already even with or ahead of source) |

## Migration plan

### Files to update (busbooking-scoped)
- `service/impl/ReportServiceImpl.java` — port `SUCCESSFUL_BOOKING_STATUSES`/`CANCELLED_BOOKING_STATUSES`/`validateBookingStatusFilter()`; fix booking/revenue/passenger/cancellation status filtering to match source; fix `" â†’ "` → `" → "` encoding in Revenue/Passenger/Cancellation reports.
- `security/SecurityUtil.java` — rewrite `getCurrentUserId()` to resolve via email→`auth.UserRepository`; return type `Long`→`UUID`; add `getCurrentProviderId()`/`resolveEffectiveProviderId(Long)` (adapted from source).
- `entity/Booking.java`, `entity/SeatLock.java`, `entity/SearchHistory.java` — `userId` field `Long`→`UUID`.
- `dto/response/BookingResponse.java` — `userId` field `Long`→`UUID`.
- `repository/BookingRepository.java`, `repository/SeatLockRepository.java`, `repository/SearchHistoryRepository.java`, `repository/RefundRepository.java` — update user-id-based method signatures to `UUID`.
- `service/impl/{BookingServiceImpl,ScheduleServiceImpl,RefundServiceImpl,SeatAllocationServiceImpl}.java` — update `Long userId = securityUtil.getCurrentUserId()` call sites to `UUID`; `BookingServiceImpl.ensureOwnership` gains ADMIN bypass + `AccessDeniedException` (403) instead of `BookingException` (400).
- `service/SeatAllocationService.java` — `userId` parameter type `Long`→`UUID`.
- `controller/{AnalyticsController,BusController,FleetOperationController,ScheduleController,ReportController,RefundController}.java` — `@PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")` where source has it; add authorization to `RefundController` if currently absent.
- `service/impl/{BusServiceImpl,DriverServiceImpl,ConductorServiceImpl,AnalyticsServiceImpl,ReportServiceImpl,TripServiceImpl}.java` — wire `resolveEffectiveProviderId` for ownership-scoped queries.
- `controller/BookingController.java` — remove `POST /{id}/complete` if still present (superseded by automatic completion).
- `controller/FleetOperationController.java` — fold driver/conductor `PATCH .../status` into `PUT` if still separate.

### Files to add
None expected — the package rename already carried every source file across. Confirmed during implementation if any standalone-only file surfaces.

### Files to preserve
Everything in `busbooking` not listed above (mappers, most DTOs, most entities, most repositories, most controllers/services untouched by the ownership/id changes) — these are already in sync or contain no divergence worth migrating.

### Files to delete
None. Source's retired `DevAuthenticationFilter` placeholder does not exist in target's busbooking `security` package — already clean.

### Auth changes required: **yes**, minimal and additive
- `auth/entity/Role.java` — add `ROLE_PROVIDER` enum constant.
- `auth/entity/User.java` — add nullable `Long providerId` field.
- `auth/dto/UserResponse.java` — add `providerId` field.
- `shared/config/SecurityConfig.java` — add busbooking's public-GET permitAll matchers (`/api/buses/**`, `/api/routes/**`, `/api/schedules/**`, `/api/seats/**`, `/api/bookings/ticket/verify/**`, with `search/history`/`search/suggestions` authenticated-before-wildcard ordering) and a CORS `SecurityFilterChain` sourced from `app.cors.allowed-origins`. Currently a live bug (anonymous bus-browsing 401s today; no CORS configured at all).
- `application.properties` — add `app.cors.allowed-origins` and `app.scheduler.default-provider-id` (both keys currently missing).

All four are backward compatible: new enum constant, new nullable column, new response field, and widened (not narrowed) `permitAll` rules — no existing consumer's behavior changes.

### Local security files not migrated
Standalone's `security/{SecurityConfig,JwtService,JwtAuthFilter,DevAuthenticationFilter}.java` are not migrated — target's shared `security`/`shared.config.SecurityConfig` already provide this functionality (with the email-principal convention this migration must respect, not override). Standalone's `entity/User.java`, `entity/enums/Role.java`, `repository/UserRepository.java`, `service/{AuthService,AuthServiceImpl,UserService,UserServiceImpl}.java`, `controller/AuthController.java`, `dto/{request/LoginRequest,request/RegisterRequest,response/UserResponse,response/LoginResponse}.java` are all duplicates of what target's `auth` module already correctly provides — not migrated.

## Change isolation

**Allowed change scope:**
- `backend/src/{main,test}/java/com/travelease/backend/busbooking/**`
- `backend/src/main/resources/application.properties` (additive lines only)
- `backend/pom.xml` (additive only — `spring-security-test`, test-scope, only if needed)
- `backend/src/main/java/com/travelease/backend/auth/entity/Role.java`
- `backend/src/main/java/com/travelease/backend/auth/entity/User.java`
- `backend/src/main/java/com/travelease/backend/auth/dto/UserResponse.java`
- `backend/src/main/java/com/travelease/backend/shared/config/SecurityConfig.java`

Everything else is forbidden. After implementation, `git status` and `git diff --name-only` are checked against this list; any unrelated file change is reverted.

## Validation

- Compile/build the backend module.
- Run BusBooking's test suite; run auth-adjacent tests since `auth`/`SecurityConfig` changed.
- Inspect Spring bean wiring in `SecurityConfig` for duplicate filter chains/beans.
- Confirm no other module's `authentication.getName()` call site is affected (it isn't — principal design unchanged).
- If practical, run the full backend test suite; report (not fix) any unrelated pre-existing failures.

## Commit strategy

Two commits, since a shared-auth change is genuinely required:
1. **Shared auth compatibility update required by BusBooking** — `Role.ROLE_PROVIDER`, `User.providerId`, `UserResponse.providerId`, `SecurityConfig` permitAll+CORS fix, the two new properties.
2. **Synchronize latest BusBooking implementation** — everything under `busbooking/**`.

No push until the final diff is reviewed with the user.

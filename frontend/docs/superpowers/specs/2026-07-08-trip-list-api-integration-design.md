# Trip List — Real API Integration — Design

## Context

Every feature page in this app currently renders from static mock data in `core/mock-data.ts`.
The only real backend integration so far is authentication (`core/auth/`). This design wires
`trip-list.html` — the traveler's "My Trips" page — to the real `GET /api/trips` endpoint
(`TripController`), making it the first non-auth page backed by live data. The pattern
established here (a feature-scoped HTTP service consuming the shared `ApiResponse<T>` envelope)
is expected to be copied by later pages as they're migrated off mock data, one page at a time.

The real `TripResponse` returned by the backend is much leaner than the mock `Trip` shape: it has
`tripName`, `organizer`, `sourceLocation`, `destinationId` (a bare `Integer`, no name-lookup API
exists — `GET /api/destinations` is documented in `api_contract.md` as planned but was never
built), `budgetAmount` (a single total, not per-person), `startDate`/`endDate`, and
`status: TravelerTripStatus` (`PLANNING`/`CONFIRMED`/`ONGOING`/`COMPLETED`/`CANCELLED`). There is
no hero image, trip "type", member count, or budget-progress field anywhere on the `Trip` entity —
those were invented for the mock and have no backend equivalent.

## Decisions

**Card content — real data only.** The trip card drops the hero image, type badge, member count,
and progress bar entirely. It shows: trip name, `sourceLocation → Destination #<destinationId>`
(via the existing `DestinationPill` component, using a `Destination #<id>` placeholder until a
real destinations API exists), start–end dates, a status badge, and the total budget (relabeled
from "per person" to a plain total, since that's all the API provides).

**Shared `ApiResponse<T>` type.** `auth.service.ts` currently declares its own private
`ApiResponse<T>` interface. Since `TripsService` needs the identical envelope shape, it's
extracted to `core/api/api-response.model.ts` and both services import it from there, rather than
duplicating the type a second time.

**New `features/trips/services/` module:**
- `trip.models.ts` — a `Trip` interface mirroring `TripResponse` field-for-field (`tripId`,
  `tripName`, `organizer: { userId, name, email }`, `sourceLocation`, `destinationId`,
  `budgetAmount`, `categoryId`, `startDate`, `endDate`, `status`, `viewerRole`, `createdAt`,
  `updatedAt`), and the `TripStatus` union type (`'PLANNING' | 'CONFIRMED' | 'ONGOING' |
  'COMPLETED' | 'CANCELLED'`).
- `trips.service.ts` — `TripsService` (`@Injectable({ providedIn: 'root' })`) with
  `listMyTrips(): Observable<Trip[]>`, calling `GET /api/trips` and mapping
  `ApiResponse<Trip[]>` to its `.data`. No manual auth header handling needed — the existing
  `authInterceptor` attaches the bearer token to every outgoing request automatically, and the
  route is already behind `authGuard` (role `traveler`).

**`trip-list.ts` rewrite.** Replace the static `trips = trips` mock import with three signals:
`trips` (starts `[]`), `loading` (starts `true`), `error` (starts `null`). The constructor
subscribes to `tripsService.listMyTrips()` once: on success, populate `trips` and clear `loading`;
on failure, set a generic `error` message and clear `loading`.

**`trip-list.html` rewrite.** Four render states driven by the three signals: loading (a simple
loading message — no skeleton shimmer, out of scope), error (inline error message, no retry
button — out of scope), empty (a "No trips yet" message plus the existing "New Trip" CTA, since
the CTA already exists in the page header), and populated (the stripped-down card grid described
above).

**`status-badge.ts` — additive only.** Add the 5 real backend status strings (`PLANNING`,
`CONFIRMED`, `ONGOING`, `COMPLETED`, `CANCELLED`) as new keys in `STATUS_CLASS_MAP`, alongside the
existing mock-only lowercase keys (`planning`, `upcoming`, `ongoing`, `completed`, plus the
non-trip keys like `Accepted`/`Paid`/`Active`), which stay untouched since other still-mocked
pages (admin, hotel, transport dashboards) still rely on them.

## Known trade-off (not fixed here)

`trip-detail.ts` (`features/trips/components/trip-detail/trip-detail.ts:67`) still reads from
mock data and falls back to `trips[0]` (the mock Goa trip) when a trip ID isn't found. Once real
trips are listed, clicking a real trip card navigates to `/trips/<real-uuid>`, which won't match
any mock ID — the user will silently see the wrong (mock) trip detail page rather than an error.
This is a known, accepted gap: migrating `trip-detail` to the real `GET /api/trips/{tripId}`
endpoint is explicitly a future, separate pass, not part of this change.

## Testing

- `trips.service.spec.ts` — `HttpTestingController`-based, mirroring `auth.service.spec.ts`'s
  pattern: `listMyTrips()` issues `GET /api/trips`, unwraps `.data` from a flushed
  `ApiResponse<Trip[]>`, and propagates an HTTP error on failure.
- `trip-list.spec.ts` — rewritten to provide a stubbed `TripsService` (no real HTTP): covers the
  loading state (before the observable emits), the populated state (cards render real fields,
  including the `Destination #<id>` placeholder and total budget), the empty state (zero trips
  returns the "No trips yet" message), the error state (a failing observable shows the error
  message), and that each card links to `/trips/<tripId>` using the real UUID.

## Verification

- `ng build` and `ng test` succeed with no regressions.
- Manual: log in as a traveler with at least one real trip (create one via `POST /api/trips` if
  none exist yet — out of scope to build a UI for this in this pass, a direct API call is enough
  for manual verification), visit `/trips`, and confirm the card shows real data with no console
  errors. Log in as a traveler with zero trips and confirm the empty state renders.

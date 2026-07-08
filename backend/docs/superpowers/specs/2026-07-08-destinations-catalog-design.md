# Destinations Catalog — Design

## Context

`Trip.destinationId` (and `Activity.destinationId`) have existed as bare, unvalidated `Integer`
columns with no backing catalog since the trip and trip-list/new-trip frontend work earlier this
session — `trip-list.html` and `new-trip.html` both had to fall back to raw
`Destination #<id>` placeholders because no lookup existed anywhere, and `api_contract.md`
documents a full `GET/POST/PUT/DELETE /api/destinations` contract that was never implemented.

This adds a real destinations catalog: backend entity + seed data + read endpoints, and wires the
frontend's two existing gaps (the `new-trip` create form's raw destination-ID number input, and
`trip-list`'s `Destination #<id>` placeholder) to real names.

**Scope, per your direction:** read-only for now (`GET /api/destinations`,
`GET /api/destinations/{id}`) plus seed data. No create/update/delete endpoints or admin UI —
there's nothing to manage them with yet; that's a future pass once an admin destinations page
exists.

## Decisions

**Entity — `admin/entity/Destination.java`, standalone (not extending `BaseEntity`).**
`BaseEntity` forces a `Long` auto-IDENTITY `id` plus audit timestamps; every existing
`destinationId` reference in the codebase (`Trip.destinationId`, `Activity.destinationId`) is
already `Integer` with no FK. Matching that type now, rather than introducing a second
incompatible ID type, sets up real FK compatibility later without a migration:

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer destinationId;
private String destinationName;  // required, max 200
private String state;            // required, max 100
private String country;          // required, max 100
private String description;      // optional, TEXT
```

**DTO** — just `DestinationResponse` (`destinationId, destinationName, state, country,
description`); no `Create`/`UpdateDestinationRequest` needed since there's no write path.

**Endpoints** (`admin/controller/DestinationController.java`, `@RequestMapping("/api/destinations")`):
- `GET /api/destinations` — any authenticated user, returns `List<DestinationResponse>`
- `GET /api/destinations/{destinationId}` — any authenticated user, 404 via
  `ResourceNotFoundException` (already handled by the existing `GlobalExceptionHandler`) if not
  found

**Seed data — via `DemoDataInitializer`** (a fresh JPA entity follows the ORM-managed seeding
style already used for `User`/`Trip`, not the legacy raw-SQL `seed_data.sql` style used for the
ported busbooking/hotel/activity data). 7 destinations, seeded in an order that deliberately
aligns the first two IDs with data that already implicitly assumed this meaning:
`seed_data.sql`'s hotels (`Grand Palace Mumbai` → `destination_id=1`, `Coastal Stays Goa` →
`destination_id=2`) and activities (`Mumbai Heritage Walking Tour` → `destinationid=1`,
`Goa Jet Ski Experience` → `destinationid=2`) already agree: **1 = Mumbai, 2 = Goa**.

| ID | Name | State |
|---|---|---|
| 1 | Mumbai | Maharashtra |
| 2 | Goa | Goa |
| 3 | Manali | Himachal Pradesh |
| 4 | Jaipur | Rajasthan |
| 5 | Alleppey | Kerala |
| 6 | Chennai | Tamil Nadu |
| 7 | Coorg | Karnataka |

**Bug fix alongside the seed data:** `DemoDataInitializer`'s "Demo Goa Trip" currently sets
`destinationId=1`, which — now that IDs carry real meaning — would mean Mumbai, not Goa. Corrected
to `2`.

**Frontend — new `core/destinations/` module** (not under `features/trips/`; this is reference
data other features will need too, e.g. hotel/activity dropdowns later):
- `destination.models.ts` — `Destination { destinationId: number; destinationName: string; state: string; country: string; description: string }`
- `destinations.service.ts` — `DestinationsService.listDestinations(): Observable<Destination[]>`, same `ApiResponse<T>` unwrap pattern as `TripsService`

**`new-trip.ts`/`.html`** — the "Destination ID" number input becomes an `hlm-select` dropdown
(same `[value]`/`(valueChange)` signal-binding pattern already used for Trip Type), loaded from
`DestinationsService` on init. Options are labeled `"<name>, <state>"`. Selecting one sets
`destinationId` directly in the `CreateTripPayload` — no more manual numeric entry. If the
destinations list fails to load, the dropdown shows a "Could not load destinations" state and the
form's submit is disabled (can't create a trip without a valid destination selected).

**`trip-list.ts`/`.html`** — also gets `DestinationsService` injected, fetches the destinations
list alongside trips, and builds a `Map<number, string>` (id → name) to resolve each trip card's
real destination name instead of the `Destination #<id>` placeholder. If destinations fail to
load (independently of the trips request), cards fall back to the old `Destination #<id>` text
rather than blocking the whole page — trips are the primary content; destination names are a
enhancement, not a hard dependency.

## Testing

**Backend** (per `coding_guidelines.md`):
- `DestinationServiceImplTest` — Mockito, mocked repository: `getAllDestinations()` returns the
  mapped list; `getDestinationById()` returns the mapped response or throws
  `ResourceNotFoundException` for an unknown ID.
- `DestinationControllerTest` — `@WebMvcTest` slice, mocked service: both endpoints return 200
  with the expected JSON shape; unauthenticated requests are rejected (401, matching every other
  endpoint's baseline).

**Frontend:**
- `destinations.service.spec.ts` — `HttpTestingController`, same pattern as `trips.service.spec.ts`.
- `new-trip.spec.ts` — updated: the destination dropdown is populated from a stubbed
  `DestinationsService`; selecting a destination and submitting sends the right `destinationId`.
- `trip-list.spec.ts` — updated: a trip card shows the real destination name once both trips and
  destinations have loaded.

## Verification

- `./mvnw test` and `ng test`/`ng build` succeed with no regressions.
- Manual: `GET /api/destinations` (with a valid bearer token) returns all 7 seeded destinations
  with `Mumbai` as ID 1 and `Goa` as ID 2. Visiting `/trips/new` shows a real destination dropdown
  instead of a number input; creating a trip with a selected destination and then viewing it in
  `/trips` shows the real destination name, not a placeholder.

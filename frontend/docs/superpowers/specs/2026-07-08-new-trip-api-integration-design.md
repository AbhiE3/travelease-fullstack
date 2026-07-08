# New Trip — Real API Integration — Design

## Context

Following [2026-07-08-trip-list-api-integration-design.md](2026-07-08-trip-list-api-integration-design.md),
this wires `new-trip.html` — the "Create a new trip" form — to the real `POST /api/trips`
endpoint. `new-trip.ts`'s `onSubmit` is currently a stub: it prevents default and navigates
straight to `/trips/goa-2026` with no HTTP call at all.

The backend's `CreateTripRequest` is, once again, leaner than the mock-era form: `tripName`,
`sourceLocation`, `destinationId: Integer`, `budgetAmount: BigDecimal`, `categoryId: Integer`,
`startDate`, `endDate`. There is no "Preferred Area" field anywhere in the backend. `categoryId`
has no lookup table or enum in the codebase, but a stale section of `api_contract.md` (never
implemented) documents a `GET /api/traveler-categories` endpoint returning exactly
`Solo, Couple, Family, Friends, Corporate` — the same 5 values already hardcoded in
`new-trip.ts`'s `tripTypes` array — and `DemoDataInitializer` seeds `categoryId=1` with no
further explanation. The best available inference is `categoryId` = 1-based index into that
list; this is not backend-validated (it's a bare `@NotNull Integer`), so it's a reasonable but
unenforced mapping, not a documented contract.

You confirmed PUT (edit trip) is out of scope for this pass — no edit-trip UI exists anywhere in
the app to drive it, and none is being built here.

## Decisions

**`TripsService.createTrip()`.** New method: `createTrip(payload: CreateTripPayload):
Observable<Trip>`, `POST /api/trips`, unwrapping `ApiResponse<Trip>.data` — same pattern as
`listMyTrips()`.

**`CreateTripPayload` type** (added to `trip.models.ts`): `{ tripName: string; sourceLocation:
string; destinationId: number; budgetAmount: number; categoryId: number; startDate: string;
endDate: string }`, field-for-field matching `CreateTripRequest`.

**Shared `ApiResponse` fix.** `core/api/api-response.model.ts`'s error shape currently omits
`details` (only `{ code, message }`), because login errors never needed it. The backend's real
`ApiError` record is `{ code, message, details: List<String> }` — validation failures (e.g. a
budget below the `@DecimalMin(0.01)` floor) populate `details` with one string per invalid field.
Add `details?: string[]` to the shared type so this form (the first one that can trigger
multi-field validation errors) can surface them.

**Form field changes (`new-trip.html`):**
- Remove "Preferred Area" entirely — no backend field exists for it.
- "Destination" (currently free-text "Goa") becomes a number input labeled "Destination ID",
  with a small caption noting no name-based lookup exists yet (consistent with trip-list already
  rendering `Destination #<id>` for the same reason).
- "Budget per Person (₹)" → "Budget (₹)" — it's the trip's total budget, not per-person (same
  relabel trip-list already applied).
- Trip Type `hlm-select` switches from a static `[value]="'Friends'"` to `[value]`/`(valueChange)`
  bound to a component signal, so the selected value is readable at submit time.
- Every input gets a template reference variable (`#nameInput`, `#budgetInput`, `#sourceInput`,
  `#destinationInput`, `#startDateInput`, `#endDateInput`) — the same lightweight pattern
  `login.html` already uses, no reactive forms introduced.

**`new-trip.ts` rewrite.** Adds `error`/`submitting` signals (mirroring `Login`). `onSubmit`
reads the template refs, computes `categoryId` as `tripTypes.indexOf(selectedType) + 1`, builds
the `CreateTripPayload`, and calls `tripsService.createTrip(...)`. On success, navigates to
`/trips/<real-uuid>` — this currently lands on the mock trip-detail page's fallback (same known,
already-accepted trade-off from the trip-list design, not re-litigated here). On failure, sets
`error` to the backend's `error.message`, plus each `details` entry appended on its own line if
present.

## Testing

- `trips.service.spec.ts` gains a `createTrip` test: posts the exact payload shape, unwraps the
  created `Trip` from the response.
- `new-trip.spec.ts` rewritten: a successful submit calls `TripsService.createTrip` with the
  expected payload (including the derived `categoryId`) and navigates to `/trips/<returned
  tripId>`; a validation failure (simulated `HttpErrorResponse` with `details`) displays every
  detail line and does not navigate.

## Verification

- `ng build` and `ng test` succeed with no regressions.
- Manual: log in as a traveler, visit `/trips/new`, submit valid values, confirm a real trip is
  created (`GET /api/trips` afterward shows it) and the browser navigates to its real UUID URL.
  Submit with an invalid budget (e.g. `0`) and confirm the validation error displays inline
  without navigating.

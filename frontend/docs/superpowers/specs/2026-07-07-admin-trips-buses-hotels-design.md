# Admin — Trips + Buses + Hotels — Design

## Context

Third of 4 sub-projects covering the Admin role, following
[2026-07-06-admin-approvals-users-design.md](2026-07-06-admin-approvals-users-design.md). Covers
the `'trips'`, `'buses'`, and `'hotels'` children of `admin.routes.ts` (currently
`RoutePlaceholder`). This is the first Admin sub-project to introduce a Dialog ("Add X") pattern,
reusing the exact `HlmDialogImports` structure already verified in the Trip Detail Members tab's
"Invite Member" dialog. Only the Route Analytics + Partner Analytics + Booking Funnel sub-project
remains after this one.

Source: `trip-weaver-83-main/src/routes/admin.trips.tsx`, `admin.buses.tsx`, `admin.hotels.tsx`.

## Decisions

- **Naming/structure**: `features/admin/components/<name>/<name>.ts`, no "Page" suffix.
- **No new icons** — `Plus`, `Star`, `MapPin` are all already registered in `app.config.ts`.
- **`AdminBuses`'s two quirks preserved verbatim**: every fleet row shows the same hardcoded route
  text ("Bengaluru → Goa"), regardless of the bus's actual data (there is no per-bus route field
  in `buses`); and each row's status comes from a hardcoded by-index array
  (`["On Time", "Delayed", "On Time"][i] ?? "On Time"`), not any data field — with exactly 3
  `buses` entries in mock data, every index resolves without hitting the fallback, but the
  fallback is kept for fidelity.
- **The bus status badge is plain markup, not `StatusBadge`**: same reasoning as `HotelProperties`'
  "Live" badge — it's derived from array position, not a status field, so routing it through the
  shared status-driven component would be misleading.
- **Both "Add Bus" and "Add Hotel" dialogs are non-functional forms** — all fields and the save
  button have no handlers, matching React exactly. Both reuse the same `HlmDialogImports` +
  `HlmInputImports` + `HlmLabelImports` structure already verified working.
- **`AdminTrips` reuses the existing global `trips` array** and `StatusBadge` (already supports
  `upcoming`/`planning`/`ongoing`/`completed` from the Dashboard sub-project) — no new data, no
  `StatusBadge` changes.

## Scope

**In scope:**

- `AdminTrips` (`features/admin/components/admin-trips/admin-trips.ts`, route `'trips'`):
  `PageHeader`; a table over the full `trips` array — thumbnail image, name + source→destination,
  type, dates, member count, computed budget (`budgetPerPerson * members`), `StatusBadge`.
- `AdminBuses` (`features/admin/components/admin-buses/admin-buses.ts`, route `'buses'`):
  `PageHeader` with a non-functional "Add Bus" `Dialog` action (6 fields: Bus Name, Operator,
  Seats, Source, Destination, Price); a "Fleet" table over the full `buses` array — name,
  operator, hardcoded route text, seats, price, rating, by-index status badge.
- `AdminHotels` (`features/admin/components/admin-hotels/admin-hotels.ts`, route `'hotels'`):
  `PageHeader` with a non-functional "Add Hotel" `Dialog` action (6 fields: Hotel Name, Area,
  Capacity, Rooms, Price, Rating); a grid over the full `hotels` array — image, name, area,
  rating, capacity/rooms/price stats, non-functional Edit/Manage buttons.
- `admin.routes.ts` modified in place: its remaining 3 `RoutePlaceholder` children swap for the
  real components above, `data: { title }` dropped from all 3. This closes out every child except
  the final analytics sub-project.

**Explicitly out of scope:**

- Route Analytics, Partner Analytics, Booking Funnel — the final, separate sub-project.
- Any real bus/hotel creation, editing, or trip-status change — matching React, none of these are
  wired up.

## Testing

- `AdminTrips`: renders every trip's name and its computed budget value.
- `AdminBuses`: renders every bus's name and price; the hardcoded route text appears once per
  row; the by-index status mapping is tested as a pure function (`On Time`/`Delayed`/`On Time`
  for indices 0/1/2, falling back to `On Time` for any index beyond that); shows the "Add Bus"
  dialog trigger (render-only check, consistent with how the Invite Member dialog was tested —
  no open/interact behavior).
- `AdminHotels`: renders every hotel's name, area, capacity, rooms, and price; shows the "Add
  Hotel" dialog trigger.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/admin/trips`, `/admin/buses`, `/admin/hotels` in the browser shows real content (not
  the "coming soon" placeholder) matching the React source's layout and data.

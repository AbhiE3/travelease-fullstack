# Transport Partner Dashboard — Design

## Context

Second of the 4 remaining role dashboards (Activity → Transport → Hotel → Admin). Covers all 5
`/transport/*` routes, currently rendering `RoutePlaceholder`: the dashboard home, Manage
Vehicles, Manage Routes, Bookings, and Reports. `AppShell`'s `transport` role nav already exists
and points at these exact 5 paths; `transport.routes.ts` already exists as a single-file role
route array (same shape as `activity.routes.ts`), so this sub-project modifies it in place.

Source: `trip-weaver-83-main/src/routes/transport{,.vehicles,.routes,.bookings,.reports}.tsx`.

## Decisions

- **Naming/structure**: `features/transport/components/<name>/<name>.ts`, no "Page" suffix.
- **Dropped the pathname-check + `<Outlet/>` hack** in the dashboard route, same rationale as the
  Activity sub-project: Angular's nested child routes already only render the one leaf component
  matching the active path.
- **`TransportBookings`'s booking list stays component-local**, not promoted to
  `@app/core/mock-data` — hardcoded in the React route file too.
- **Two hardcoded-but-not-computed quirks preserved verbatim**: the Dashboard's "Seats Booked"
  (`1,284`) and "Upcoming Trips" (`47`) stats are literal strings in React, not derived from
  `vehicles`/`partnerRoutes` — kept as literal strings here too, unlike `ActivityDashboard` where
  the equivalent stats were genuinely computed.
- **`TransportReports` is entirely hardcoded**: 4 stat cards (`Occupancy 82%`, `Revenue MTD
  ₹12.4L`, `Trips Completed 186`, `Avg Rating 4.6`) and a 12-bar "Weekly Bookings" chart with
  decorative bar heights from `30 + abs(sin(i*0.7)*70)` for `i` in `0..11` — no real data source
  at all, ported verbatim including the exact formula.
- **`StatusBadge` gains two new entries**: `Active` → `'bg-success/10 text-success
  border-success/20'` (identical class to the existing `Confirmed`, different status label) and
  `Maintenance` → `'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20'` (identical class
  to the existing `Pending`). Vehicle status uses its own vocabulary (`Active`/`Maintenance`)
  distinct from booking status (`Confirmed`/`Pending`), so both need their own map keys even
  though the visual treatment is shared.
- **No new icons** — `Bus`, `Users`, `Plane`, `Wallet`, `Plus` are all already registered in
  `app.config.ts`.
- **Non-functional actions preserved as non-functional**: "Add Vehicle" and "Add Route" have no
  click handlers in React, so neither does here.

## Scope

**In scope:**

- `TransportDashboard` (`features/transport/components/transport-dashboard/transport-dashboard.ts`,
  route `''`): `PageHeader`; 4 stat cards — Total Buses (`vehicles.length`, computed), Seats
  Booked (`"1,284"`, hardcoded), Upcoming Trips (`"47"`, hardcoded), Revenue MTD (`"₹12.4L"`,
  hardcoded); a "Route Occupancy" card with one row per `partnerRoutes` entry — route name,
  departures/wk, revenue, occupancy % — and a progress bar toned by occupancy (`>80` → success,
  `>60` → primary, else → warning).
- `ManageVehicles` (`features/transport/components/manage-vehicles/manage-vehicles.ts`, route
  `'vehicles'`): `PageHeader` with non-functional "Add Vehicle" action; a 2-column grid of every
  `vehicles` entry — bus icon, name, reg + capacity, `StatusBadge` for `Active`/`Maintenance`.
- `ManageRoutes` (`features/transport/components/manage-routes/manage-routes.ts`, route
  `'routes'`): `PageHeader` with non-functional "Add Route" action; a table of every
  `partnerRoutes` entry — route name, departures/wk, occupancy %, revenue.
- `TransportBookings` (`features/transport/components/transport-bookings/transport-bookings.ts`,
  route `'bookings'`): `PageHeader`; a 6-column table (Passenger/Route/Date/Seats/Total/Status)
  over 4 hardcoded local bookings, status via `StatusBadge`.
- `TransportReports` (`features/transport/components/transport-reports/transport-reports.ts`,
  route `'reports'`): `PageHeader`; 4 hardcoded stat cards; a "Weekly Bookings" card with 12
  decorative bars per the sine-wave formula above.
- `transport.routes.ts` modified in place: its 5 `RoutePlaceholder` children become
  `loadComponent` references to the 5 real components above; `data: { title }` entries dropped.
- `StatusBadge` gains two new map entries: `Active`, `Maintenance`.

**Explicitly out of scope:**

- Any real vehicle/route creation, editing, or booking-status change — matching React, none of
  these are wired up.
- Hotel and Admin dashboards — separate, later sub-projects.

## Testing

- `TransportDashboard`: renders every `partnerRoutes` entry's route name and the two hardcoded
  stat strings; occupancy tone tiers (`>80` success, `>60` primary, else warning) are tested as a
  pure function, since current mock data (`88, 64, 72, 91`) has no entry `≤60` to exercise the
  warning tier through real data alone.
- `ManageVehicles`: renders every vehicle's name and reg; an `Active` vehicle and the
  `Maintenance` vehicle get visibly different `StatusBadge` classes.
- `ManageRoutes`: renders every route's name, departures, occupancy, and revenue.
- `TransportBookings`: renders every local booking's passenger and route; `Confirmed` and
  `Pending` rows get visibly different `StatusBadge` classes.
- `TransportReports`: renders all 4 hardcoded stat values; all 12 bars render with heights
  matching `30 + abs(sin(i*0.7)*70)` for their index.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/transport`, `/transport/vehicles`, `/transport/routes`, `/transport/bookings`,
  `/transport/reports` in the browser shows real content (not the "coming soon" placeholder)
  matching the React source's layout and data.

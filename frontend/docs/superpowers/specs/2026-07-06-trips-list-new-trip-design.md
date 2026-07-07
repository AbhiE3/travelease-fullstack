# Trips List + New Trip — Design

## Context

Second sub-project of the "Feature modules" phase, after
[2026-07-06-dashboard-page-design.md](2026-07-06-dashboard-page-design.md). "Trips" was flagged
during brainstorming as too large for one spec: the React source has `trips.tsx` (52 lines, list),
`trips.new.tsx` (60 lines, form), and `trips.$tripId.tsx` (**529 lines**, 8 tabs — Overview,
Members, Travel, Accommodation, Expenses, Itinerary, Alerts, Reviews). Agreed sequencing: this
spec covers List + New Trip only; Trip Detail gets its own dedicated brainstorming session and
likely its own further breakdown once we're there.

**Folder/naming convention change (mid-brainstorm):** the user pointed at a separate reference
project (`/Users/neeharika/Dev/temp-dev/travelease-fullstack/frontend`) and asked us to adopt its
file/folder convention for new feature work going forward:
`features/<domain>/components/<name>/<name>.ts`, with class names that have **no "Page" suffix**
(e.g. `TripList`, not `TripListPage` — unlike the `DashboardPage` built in the prior sub-project).
Per the user's explicit clarification, this adoption is **folder/naming convention only** — we
are not adopting that reference project's actual code, its real-auth `AppShell`, its spartan
`Sidebar`-based layout, or any of its extra routes/features. Already-completed sub-projects
(Foundation, UI Component Library, App Shell & Auth Layout, Dashboard Page) are not touched or
renamed to match. `shared/ui/` (not the reference's `shared/components/`) stays our convention
for cross-page helpers, since `StatusBadge`/`DestinationPill` already live there.

## Decisions

- **Adopt `features/<domain>/components/<name>/<name>.ts` naming for Trips onward.** New route
  paths stay exactly as already defined in Foundation (`''`, `'new'`, `':tripId'` under
  `trips.routes.ts`) — only the file/class naming style changes, not routes or behavior. `new-trip`
  (not the reference's `create-trip`) since our route path is `/trips/new`, matching the React
  source.
- **`PageHeader` stays in `shared/ui/`**, consistent with `StatusBadge`/`DestinationPill`, not
  moved to a new `shared/components/` folder.
- **`TripList` shows all trips**, not filtered — this is the one place a full trip list appears
  (contrast with Dashboard's upcoming-only section).
- **`NewTrip`'s form stays non-functional**, matching React exactly: no validation, no real data
  creation, submitting always navigates to the hardcoded `/trips/goa-2026`.
- **Verified `Select` usage via a real throwaway build** before writing this spec (not guessed):
  `<hlm-select [value]="...">` wraps a `<hlm-select-trigger>` and an
  `<ng-template hlmSelectPortal><hlm-select-content>...</hlm-select-content></ng-template>`.

## Scope

**In scope:**

- `PageHeader` (`src/app/shared/ui/page-header/page-header.ts`): required `title: string` input,
  optional `subtitle: string` input, and an `action` content-projection slot
  (`<ng-content select="[action]">`) for arbitrary action content (a button, in both consumers).
- `TripList` (`src/app/features/trips/components/trip-list/trip-list.ts`): `PageHeader` with a
  "New Trip" button action linking to `/trips/new`; a card grid over **all** `trips` from
  `@app/core/mock-data`, each showing the trip image with a `StatusBadge` overlay, name +
  `DestinationPill`, a date/members/budget-per-person row, and a progress bar; each card links to
  `/trips/:tripId`.
- `NewTrip` (`src/app/features/trips/components/new-trip/new-trip.ts`): a "Back to trips" link to
  `/trips`; `PageHeader` (title/subtitle only); a `Card` wrapping a form with `Input`/`Label`
  fields (trip name, budget per person, source, destination, start date, end date) and two
  `Select` fields (trip type: Solo/Couple/Family/Friends/Corporate, defaulting to Friends;
  preferred area: Baga Beach/Calangute/Vagator/Candolim, defaulting to Baga Beach); submitting
  navigates to `/trips/goa-2026`; a Cancel button links back to `/trips`.
- `trips.routes.ts` updated: `''` → `TripList`, `'new'` → `NewTrip` (both still lazy-loaded via
  `loadComponent`, matching the existing convention). `':tripId'` is unchanged (still
  `RoutePlaceholder`).

**Explicitly out of scope:**

- The Trip Detail page (`:tripId` route) — its own future sub-project.
- Real form validation, real trip creation/persistence, or any backend integration.
- The reference project's extra routes (`edit/:id`, `invite/:id`) — not present in the original
  React app, not being adopted here.

## Testing

- `PageHeader`: renders `title`/`subtitle` text; renders projected `action` content when
  provided.
- `TripList`: renders all trips from mock data (not a filtered subset); each card links to the
  correct `/trips/:tripId`.
- `NewTrip`: submitting the form navigates to `/trips/goa-2026` (verified via a router
  navigation spy).
- `trips.routes.spec.ts`: updated to confirm `''` lazily loads `TripList`, `'new'` lazily loads
  `NewTrip`, and `':tripId'` still lazily loads `RoutePlaceholder`.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Navigating to `/trips` shows the real trip list (all 3 mock trips) inside the traveler
  `AppShell`. Navigating to `/trips/new` shows the real form. Submitting it navigates to
  `/trips/goa-2026` (still a bare placeholder heading, since Trip Detail isn't built yet).

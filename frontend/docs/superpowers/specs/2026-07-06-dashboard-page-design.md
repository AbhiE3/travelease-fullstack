# Dashboard Page — Design

## Context

First sub-project of the "Feature modules" phase of the trip-weaver-83-main → Angular
migration (see [2026-07-04-angular-foundation-design.md](2026-07-04-angular-foundation-design.md),
[2026-07-06-ui-component-library-design.md](2026-07-06-ui-component-library-design.md), and
[2026-07-06-app-shell-auth-layout-design.md](2026-07-06-app-shell-auth-layout-design.md) for
prior context). Foundation, the UI Component Library, and App Shell & Auth Layout are all
complete and verified — every route currently resolves to a bare `RoutePlaceholder` inside the
correct role-based `AppShell` (or `AuthLayout`, or no shell for the landing/404 pages).

"Feature modules" — giving every route real page content instead of a placeholder — covers 7
independent areas (dashboard, trips, misc traveler pages, activity, hotel, transport, admin) and
is too large for one spec. Sequencing agreed with the user:

1. **Dashboard** (this spec) — smallest, most representative slice
2. Trips (list, new, detail — the detail page is the largest single page in the app and may
   split further)
3. Misc traveler pages (expenses, profile, notifications, invitations)
4. Activity / Hotel / Transport partner dashboards
5. Admin (broadest, most complex — last)

The React `dashboard.tsx` (157 lines) wraps itself in `<AppShell role="traveler">` and renders:
a hero banner, 4 stat cards, an "Upcoming Trips" list, a "Recent Activity" feed, "Pending
Invitations", a "Budget Summary", and a "Notifications" preview — reading `trips`, `invitations`,
and `notifications` from `mock-data.ts`. It also uses two helpers from `app-shell.tsx` that were
deferred in the App Shell sub-project: `StatusBadge` and `DestinationPill` (not `PageHeader` —
this page doesn't use it).

## Decisions

- **No new spartan-ng components needed.** `Card`, `Button`, `Progress`, `Avatar` are all
  already in `libs/ui/` from the UI Component Library sub-project.
- **Build `StatusBadge` and `DestinationPill` now**, as the first real consumers — matching the
  App Shell spec's decision to defer them until a feature page actually needs one.
- **Port hardcoded copy verbatim, not computed.** The React source hardcodes several numbers
  that *could* be derived from mock data (e.g. "Active Trips: 2", "Your Goa trip starts in 28
  days") but aren't — they're authored content, not `trips.length` or a date calculation. The
  Angular port keeps them exactly as static text/arrays rather than "improving" them into
  computed logic, which would be a behavior change beyond porting.
- **No routing/AppShell changes in this page.** Unlike React (which wraps itself in `AppShell`
  per-page), the Angular `DashboardPage` component is pure content — the shell wrapping already
  happens at the route level (`traveler.routes.ts`, from the App Shell sub-project). The page
  component only needs to render inside the shell's `<router-outlet>`.
- **`dashboard-page/` subfolder**, not a flat file, so the pattern stays consistent once Trips
  needs three page components (list/new/detail) in the same feature folder.

## Scope

**In scope:**

- `StatusBadge` (`src/app/shared/ui/status-badge/status-badge.ts`): takes a `status` input,
  renders the existing `Badge` (`variant="outline"`) with an added color class based on a status
  → class map ported from React (`Accepted`/`Paid` → success, `Pending`/`planning` → warning,
  `Rejected` → destructive, `upcoming` → primary, `ongoing` → accent, `completed` → muted,
  anything unmatched → no extra class).
- `DestinationPill` (`src/app/shared/ui/destination-pill/destination-pill.ts`): takes `from`/`to`
  inputs, renders `<MapPin icon> from → to` text. Needs `lucideMapPin` registered in
  `app.config.ts` (the one icon deliberately skipped in the App Shell sub-project since nothing
  used it yet).
- `DashboardPage` (`src/app/features/dashboard/dashboard-page/dashboard-page.ts`): the full page
  content described above, reading `trips`, `invitations`, `notifications` from
  `@app/core/mock-data`. The "Upcoming Trips" section filters `trips` to
  `status === 'upcoming' || status === 'planning'`. The "Notifications" section shows only the
  first 3 entries. "Recent Activity" and the "Budget Summary" numbers are hardcoded local data
  (matching React — neither is in `mock-data.ts`). Trip cards link to `/trips/:tripId`;
  invitation cards render static (non-functional) Accept/Decline buttons, matching React exactly
  (no click handlers there either).
- `dashboard.routes.ts` updated to `loadComponent` the new `DashboardPage` instead of
  `RoutePlaceholder`.

**Explicitly out of scope:**

- Any other feature page (Trips, misc pages, partner dashboards, Admin) — later sub-projects.
- `PageHeader` — still not needed by any page yet.
- Real Accept/Decline behavior, real budget calculations, or any backend/API integration.

## Testing

- `StatusBadge`: tests for the status → class mapping across a representative set
  (`Accepted`, `Pending`, `Rejected`, `upcoming`) plus an unmatched status falling back to no
  extra class.
- `DestinationPill`: minimal render test confirming the `from → to` text appears.
- `DashboardPage`: tests for the one real piece of logic (filtering `trips` to
  `upcoming`/`planning`, confirming a `completed` trip is excluded), plus checks that invitations
  render and notifications are capped at 3.
- `dashboard.routes.spec.ts` updated to confirm the route now lazily loads `DashboardPage`.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Navigating to `/dashboard` shows the full real page (hero banner, stat cards, upcoming trips
  with status badges and destination pills, recent activity, invitations, budget summary,
  notifications) inside the traveler `AppShell`, instead of a bare placeholder heading.

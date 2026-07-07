# Activity Partner Dashboard — Design

## Context

First of the 4 remaining role dashboards (Activity → Transport → Hotel → Admin, smallest-first).
Covers all 5 `/activity/*` routes, currently rendering `RoutePlaceholder`: the dashboard home,
Manage Activities, Bookings, Capacity, and Reports. `AppShell`'s `activity` role nav already
exists and points at these exact 5 paths; `activity.routes.ts` already exists as a single-file
role route array (not nested under a parent domain like the traveler pages were), so this
sub-project modifies it in place rather than adding a further layer of routing indirection.

Source: `trip-weaver-83-main/src/routes/activity{,.activities,.bookings,.capacity,.reports}.tsx`.

## Decisions

- **Naming/structure**: `features/activity/components/<name>/<name>.ts`, no "Page" suffix,
  matching the established convention.
- **Dropped the pathname-check + `<Outlet/>` hack**: React's `/activity` route only renders its
  dashboard content when `pathname === "/activity"`, otherwise renders `<Outlet/>` for the active
  child route — a workaround for how TanStack Router's layout routes compose. Angular's nested
  child routes already only render the one leaf component matching the active path, so this
  sub-project has no equivalent layout-route problem and the hack is dropped rather than ported.
  This is the one deliberate deviation from 1:1 porting in this sub-project — everything else is
  ported verbatim, including quirks.
- **`ActivityBookings`'s booking list stays component-local**, not promoted to
  `@app/core/mock-data` — it's hardcoded directly in the React route file too (`const ab = [...]`),
  not exported from React's `lib/mock-data.ts` either.
- **`StatusBadge` gains a new `Confirmed` entry**: `'bg-success/10 text-success border-success/20'`
  — distinct from `Accepted`/`Paid`'s `/15` opacity, matching the exact class React uses for this
  page's Confirmed badge. Worth extending the shared component (rather than duplicating tone
  logic locally) since booking-style statuses will very likely recur in Transport/Hotel/Admin.
- **No new icons** — `Activity`, `CalendarDays`, `Users`, `Wallet`, `Plus`, `Star` are all already
  registered in `app.config.ts`.
- **Non-functional actions preserved as non-functional**: "Add Activity" and "Edit" have no click
  handlers in React, so neither does here.

## Scope

**In scope:**

- `ActivityDashboard` (`features/activity/components/activity-dashboard/activity-dashboard.ts`,
  route `''`): `PageHeader`; 4 stat cards computed from `providerActivities` (Activities Listed —
  count, Bookings Received — sum of `booked`, Available Slots — sum of `slots - booked`, Revenue
  MTD — `₹{(sum of booked*price / 1000).toFixed(0)}k`); an "Activity Occupancy" card with one
  progress bar per `providerActivities` entry, bar tone by `(booked/slots)*100`: `>80` → success,
  `>50` → primary, else → warning.
- `ManageActivities` (`features/activity/components/manage-activities/manage-activities.ts`,
  route `'activities'`): `PageHeader` with non-functional "Add Activity" action; a responsive
  grid of every `activities` mock entry — image, name, destination + duration, star rating,
  price, non-functional "Edit" button.
- `ActivityBookings` (`features/activity/components/activity-bookings/activity-bookings.ts`,
  route `'bookings'`): `PageHeader`; a 7-column table (Customer/Activity/Date/Slot/Guests/
  Total/Status) over 4 hardcoded local bookings, status rendered via `StatusBadge`.
- `ActivityCapacity` (`features/activity/components/activity-capacity/activity-capacity.ts`,
  route `'capacity'`): `PageHeader`; a table with one row per `providerActivities` entry and one
  column per hardcoded timeslot (`08:00`, `10:00`, `12:00`, `14:00`, `16:00`) plus a Total column
  — each cell's `cap`/`used` computed with the exact formula from React
  (`cap = max(2, floor(slots/5))`, `used = min(cap, floor((booked/slots)*cap) + (colIndex % 2))`),
  shown as `used/cap` text plus a small occupancy bar (tone: `>80%` → success, else → primary).
- `ActivityReports` (`features/activity/components/activity-reports/activity-reports.ts`, route
  `'reports'`): `PageHeader`; a "Revenue per Activity" card with one bar per `providerActivities`
  entry, width `(booked*price / maxRevenueAcrossActivities) * 100`.
- `activity.routes.ts` modified in place: its 5 `RoutePlaceholder` children become `loadComponent`
  references to the 5 real components above; their now-unneeded `data: { title }` entries are
  dropped (matching what was done for the traveler misc pages).
- `StatusBadge` (`shared/ui/status-badge/status-badge.ts`) gains one new map entry: `Confirmed`.

**Explicitly out of scope:**

- Any real activity creation/editing, capacity adjustment, or booking-status change — matching
  React, none of these are wired up.
- Transport, Hotel, and Admin dashboards — separate, later sub-projects.

## Testing

- `ActivityDashboard`: all 4 stat values compute correctly from `providerActivities`; an activity
  with `>80%` occupancy gets the success tone and one with `≤50%` gets the warning tone.
- `ManageActivities`: renders every `activities` entry's name and price.
- `ActivityBookings`: renders every local booking's customer and activity name; a `Confirmed` row
  and the `Pending` row get visibly different `StatusBadge` classes.
- `ActivityCapacity`: for a couple of `(activity, slot-index)` cells, the rendered `used/cap`
  matches the formula computed independently in the test.
- `ActivityReports`: the activity with the highest revenue renders a full-width (100%) bar; a
  lower-revenue activity renders a proportionally narrower one.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/activity`, `/activity/activities`, `/activity/bookings`, `/activity/capacity`,
  `/activity/reports` in the browser shows real content (not the "coming soon" placeholder)
  matching the React source's layout and data.

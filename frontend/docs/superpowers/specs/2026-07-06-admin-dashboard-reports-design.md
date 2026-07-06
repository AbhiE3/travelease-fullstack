# Admin — Dashboard + Reports — Design

## Context

First of 4 sub-projects covering the Admin role (Dashboard+Reports → Approvals+Users →
Trips+Buses+Hotels → Route Analytics+Partner Analytics+Booking Funnel), the broadest and final
phase of the migration. `AppShell`'s `admin` role nav and `admin.routes.ts` (all 10 children on
`RoutePlaceholder`) already exist. This sub-project touches only the `''` (dashboard) and
`'reports'` children — the other 8 stay on `RoutePlaceholder` until their respective
sub-projects.

Source: `trip-weaver-83-main/src/routes/admin.tsx`, `admin.reports.tsx`.

## Decisions

- **Naming/structure**: `features/admin/components/<name>/<name>.ts`, no "Page" suffix.
- **Dropped the pathname-check + `<Outlet/>` hack** on the dashboard route, same rationale as
  every prior dashboard sub-project.
- **Both pages are entirely hardcoded** — neither imports any mock data. This is intentional in
  React: these are platform-aggregate stats with no single natural backing array, unlike
  per-domain pages (Trips, Bookings, etc.).
- **Two separate hardcoded destination lists, not unified into one**: `AdminDashboard`'s "Popular
  Destinations" (5 entries, field `pct`, bar width `${pct}%`) and `AdminReports`'s "Top
  Destinations" (6 entries — includes Jaipur, which Dashboard's list doesn't — field `trips`, bar
  width `${trips}%`) are distinct arrays in React with overlapping-but-different data and
  different field names. Kept as two separate local constants rather than invented as a shared
  one, since React never shares them either.
- **No new icons** — `Users`, `Plane`, `Bus`, `Hotel`, `TrendingUp`, `Wallet` are all already
  registered in `app.config.ts`.

## Scope

**In scope:**

- `AdminDashboard` (`features/admin/components/admin-dashboard/admin-dashboard.ts`, route `''`):
  `PageHeader`; 6 stat cards, each with a label, value, icon, and a trend string (`Total Trips
  248 / +12%`, `Active Users 1,842 / +8%`, `Revenue (MTD) ₹6.4L / +18%`, `Buses 36 / —`, `Hotels
  89 / +3`, `Bus Occupancy 82% / +5%`); a "Bookings (last 30 days)" card with 30 decorative bars,
  height per index `i` from `30 + abs(sin(i*0.7)*70) + (i%4)*5`; a "Popular Destinations" card
  with 5 hardcoded name/percentage rows and bars.
- `AdminReports` (`features/admin/components/admin-reports/admin-reports.ts`, route `'reports'`):
  `PageHeader`; 6 stat cards (label + value only, no trend: `Total Trips 248`, `Active Users
  1,842`, `Revenue ₹6.4L`, `Bus Occupancy 82%`, `Hotel Occupancy 76%`, `Growth (MoM) +18%`); a
  "Revenue Trend" card with a hardcoded SVG polyline chart (fixed coordinates, distinct from
  `AdminDashboard`'s bar chart and from `HotelReports`'s chart); a "Top Destinations" card with 6
  hardcoded name/trip-count rows and bars.
- `admin.routes.ts` modified in place: only its `''` and `'reports'` children swap
  `RoutePlaceholder` for the real components above, with `data: { title }` dropped from those two.
  The other 8 children are untouched.

**Explicitly out of scope:**

- The other 8 `/admin/*` pages — separate, later sub-projects in this same phase.
- Any real chart interactivity — matching React, none of this is wired up.

## Testing

- `AdminDashboard`: renders all 6 stat labels, values, and trend strings; all 30 bar heights match
  the sine-based formula (tested as a pure function against synthetic indices); renders all 5
  Popular Destinations names and percentages.
- `AdminReports`: renders all 6 stat labels and values (no trend); the SVG polyline `points`
  attributes match the exact hardcoded coordinate strings; renders all 6 Top Destinations names
  and trip counts.
- `admin.routes.ts`: only the `''` and `'reports'` children resolve to the real components; the
  remaining 8 still resolve to `RoutePlaceholder`.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/admin` and `/admin/reports` in the browser shows real content (not the "coming soon"
  placeholder) matching the React source's layout and data; the other 8 `/admin/*` routes still
  show placeholders (expected — out of scope here).

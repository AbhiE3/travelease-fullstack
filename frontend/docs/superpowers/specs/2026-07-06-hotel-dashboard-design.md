# Hotel Partner Dashboard — Design

## Context

Third of the 4 remaining role dashboards (Activity → Transport → Hotel → Admin). Unlike Activity's
and Transport's dashboards (~55-58 lines each), Hotel's dashboard route is 113 lines with 4
distinct widget sections, so it's split out as its own sub-project — the other 5 Hotel pages
(Properties, Rooms, Bookings, Reviews, Reports), all simple list/table pages, will follow as a
separate bundled sub-project once this one is done.

`AppShell`'s `hotel` role nav already exists and points at all 6 `/hotel/*` paths; `hotel.routes.ts`
already exists as a single-file role route array with all 6 children on `RoutePlaceholder`. This
sub-project replaces only the `''` (dashboard) child — the other 5 stay on `RoutePlaceholder`
until the follow-up sub-project.

Source: `trip-weaver-83-main/src/routes/hotel.tsx`.

## Decisions

- **Naming/structure**: `features/hotel/components/hotel-dashboard/hotel-dashboard.ts`, no "Page"
  suffix.
- **Dropped the pathname-check + `<Outlet/>` hack**, same rationale as Activity/Transport:
  Angular's nested child routes already only render the one leaf component matching the active
  path.
- **"Bookings Today" is `hotelBookings.length` verbatim** — not filtered by any date, despite the
  label. This is React's literal behavior (a static-prototype simplification, not a bug worth
  fixing), preserved as-is.
- **The 28-day occupancy calendar uses a synthetic formula**, ported verbatim:
  `occ = 30 + abs(sin(i * 0.9) * 60) + (i % 5) * 4` for `i` in `0..27`, day number `i + 1`. No real
  calendar/date data — it's a decorative visualization, same treatment as Transport's weekly bar
  chart.
- **Calendar cell background uses CSS `color-mix`**: `color-mix(in oklab, var(--primary) {occ *
  0.6}%, var(--card))`, computed per-cell and bound via `[style.background]`. Text tone flips at
  `occ > 60` (`text-primary-foreground` vs `text-foreground`).
- **Recent Bookings uses `hotelBookings.slice(0, 4)`** — currently a no-op since the mock array
  has exactly 4 entries, but the `.slice(0, 4)` call is kept (matching React) rather than removed,
  since it documents an intentional cap that would matter if the mock data grew.
- **Guest Rating Snapshot is fully hardcoded**: average `4.7`, `182 reviews`, and a 5-star
  distribution `[72, 18, 6, 2, 2]` indexed by `[5 - s]` for star value `s` in `5,4,3,2,1` (so
  `s=5` → index `0` → `72%`, down to `s=1` → index `4` → `2%`) — ported verbatim, including the
  index-flip trick.
- **No new icons** — `DoorOpen`, `Hotel`, `CalendarDays`, `Wallet`, `Star` are all already
  registered in `app.config.ts`.

## Scope

**In scope:**

- `HotelDashboard` (`features/hotel/components/hotel-dashboard/hotel-dashboard.ts`, route `''`):
  - `PageHeader`.
  - 4 stat cards: Total Rooms (`rooms` sum of `total`), Available Rooms (`rooms` sum of
    `available`), Bookings Today (`hotelBookings.length`), Revenue MTD (`₹{(sum of
    hotelBookings.total / 1000).toFixed(0)}k`).
  - "Occupancy Calendar — June" card: a 7-column grid of day-of-week headers plus 28 day cells,
    each showing day number and `occ.toFixed(0)}%`, colored via the `color-mix` formula above.
  - "Recent Bookings" card: `hotelBookings.slice(0, 4)`, each row showing guest, room + check-in,
    and a `Confirmed`/`Pending` `StatusBadge`.
  - "Room Inventory" card: a grid over the full `rooms` array — type, price/night, available/total,
    and a progress bar for `(total - available) / total`.
  - "Guest Rating Snapshot" card: the hardcoded `4.7` average, `182 reviews`, and 5 percentage bars
    for star ratings 5 down to 1.
  - `hotel.routes.ts`'s `''` child modified in place: `RoutePlaceholder` → `HotelDashboard`,
    `data: { title }` dropped. The other 5 children are untouched.

**Explicitly out of scope:**

- The other 5 `/hotel/*` pages (Properties, Rooms, Bookings, Reviews, Reports) — separate,
  immediately-following sub-project.
- Any real calendar interactivity, booking-status change, or inventory editing — matching React,
  none of these are wired up.
- Admin dashboard — separate, later sub-project.

## Testing

- Stat values (Total Rooms, Available Rooms, Bookings Today, Revenue MTD) compute correctly from
  `rooms`/`hotelBookings`.
- The occupancy-calendar formula is tested as a pure function for a few day-indices, matching the
  exact sine-based expression.
- All 28 calendar cells render with the correct day numbers (`1`–`28`).
- Recent Bookings renders every `hotelBookings` entry (there are exactly 4, so `slice(0, 4)` keeps
  all of them — the test asserts this outcome, not that slicing is unreachable).
- Room Inventory renders every `rooms` entry's type with the correct `available`/`total` numbers.
- Guest Rating Snapshot renders the hardcoded `4.7`, `182 reviews`, and all 5 percentage values
  (`72`, `18`, `6`, `2`, `2`).

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/hotel` in the browser shows the real dashboard (not the "coming soon" placeholder)
  matching the React source's layout and data; the other 5 `/hotel/*` routes still show
  placeholders (expected — they're out of scope here).

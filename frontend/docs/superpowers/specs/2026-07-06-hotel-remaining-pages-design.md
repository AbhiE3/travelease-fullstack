# Hotel Partner — Remaining Pages — Design

## Context

Second half of the Hotel Partner role, following
[2026-07-06-hotel-dashboard-design.md](2026-07-06-hotel-dashboard-design.md). Covers the
remaining 5 `/hotel/*` routes, currently rendering `RoutePlaceholder`: Properties, Rooms,
Bookings, Reviews, Reports. `hotel.routes.ts` already exists with these 5 children on
`RoutePlaceholder` (the dashboard child was already replaced in the prior sub-project). Once this
is done, the entire Hotel Partner role is fully built out.

Source: `trip-weaver-83-main/src/routes/hotel.{properties,rooms,bookings,reviews,reports}.tsx`.

## Decisions

- **Naming/structure**: `features/hotel/components/<name>/<name>.ts`, no "Page" suffix, matching
  the established convention (same domain, alongside the already-built `hotel-dashboard`).
- **`HotelReviews`'s review list stays component-local** (4 hardcoded reviews with name/rating/
  text/date), not promoted to `@app/core/mock-data` — hardcoded in the React route file too.
- **`HotelReports` is entirely hardcoded**: 4 stat cards (`Occupancy 78%`, `Revenue MTD ₹9.4L`,
  `ADR ₹4,820`, `Avg Rating 4.7`) and a "Revenue Trend" card with a raw inline SVG line+area
  chart using fixed polyline point coordinates — no formula, no data source, ported verbatim
  including the exact point coordinates.
- **`HotelProperties`'s "Live" badge is plain markup, not `StatusBadge`**: every hotel card shows
  the same hardcoded `bg-success/10 text-success border-success/20` "Live" badge regardless of
  any per-hotel field (there is no status field on `hotels` — React hardcodes this constant on
  every card). Since it never varies, it doesn't fit `StatusBadge`'s status-driven design, so it's
  rendered as plain markup instead of a misleading "dynamic" component call.
- **No new icons** — `Plus`, `MapPin`, `Star` are all already registered in `app.config.ts`.
  `StatusBadge` needs no further changes — `Confirmed`/`Pending` are already supported from the
  Activity/Transport sub-projects.
- **Non-functional actions preserved as non-functional**: "Add Property", "Add Room Type",
  "Manage" have no click handlers in React, so none do here.

## Scope

**In scope:**

- `HotelProperties` (`features/hotel/components/hotel-properties/hotel-properties.ts`, route
  `'properties'`): `PageHeader` with non-functional "Add Property" action; a responsive grid of
  every `hotels` entry — image, name, area (MapPin), star rating, room count, hardcoded "Live"
  badge, price/night, non-functional "Manage" button.
- `ManageRooms` (`features/hotel/components/manage-rooms/manage-rooms.ts`, route `'rooms'`):
  `PageHeader` with non-functional "Add Room Type" action; a table of every `rooms` entry — type,
  total, available, price, and an occupancy % + bar computed as `(total - available) / total *
  100` (same formula already used in `HotelDashboard`'s Room Inventory card, recomputed locally
  here rather than shared, matching how React recomputes it per-route too).
- `HotelBookings` (`features/hotel/components/hotel-bookings/hotel-bookings.ts`, route
  `'bookings'`): `PageHeader`; a 6-column table (Guest/Room/Check-in→out/Guests/Total/Status) over
  the full `hotelBookings` mock array (unlike Activity/Transport Bookings, this one uses the real
  global array directly — no local hardcoded list), status via `StatusBadge`.
- `HotelReviews` (`features/hotel/components/hotel-reviews/hotel-reviews.ts`, route `'reviews'`):
  `PageHeader`; a 2-column grid of 4 hardcoded reviews — avatar with the reviewer's first-initial
  fallback, name, date, a star row sized to that review's own `rating` (not a fixed 5), and the
  review text.
- `HotelReports` (`features/hotel/components/hotel-reports/hotel-reports.ts`, route `'reports'`):
  `PageHeader`; 4 hardcoded stat cards; a "Revenue Trend" card with the exact inline SVG polyline
  chart from React.
- `hotel.routes.ts` modified in place: its remaining 5 `RoutePlaceholder` children become
  `loadComponent` references to the 5 real components above; their `data: { title }` entries are
  dropped (matching the dashboard child from the prior sub-project).

**Explicitly out of scope:**

- Any real property/room creation, editing, or booking-status change — matching React, none of
  these are wired up.
- Admin dashboard — separate, final sub-project of this phase.

## Testing

- `HotelProperties`: renders every `hotels` entry's name, price, and rating.
- `ManageRooms`: renders every `rooms` entry's type, total, available, and price; the occupancy %
  for a couple of rooms matches the formula computed independently in the test.
- `HotelBookings`: renders every `hotelBookings` entry's guest and room; a `Confirmed` row and the
  `Pending` row get visibly different `StatusBadge` classes.
- `HotelReviews`: renders all 4 hardcoded reviewer names; each review renders exactly as many
  star icons as its own `rating` value (not always 5).
- `HotelReports`: renders all 4 hardcoded stat values; the SVG polyline `points` attributes match
  the exact coordinate strings from the React source.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/hotel/properties`, `/hotel/rooms`, `/hotel/bookings`, `/hotel/reviews`,
  `/hotel/reports` in the browser shows real content (not the "coming soon" placeholder) matching
  the React source's layout and data. This completes the entire Hotel Partner role.

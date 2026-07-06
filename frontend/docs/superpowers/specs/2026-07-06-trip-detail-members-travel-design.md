# Trip Detail — Members + Travel Tabs — Design

## Context

Fourth sub-project of "Trips" and second of the four-part Trip Detail breakdown, after
[2026-07-06-trip-detail-shell-overview-design.md](2026-07-06-trip-detail-shell-overview-design.md).
Adds real content to the `members` and `travel` tabs of the `TripDetail` shell, replacing two of
its "coming soon" placeholders.

**Verified via a real scratch build before this spec was written:** the `Dialog` component's
correct usage pattern is `<hlm-dialog><button hlmDialogTrigger>...</button><ng-template
hlmDialogPortal><hlm-dialog-content>...</hlm-dialog-content></ng-template></hlm-dialog>` — no
extra structural directive needed on `hlm-dialog-content` (an initial guess including
`*brnDialogContent` compiled with an unused-import warning; removing it compiled clean).

## Decisions

- **`TripMembersTab` takes no inputs.** `members` is global mock data (not trip-specific) in
  both the React source and our port — matches exactly.
- **`TripTravelTab` takes `trip` as an input** (needed for the `bus.seats >= trip.members`
  comparison driving the "Suitable for Group" badge).
- **All seat-map data, the warning message, and the search-form defaults are hardcoded**,
  matching React exactly (they are static arrays/strings in the source, not derived from any
  data) — ported verbatim rather than "improved" into computed logic.
- **Dialogs and their action buttons stay non-functional** ("Send Invite" has no click handler
  in React either) — consistent with every other sub-project's static-prototype scope.

## Scope

**In scope:**

- `TripMembersTab` (`src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.ts`):
  a card with "Trip Members" title and an "Invite Member" `Dialog` trigger (email input, Role
  `Select` [Traveler/Organizer, default Traveler], non-functional "Send Invite" button); a table
  over all `members` (avatar+name+email, role badge, `StatusBadge`, non-functional "···" action
  button).
- `TripTravelTab` (`src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.ts`),
  receiving `trip` as input:
  - Search Buses card: static Source/Destination/Date inputs (defaults "Bengaluru"/"Goa"/
    "2026-07-12"), non-functional Search button.
  - Bus list: one card per `buses` entry, "Suitable for Group" badge when
    `bus.seats >= trip.members`, departure→arrival with an arrow icon, seats-left + rating,
    price + non-functional "View Seats"/"Select" buttons.
  - Seat Allocation card: hardcoded 30-seat grid (booked `[2,5,7,11,14,18,22,25]`, selected
    `[12,13,17,19]`, recommended `[12,13,17,19,8,9]`), color legend, "Selected Seats" badges
    (`13,14,18,20`), hardcoded split-group warning, total/price footer with non-functional
    "Proceed" button.
- `TripDetail` updated: hosts `TripMembersTab` for `members` and `TripTravelTab` for `travel`
  instead of the generic placeholder; the placeholder loop now covers the remaining 5 tabs.
- 2 new icons registered in `app.config.ts`: `UserPlus`, `ArrowRight`.

**Explicitly out of scope:**

- Accommodation, Expenses, Itinerary, Alerts, Reviews tabs — later sub-projects.
- Real invite/booking/seat-selection behavior or any backend integration.

## Testing

- `TripMembersTab`: renders every entry from `members`; the "Invite Member" dialog trigger is
  present.
- `TripTravelTab`: renders every entry from `buses`; the "Suitable for Group" badge appears only
  when `bus.seats >= trip.members` (tested with both a high-member and low-member trip); the
  seat grid renders exactly 30 seats.
- `TripDetail`: the "coming soon" placeholder now covers only 5 tabs, not 7.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- On `/trips/goa-2026`, clicking the "Members" tab shows the real member table and invite
  dialog; clicking "Travel" shows the real bus search/list and seat allocation grid.

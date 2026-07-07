# Admin Bus/Hotel Per-Item Approve — Design

## Context

New feature (not part of the original React source) requested for the Admin `AdminBuses` and
`AdminHotels` pages: each bus and hotel should carry its own local approval status, starting as
"Pending", with an "Approve" action that flips it to "Accepted". This is purely local UI state on
these two pages — it does not connect to `AdminApprovals` (the existing partner-registration
approval flow) or to the Hotel/Transport Partner dashboards, since the app has no shared/mutable
state mechanism across roles (every dashboard independently reads the same static mock arrays).

## Decisions

- **New local `'Pending' | 'Accepted'` status per item**, defaulting to `'Pending'` for every bus
  and hotel when the page loads. This state lives only in the component (an Angular signal holding
  a `Set` of approved ids, or an array of per-row signals) — not persisted, not shared with any
  other page, and reset on reload, consistent with this app's mock-data-only architecture.
- **Reuses `StatusBadge`'s existing `Accepted` styling** for the "Accepted" state — no new
  `StatusBadge` entries needed. "Pending" state shows the "Approve" button instead of a badge (not
  a "Pending" badge), since the button itself communicates the pending state.
- **`AdminBuses`**: a new 8th "Approval" column in the Fleet table. The existing 7 columns (Bus,
  Operator, Route, Seats, Price, Rating, Status — the unrelated On Time/Delayed status) are
  untouched.
- **`AdminHotels`**: a new element in each hotel card, placed above the existing Edit/Manage
  button row.
- **Clicking "Approve" is a real, functional action** (unlike most buttons ported from React,
  which are non-functional prototypes) — this is new functionality the user explicitly asked for,
  not a source-fidelity port.

## Scope

**In scope:**

- `AdminBuses`: track per-bus approval state (keyed by bus `id`); render an "Approve" button for
  pending buses and an "Accepted" `StatusBadge` for approved ones, in a new table column.
- `AdminHotels`: same per-hotel approval state and button/badge, added to each hotel card.

**Explicitly out of scope:**

- Any connection to `AdminApprovals`, the Hotel/Transport Partner dashboards, or any backend —
  approval state here is local-only and resets on page reload.
- Any change to the existing bus On Time/Delayed status or hotel Edit/Manage buttons.

## Testing

- `AdminBuses`: every bus starts in the Pending state (renders "Approve", not "Accepted"); clicking
  a bus's "Approve" button changes only that bus's row to "Accepted" (others remain Pending).
- `AdminHotels`: same two behaviors — every hotel starts Pending; approving one hotel doesn't
  affect the others.

## Verification

- `ng build` and `ng test` succeed with no regressions.
- Visiting `/admin/buses` and `/admin/hotels`: every item shows "Approve"; clicking it on one item
  changes that item to an "Accepted" badge while the rest remain unaffected.

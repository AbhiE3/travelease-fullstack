# Traveler Misc Pages — Design

## Context

With the Trips module (List, New, Detail) fully built, this sub-project covers the remaining
traveler-role routes that currently render `RoutePlaceholder`: `/expenses`, `/profile`,
`/notifications`, `/invitations`. Each is small (~40 lines in the React source), self-contained,
and reuses mock data / shared components that already exist in the Angular app, so all 4 are
handled as a single sub-project rather than split further.

Source: `trip-weaver-83-main/src/routes/{expenses,profile,notifications,invitations}.tsx`.

## Decisions

- **Naming/structure**: one feature domain per page, following the established convention —
  `features/<domain>/components/<name>/<name>.ts`, no "Page" suffix. Each domain gets its own
  `<domain>.routes.ts` (matching the Dashboard precedent), even though each currently has only
  one route, for structural consistency.
- **Content fidelity**: ported 1:1 from React, including quirks — `InvitationList` hardcodes the
  destination text as "Goa" on every card regardless of the invitation's actual trip (a real bug
  in the source), and `ExpenseList`'s settlement summary numbers ("You owe ₹2,300" / "You'll
  receive ₹5,400") are hardcoded, duplicating the same hardcoded numbers already used in the Trip
  Detail Expenses tab. This migration's standing rule is behavior-for-behavior parity with the
  static mock-data prototype, not a corrected reimplementation.
- **No new mock data**: `expenses`, `notifications`, `invitations` all already exist in
  `@app/core/mock-data`. `Profile` has no backing mock data in React either — it's fully
  hardcoded there and stays that way here.
- **One new icon**: `BellRing` (used by `NotificationList`'s icon-map fallback). Everything else
  needed (`Mail`, `Wallet`, `AlertTriangle`, `CheckCircle2`, `Users`, `Calendar`, `MapPin`,
  `Plus`) is already registered in `app.config.ts`.
- **Non-functional actions preserved as non-functional**: "Add Expense", "Change photo", "Save
  changes", "Change password", "Accept"/"Decline" — none have click handlers in React, so none
  get them here.

## Scope

**In scope:**

- `ExpenseList` (`features/expenses/components/expense-list/expense-list.ts`, route
  `/expenses`): `PageHeader` with "Add Expense" action button; a 2-column layout — "All Expenses"
  card listing every `expenses` entry (wallet icon, name, "Paid by X · split N ways",
  amount, `StatusBadge`), and a sidebar with two hardcoded summary cards ("You owe" /
  "You'll receive").
- `Profile` (`features/profile/components/profile/profile.ts`, route `/profile`): `PageHeader`;
  a 3-column layout — an avatar card (initials "SR", name "Sarathy R", email, non-functional
  "Change photo" button) and an "Account details" card with 4 pre-filled inputs (Name, Email,
  Phone, Default city) and "Save changes"/"Change password" buttons.
- `NotificationList` (`features/notifications/components/notification-list/notification-list.ts`,
  route `/notifications`): `PageHeader`; one card whose content is a divided list of every
  `notifications` entry, each row showing an icon (from a `type` → icon map: `invitation`→Mail,
  `expense`→Wallet, `budget`→AlertTriangle, `delay`→BellRing, `booking`→CheckCircle2, unmapped
  types → BellRing fallback), title, description, and relative time.
- `InvitationList` (`features/invitations/components/invitation-list/invitation-list.ts`, route
  `/invitations`): `PageHeader`; a 2-column grid of cards, one per `invitations` entry — trip
  name, "Invited by X", a row of Calendar/Users/MapPin-iconed meta (dates, member count, and the
  hardcoded "Goa" destination), and non-functional Accept/Decline buttons.
- 4 new `<domain>.routes.ts` files, each `loadChildren`'d from `traveler.routes.ts` in place of
  the corresponding `RoutePlaceholder` entry.
- 1 new icon registered in `app.config.ts`: `BellRing`.

**Explicitly out of scope:**

- Any real form submission, expense creation, invitation accept/decline, or password-change
  behavior — matching React, none of these are wired up.
- Any other feature module (Activity/Hotel/Transport partner dashboards, Admin) — separate
  sub-projects.

## Testing

- `ExpenseList`: renders every `expenses` entry's name and amount; renders both hardcoded
  settlement summary figures.
- `Profile`: renders the hardcoded name, email, and all 4 input `defaultValue`s.
- `NotificationList`: renders every `notifications` entry's title; an entry with an unmapped
  `type` (or a component-level check of the icon-map function) falls back to the BellRing icon.
- `InvitationList`: renders every `invitations` entry's trip name and organizer; every card shows
  "Goa" regardless of which invitation it is.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/expenses`, `/profile`, `/notifications`, `/invitations` in the browser shows real
  content (not the "coming soon" placeholder) matching the React source's layout and data.

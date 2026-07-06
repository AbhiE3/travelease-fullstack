# Admin — Approvals + Users — Design

## Context

Second of 4 sub-projects covering the Admin role, following
[2026-07-06-admin-dashboard-reports-design.md](2026-07-06-admin-dashboard-reports-design.md).
Covers the `'approvals'` and `'users'` children of `admin.routes.ts` (currently
`RoutePlaceholder`). The other 6 children stay untouched until their sub-projects.

Source: `trip-weaver-83-main/src/routes/admin.approvals.tsx`, `admin.users.tsx`.

## Decisions

- **Naming/structure**: `features/admin/components/<name>/<name>.ts`, no "Page" suffix.
- **3 new icons**: `Check`, `X`, `FileText` (all confirmed present in `@ng-icons/lucide`).
  Everything else needed (`Hotel`, `Bus`, `Activity`, `Search`) is already registered.
- **`StatusBadge` needs no changes** — `members`' `status` values (`Accepted`/`Pending`/
  `Rejected`) already match existing map entries exactly.
- **`AdminUsers`'s member-doubling quirk is preserved verbatim**: React renders
  `[...members, ...members]` (re-keyed with an index suffix so React doesn't warn about duplicate
  keys) — i.e., every member appears twice in the table. This is intentional prototype padding to
  make the table look populated, not a bug to fix.
- **Type→icon mapping for Approvals** (`Hotel`→`Hotel`, `Transport`→`Bus`, `Activity`→`Activity`)
  follows the same exported-pure-function pattern used for `NotificationList`'s
  `iconForNotificationType`, since `pendingApprovals`' 3 `type` values map 1:1 with no fallback
  needed (unlike notifications, which had an unmapped-type fallback).
- **Non-functional actions preserved as non-functional**: the search input, "Export", "N docs",
  "Reject", "Approve", and the "···" row-action button all have no handlers in React.

## Scope

**In scope:**

- `AdminApprovals` (`features/admin/components/admin-approvals/admin-approvals.ts`, route
  `'approvals'`): `PageHeader`; 4 stat cards computed from `pendingApprovals` (`Pending` = total
  length, `Hotels`/`Transport`/`Activity` = counts filtered by `type`); an "Awaiting Review" list,
  one row per `pendingApprovals` entry — icon by type, name + type `Badge`, city + registered
  date, non-functional "`{documents}` docs" button, non-functional destructive-styled "Reject"
  button, non-functional success-styled "Approve" button.
- `AdminUsers` (`features/admin/components/admin-users/admin-users.ts`, route `'users'`):
  `PageHeader`; a non-functional search input (with `Search` icon) + non-functional "Export"
  button; a table over `[...members, ...members]` (re-keyed ids, all `members` rendered twice) —
  avatar-initial, name, email, role, `StatusBadge`, non-functional "···" action button.
- `admin.routes.ts` modified in place: only its `'approvals'` and `'users'` children swap
  `RoutePlaceholder` for the real components above, `data: { title }` dropped from those two. The
  other 6 children (`route-analytics`/`partners`/`funnel`/`trips`/`buses`/`hotels`) are untouched.

**Explicitly out of scope:**

- The other 6 `/admin/*` pages — separate, later sub-projects.
- Any real approval/rejection, search filtering, export, or user-action behavior — matching
  React, none of these are wired up.

## Testing

- `AdminApprovals`: all 4 stat counts compute correctly from `pendingApprovals`; renders every
  approval's name and city; the type→icon mapping resolves correctly for `Hotel`, `Transport`,
  and `Activity` (tested as a pure function).
- `AdminUsers`: renders exactly `members.length * 2` rows; all row ids are unique (re-keyed, not
  literally duplicated); renders each member's name, email, and role (appearing twice each); an
  `Accepted` row and a `Pending`/`Rejected` row get their already-distinct `StatusBadge` classes.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/admin/approvals` and `/admin/users` in the browser shows real content (not the
  "coming soon" placeholder) matching the React source's layout and data.

# Login — Temporary Role-Based Auth — Design

## Context

The current `Login` component (built in the [2026-07-07-login-register-design.md](2026-07-07-login-register-design.md)
sub-project) ports React's behavior verbatim: any submit navigates to `/dashboard`, and 4
separate "Or enter as" buttons let you jump straight to any role's dashboard with no credential
check at all. The user now wants a real (temporary, hardcoded) credential gate: typing one of 5
known username/password pairs and submitting should route to that role's dashboard; the
quick-switch buttons go away entirely. This is an explicit temporary fix — not a real auth
system — so it's implemented as a small hardcoded credential list, not a service, database, or
guard.

## Decisions

- **5 hardcoded role credentials**, defined as a local constant in `Login`'s own file:

  | Username   | Password      | Route         |
  |------------|---------------|---------------|
  | `user`     | `user123`     | `/dashboard`  |
  | `admin`    | `admin123`    | `/admin`      |
  | `hotel`    | `hotel123`    | `/hotel`      |
  | `bus`      | `bus123`      | `/transport`  |
  | `activity` | `activity123` | `/activity`   |

- **The "Email" field is relabeled "Username"** (`type="text"`, placeholder `e.g. admin`, no
  prefilled value — previously prefilled with `sarathy@example.com`). **"Password" stays a
  password input**, no prefilled value (previously prefilled with `password`).
- **The entire "Or enter as" divider + 4 role buttons are removed.**
- **On submit**: look up the username/password pair against the 5 credentials above. On a match,
  navigate to that role's route. On no match, show an inline "Invalid username or password"
  message and stay on the page — no navigation.
- **The error clears on the next submit attempt** (whether that attempt succeeds or fails again),
  so a stale error message never lingers after the user corrects their input and resubmits.
- **Credential matching is an exported pure function** (`matchRole(username, password): string |
  null`), separate from the component, so the matching logic is directly unit-testable without
  going through form/DOM interaction for every case.
- **`Register` and the "Forgot password?" link are untouched** — not mentioned in this request,
  out of scope.

## Scope

**In scope:**

- `Login` (`features/auth/components/login/login.ts`): remove the `enterAs()` method and its 4
  template buttons plus the "Or enter as" divider; relabel the Email field to Username (text
  input, no prefill) and clear the Password field's prefill; add an error-message signal; change
  `onSubmit` to call `matchRole` and either navigate or set the error.
- New exported `matchRole(username: string, password: string): string | null` pure function and
  the 5-entry credential constant, both in `login.ts`.

**Explicitly out of scope:**

- Any real authentication (session storage, guards, backend calls) — this remains a client-side,
  hardcoded, temporary mechanism as explicitly requested.
- `Register` page changes.
- The "Forgot password?" link.

## Testing

- `matchRole` returns the correct route for each of the 5 valid username/password pairs, and
  `null` for an invalid pair (wrong password for a valid username, and a wholly unknown username).
- Submitting the form with each of the 5 valid credential pairs calls `router.navigate` with the
  correct path.
- Submitting with an invalid pair shows the inline error message and does not call
  `router.navigate`.
- Submitting an invalid pair, then correcting and resubmitting with a valid pair, clears the error
  and navigates.

## Verification

- `ng build` and `ng test` succeed with no regressions.
- Visiting `/login` in the browser: the "Or enter as" section is gone; entering `admin`/`admin123`
  and submitting navigates to `/admin`; entering a wrong password shows the inline error and stays
  on `/login`.

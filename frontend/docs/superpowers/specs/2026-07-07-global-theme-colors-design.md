# Global Theme Colors — Design

## Context

`frontend/src/styles.css` still uses spartan-ng's generic scaffolded grayscale theme, never
replaced with TravelEase's actual brand palette from the React source
(`trip-weaver-83-main/src/styles.css`). Worse, this is a live bug, not just a cosmetic gap: four
CSS custom properties — `--success`, `--warning`, `--gradient-hero`, `--gradient-ocean`,
`--shadow-card`, `--shadow-elevated` — are already referenced by components built earlier this
session (`AuthLayout`'s hero gradient, `Landing`, `DashboardPage`, `TripList`, and every
`bg-success`/`text-warning`/`border-success/20`-style class used throughout `StatusBadge` and
the many occupancy/rating/alert components across every role dashboard) but are never defined
anywhere in the Angular app's CSS. Tailwind v4 only generates utility classes for colors
registered in a `@theme` block; since neither spartan-ng's own preset
(`node_modules/@spartan-ng/brain/hlm-tailwind-preset.css`) nor the app's own `styles.css`
registers `--color-success`/`--color-warning`, every `bg-success`/`text-warning`/etc. class in
the app is currently silently non-functional.

## Decisions

- **Light theme (`:root`) only** — `.dark` is left untouched. The app has no dark-mode toggle
  wired up anywhere, so leaving `.dark` on its current generic grayscale values (rather than also
  copying React's `.dark`, which itself is just shadcn's generic dark theme, not a customized
  brand dark theme) causes no visible inconsistency today.
- **Copy React's `:root` block verbatim** into `frontend/src/styles.css`, including `--radius:
  0.75rem` (currently `0.625rem`) for consistent corner-rounding across cards/buttons/badges —
  not strictly a color, but part of the same token block and part of matching the reference's
  actual look.
- **`--font-sans` and `--font-mono` are left untouched** — React's `styles.css` doesn't define
  `--font-sans` in `:root` at all (no conflict), so Angular's existing system-font stack stays.
- **Add a new `@theme inline` block** to the app's own `styles.css` registering
  `--color-success: var(--success)`, `--color-success-foreground: var(--success-foreground)`,
  `--color-warning: var(--warning)`, `--color-warning-foreground: var(--warning-foreground)` —
  since spartan-ng's own preset doesn't register these, the app's own stylesheet must, which is
  what actually turns the `bg-success`/`text-warning`/etc. classes already used throughout the
  app from silently-broken into functional.
- **Also register `--color-chart-1` through `--color-chart-5`** in the same new `@theme inline`
  block for consistency with the source's full token set, even though no chart component
  currently consumes them — low-cost, keeps the token set complete for any future chart work.
- **No component code changes** — every component that references these tokens already assumes
  they exist; this is purely a CSS-file fix.

## Scope

**In scope:**

- `frontend/src/styles.css`: replace the existing `:root` block's color/radius values with
  React's exact values (`--radius`, `--background`, `--foreground`, `--card`,
  `--card-foreground`, `--popover`, `--popover-foreground`, `--primary`, `--primary-foreground`,
  `--secondary`, `--secondary-foreground`, `--muted`, `--muted-foreground`, `--accent`,
  `--accent-foreground`, `--destructive`, `--destructive-foreground`, `--success`,
  `--success-foreground`, `--warning`, `--warning-foreground`, `--border`, `--input`, `--ring`,
  `--chart-1` through `--chart-5`, `--sidebar`, `--sidebar-foreground`, `--sidebar-primary`,
  `--sidebar-primary-foreground`, `--sidebar-accent`, `--sidebar-accent-foreground`,
  `--sidebar-border`, `--sidebar-ring`, `--gradient-hero`, `--gradient-ocean`, `--shadow-card`,
  `--shadow-elevated`).
- Add a new `@theme inline` block registering `--color-success`, `--color-success-foreground`,
  `--color-warning`, `--color-warning-foreground`, `--color-chart-1` through `--color-chart-5`.

**Explicitly out of scope:**

- `.dark` block — untouched.
- `--font-sans`/`--font-mono` — untouched.
- Any component template/logic changes.

## Testing

- This is a pure CSS-token change with no unit-testable logic — verification is visual/build-based
  (see Verification below), not unit tests.

## Verification

- `ng build` succeeds with no errors.
- Visiting any already-built page (e.g. `/`, `/login`, `/dashboard`, `/trips/*`, any role
  dashboard) in the browser shows the actual TravelEase blue/teal primary color, orange accent,
  and — critically — visibly colored (not transparent/missing) success/warning badges and
  progress bars (e.g. `StatusBadge`'s "Confirmed"/"Accepted" green tone, "Pending" amber tone; the
  occupancy bars in `ActivityDashboard`/`TransportDashboard`; the star ratings' warning-colored
  fill).
- The `AuthLayout` hero panel (visible on `/login`, `/register`) shows its gradient background,
  not a flat/missing background.

# UI Component Library — Design

## Context

This is sub-project 2 of the trip-weaver-83-main → Angular migration (see
[2026-07-04-angular-foundation-design.md](2026-07-04-angular-foundation-design.md) for the full
decomposition and prior context). Foundation (sub-project 1) is complete: routing skeleton,
design tokens, mock data, and tooling are all in place and verified.

The React app's `src/components/ui/` folder has 46 shadcn/ui component files. Comparing that
list against spartan-ng's component registry and what's already been generated into
`libs/ui/` (via `ng g @spartan-ng/cli:init` and manual `ng g @spartan-ng/cli:ui` runs done
outside this process, before Foundation started):

**Already generated (19 direct matches):** badge, button, calendar, card, checkbox,
collapsible, dialog, dropdown-menu, input, label, popover, select, separator, sheet, sidebar,
skeleton, switch, textarea, tooltip — plus `field` (spartan's form-field wrapper, the
structural equivalent of React's `form.tsx`, which wraps `react-hook-form`).

**Available via spartan-ng but not yet generated (25):** accordion, alert, alert-dialog,
aspect-ratio, avatar, breadcrumb, carousel, command, context-menu, drawer, hover-card,
input-otp, menubar, navigation-menu, pagination, progress, radio-group, resizable,
scroll-area, slider, sonner, table, tabs, toggle, toggle-group.

**One true gap — no spartan-ng equivalent exists:** `chart` (the React app uses `recharts`;
there is no Angular charting primitive in spartan-ng's registry).

## Decisions

- **Generate all 25 remaining components now**, in this sub-project, rather than lazily as
  each feature page needs one. This means every later feature sub-project can just consume
  components without a "generate this one first" detour.
- **Defer the charting library decision.** `chart` has no spartan-ng equivalent and needs its
  own research (e.g. `ng2-charts`/Chart.js vs. `ngx-charts`). Several later feature
  sub-projects use charts (admin reports/funnel/route-analytics, activity/hotel/transport
  reports) — the decision is better made there, against concrete chart types and data shapes,
  than speculatively here.
- **Build-level verification only.** These are CLI-vended files from spartan-ng's own
  generator (like the 19 already in `libs/ui/`) — not hand-written code we own, so they don't
  get new hand-written unit tests. `ng build` and `ng test` passing is the bar.

## Scope of this sub-project

**In scope:**

- Run `ng g @spartan-ng/cli:ui <name>` for each of the 25 components listed above. Each lands
  in `libs/ui/<name>/`, matching the existing pattern (`src/index.ts` barrel export,
  `src/lib/` implementation), with the CLI automatically wiring up `tsconfig.json`
  `@spartan-ng/helm/<name>` path mappings the same way it did for the existing 19.
- Generate in small batches (~5 components at a time) rather than all-at-once or strictly
  one-by-one, running `ng build` + `ng test` after each batch to catch issues early without
  paying for 25 separate full builds.
- Run `npm install` after generation for the two components that pull in new npm packages: the
  `carousel` generator adds `embla-carousel` + `embla-carousel-angular` (the same underlying
  embla library the React app already uses via `embla-carousel-react`, so behavior should be
  familiar), and the `scroll-area` generator adds `ngx-scrollbar` (spartan's chosen dependency
  — a different underlying library than Radix's scroll-area, but the same job).
- Final full verification: `ng build` and `ng test` both pass with all 25 new components plus
  the pre-existing 19 in place.

**Explicitly out of scope** (deferred to later sub-projects):

- Consuming, styling, or wiring any generated component into an actual page or layout.
- The `AppShell` (role-based sidebar + header) and `AuthLayout` — this sub-project does add
  `Avatar`, which was the last missing dependency blocking that work, so the *next*
  sub-project can proceed, but building them is not part of this one.
- Any charting library decision or `chart` component work.
- Any real feature page content.

## Verification

This sub-project is "done" when:

- All 25 components exist under `libs/ui/<name>/`, following the same file structure as the
  existing 19.
- `npm install` has been run and `package.json` reflects the new peer dependencies
  (`embla-carousel`, `embla-carousel-angular`, `ngx-scrollbar`) alongside the ones already
  present from `ng g @spartan-ng/cli:init`.
- `ng build` succeeds (browser + server bundles, no compile errors).
- `ng test` passes with no regressions (same test count as the end of Foundation, since no
  new hand-written tests are added in this sub-project).

# Design: Migrate CSS-based charts to Apache ECharts

Date: 2026-07-07

## Context

The frontend (`frontend/`) currently has no charting library. Every "chart" across the admin and hotel dashboards is a plain CSS/Tailwind div bound to Angular data via `[style.width.%]` / `[style.height.%]` / `[style.background]`. This works for simple bars, but two cases are hand-rolled approximations of real chart types:

- `admin-funnel` builds a funnel shape out of stacked, shrinking-width cards.
- `hotel-dashboard` builds a calendar-heatmap out of a 28-cell CSS grid with colors computed via `color-mix(in oklab, var(--primary) X%, var(--card))`, using a fake sine-wave occupancy formula and a hardcoded "June" label (the day indices 1–28 aren't tied to real dates).

Apache ECharts has native `funnel` and `calendar`+`heatmap` chart types that map directly onto these two cases, plus a standard `bar` type for the remaining simple ranking bars — so it replaces hand-rolled CSS approximations with real, purpose-built chart primitives instead of just re-skinning the same divs.

## Goal

Replace every CSS-based chart across the app with ECharts, for visual and implementation consistency, using a thin custom Angular wrapper (no `ngx-echarts` dependency) that fits this codebase's existing pattern of small, hand-rolled `shared/ui` components.

## Non-goals

- No new data-fetching layer — all charts continue to read from the existing `src/app/core/mock-data.ts`.
- No dark-mode toggle is being added. The app has dark-mode CSS variables defined but nothing currently switches the `dark` class; charts read theme colors once at init, matching current behavior (no live re-theming requirement).
- No changes to non-chart UI (stat cards, tables, badges, etc.).

## Architecture

### Dependency

Add `echarts` to `package.json`. Import only from `echarts/core` plus the specific pieces used, so the bundle stays tree-shaken:

- Renderer: `SVGRenderer` (not `CanvasRenderer`). Chosen because:
  - The Angular 21 native unit-test builder runs on jsdom; SVG output is jsdom-friendly, while `CanvasRenderer` needs a native `canvas` binary in CI.
  - SVG is easy to theme and inspect, consistent with how the rest of the app is styled via plain CSS/DOM rather than pixel-drawn content.
- Chart types: `BarChart`, `FunnelChart`, `HeatmapChart`.
- Components: `TooltipComponent`, `GridComponent`, `CalendarComponent`, `VisualMapComponent` (color scale for the heatmap).

### Shared wrapper — `src/app/shared/ui/echart/`

- **`echart.ts`** — standalone `EChart` component.
  - Inputs: `options` (signal input of `EChartsCoreOption`), `height` (string, e.g. `'256px'`).
  - Initializes ECharts only in the browser, via `afterNextRender`. On the server, the component renders an empty placeholder `div` at the given `height` (avoids layout shift, and avoids any SSR/hydration mismatch since the wrapper never touches Angular-rendered DOM content — it injects ECharts' own SVG into an otherwise-empty container after hydration).
  - Uses `ResizeObserver` on the container to call `chart.resize()`.
  - An effect watches the `options` input and calls `chart.setOption(...)` on change.
  - Disposes the ECharts instance on destroy (`DestroyRef`).
- **`echart-theme.ts`** — `resolveThemeColor(varName: string): string`, reads `getComputedStyle(document.documentElement).getPropertyValue('--primary')` etc. at chart-init time. The app's OKLCH CSS variables (e.g. `oklch(0.50 0.11 215)`) are already used as raw inline style values elsewhere (the current calendar's `color-mix` background), so passing the raw variable value straight into ECharts' color config works the same way — no color-space conversion needed. Colors are read once at init (see Non-goals).
- **`ranking-bar-chart.ts`** — shared pure builder:
  ```ts
  buildRankingBarOption(items: {label: string; value: number}[], color: string, unit?: '%' | ''): EChartsCoreOption
  ```
  Produces a horizontal bar chart (categories on the y-axis). This single function covers every "list of labeled values as bars" case in the app (see mapping table below), replacing 6 separate hand-written CSS bar groups.

Each chart-driven feature component builds its own `EChartsCoreOption` in its `.ts` file as a plain, independently-testable function (following the existing `bookingBarHeight` / `calendarOccupancy` pattern already in this codebase), and the template swaps the CSS markup for:

```html
<app-echart [options]="chartOptions" height="256px" />
```

## Per-page mapping

| Page | Current CSS chart | ECharts replacement |
|---|---|---|
| admin-dashboard | 30 height-bars (bookings, last 30 days) | Single vertical `bar` chart, 30 categories (Day 1–30) |
| admin-dashboard | Popular Destinations width-bars | `buildRankingBarOption` (5 categories, `--primary`) |
| admin-funnel | Stacked shrinking-width funnel cards | Native ECharts `funnel` series |
| admin-funnel | Drop-off Reasons width-bars | `buildRankingBarOption` (`--accent`) |
| admin-route-analytics | Most Booked Routes width-bars | `buildRankingBarOption` (`--success`) |
| admin-route-analytics | Least Booked Routes width-bars | `buildRankingBarOption` (`--destructive`) |
| hotel-dashboard | 28-cell sine-wave calendar grid | Native ECharts `calendar` + `heatmap` series, `visualMap` for color intensity |
| hotel-dashboard | Room Inventory width-bars (4 separate cards) | Consolidated into **one** `buildRankingBarOption` chart (categories = room types) — replaces 4 separate mini-bars with a single exhibit, rather than 4 tiny chart instances |
| hotel-dashboard | Rating Distribution width-bars | `buildRankingBarOption` (5 categories, `--warning`) |

### Known fidelity trade-offs (accepted)

1. **Funnel drop-off annotations**: today, a red "X% drop-off" row with a down-arrow icon sits *between* funnel segments. ECharts' native funnel renders contiguous segments and has no concept of an inter-segment annotation row. The drop-off detail moves into the tooltip (hover a segment), with a concise value label kept on the segment itself.
2. **Calendar heatmap needs real dates**: ECharts' `calendar` component requires an actual date range (e.g. `2026-07-01`..`2026-07-31`), not an arbitrary 28-count array. `mock-data.ts` changes to generate real days for the current month (via `new Date()`) instead of the fixed "June"/28-day placeholder. The fake occupancy formula (`calendarOccupancy`) is unchanged — only the date axis becomes real.

## Testing strategy change

Existing specs assert two different things: (1) pure data-transform functions (e.g. `bookingBarHeight`), and (2) rendered text content via `fixture.nativeElement.textContent`. Once values move into ECharts' SVG output, scraping `textContent` for chart values becomes a test of chart-library internals rather than app code, so:

- **Keep** testing pure functions exactly as now, plus the new option-builder functions (`buildRankingBarOption`, and the funnel/calendar option builders) — asserting the resulting series/data arrays are correct. This is more precise than the current textContent scraping for the same values.
- **Drop** `expect(text).toContain(...)` assertions for values that move into chart SVG (e.g. destination percentage labels).
- **Keep** textContent assertions for anything that stays as real rendered HTML (stat cards, card titles, room prices, etc.).
- **Add** one smoke test for the `EChart` wrapper: given an `options` input, it initializes an ECharts instance in the container without throwing, and disposes cleanly on destroy.

## Files

**New:**
- `src/app/shared/ui/echart/echart.ts`, `.html`, `.spec.ts`
- `src/app/shared/ui/echart/echart-theme.ts`
- `src/app/shared/ui/echart/ranking-bar-chart.ts`

**Modified:**
- `src/app/features/admin/components/admin-dashboard/admin-dashboard.ts`, `.html`, `.spec.ts`
- `src/app/features/admin/components/admin-funnel/admin-funnel.ts`, `.html`, `.spec.ts`
- `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.ts`, `.html`, `.spec.ts`
- `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.ts`, `.html`, `.spec.ts`
- `src/app/core/mock-data.ts` (calendar dates), `src/app/core/mock-data.spec.ts` if it covers calendar data
- `package.json` (add `echarts` dependency)

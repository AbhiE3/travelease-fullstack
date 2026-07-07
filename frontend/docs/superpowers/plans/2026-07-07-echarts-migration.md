# ECharts Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every CSS-based "chart" in the Angular frontend (bookings bar chart, ranking bars, booking funnel, hotel occupancy calendar) with real Apache ECharts charts, via a thin custom Angular wrapper component (no `ngx-echarts`).

**Architecture:** A standalone `EChart` wrapper component in `src/app/shared/ui/echart/` initializes ECharts (SVG renderer, tree-shaken imports) only in the browser via `afterNextRender`, so SSR renders an inert placeholder `div` and the real chart draws in after hydration. Each feature component builds a plain, independently-testable `EChartsCoreOption` object from its existing mock data and passes it to `<app-echart [options]="...">`. A shared `buildRankingBarOption` builder covers the 6 near-identical "list of labeled values as horizontal bars" cases; the bookings bar chart, funnel chart, and calendar heatmap get their own small builder functions colocated in the feature file that uses them (following this codebase's existing pattern of exported pure functions like `bookingBarHeight`).

**Tech Stack:** Angular 21 (standalone components, signal `input()`/`computed()`/`viewChild()`, `afterNextRender`), Apache ECharts 6 (`echarts/core` + `SVGRenderer` + `BarChart`/`FunnelChart`/`HeatmapChart` + `TooltipComponent`/`GridComponent`/`CalendarComponent`/`VisualMapComponent`), Tailwind v4, Vitest via `@angular/build:unit-test` (jsdom environment).

## Global Constraints

- Standalone components only, using signal-based APIs (`input()`, `computed()`, `viewChild()`) — matches existing `PageHeader`/`StatusBadge` style. No NgModules.
- Use existing Tailwind/Spartan card conventions (`hlmCard`, `hlmCardHeader`, `hlmCardTitle`, `hlmCardContent`) for all layout — no new card styling.
- ECharts renderer is `SVGRenderer`, never `CanvasRenderer` — required because the test environment is jsdom, which does not implement `HTMLCanvasElement.getContext()` (validated directly: `SVGRenderer` renders real SVG content under jsdom; `CanvasRenderer` would silently no-op).
- Chart colors are static literal OKLCH strings copied from `src/styles.css`'s light-theme values (`CHART_COLORS` in `echart-theme.ts`) — not resolved dynamically via `getComputedStyle`. This app has no dark-mode toggle wired up yet (confirmed non-goal in the design spec), so there is nothing to react to; static literals avoid any SSR/`getComputedStyle`-in-a-fake-DOM ambiguity entirely.
- The `EChart` wrapper reads its `options` input once, inside `afterNextRender`, and does not reactively re-`setOption` on later input changes — every call site in this app builds its options once from static mock data, so live-updating is unimplemented dead code today (YAGNI). This is a deviation from the original design doc's mention of an update-tracking `effect()`; flagged here since it was discovered while writing exact code.
- The wrapper only wires up `ResizeObserver` when `typeof ResizeObserver !== 'undefined'`, so no test-environment polyfill is needed anywhere (jsdom does not implement `ResizeObserver`).
- Per the approved design's Section 2 trade-off: the funnel chart's drop-off percentage moves into the tooltip (hover), not an always-visible row.
- Per the approved design's Section 2 trade-off: the hotel occupancy calendar now plots real dates for the current month (via `new Date()`) instead of a hardcoded 28-cell/"June" placeholder. This logic lives in `hotel-dashboard.ts` itself (the design doc's file list said `mock-data.ts`, but the existing calendar-generation code was never in `mock-data.ts` — corrected here).
- This project's `tsconfig.json` has `strict: true` and `noPropertyAccessFromIndexSignature: true`. `EChartsCoreOption`'s `series`/`xAxis`/`yAxis`/`tooltip`/`calendar`/`visualMap` fields are only reachable via that type's index signature (not named properties), so any code that reads them back off an already-typed `EChartsCoreOption` value (test assertions, tooltip formatters referencing sibling fields) must use bracket notation, e.g. `option['series']` not `option['series']`. Building a fresh option object literal to *return* as `EChartsCoreOption` is unaffected — the rule only bites on reads. Formatter callback parameters (e.g. `valueFormatter: (v) => ...`) also need an explicit type annotation (e.g. `(v: number) =>`) since contextual typing doesn't flow through the index signature. Discovered while running Task 3's tests; every task after this one already applies the fix.

---

## Task 1: Shared chart theme colors

**Files:**
- Create: `frontend/src/app/shared/ui/echart/echart-theme.ts`
- Test: `frontend/src/app/shared/ui/echart/echart-theme.spec.ts`

**Interfaces:**
- Produces: `CHART_COLORS: { primary: string; success: string; destructive: string; warning: string; accent: string; muted: string; mutedForeground: string }` — consumed by every task after this one.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/shared/ui/echart/echart-theme.spec.ts`:

```ts
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';

describe('CHART_COLORS', () => {
  it('matches the light-theme CSS variable values defined in styles.css', () => {
    expect(CHART_COLORS.primary).toBe('oklch(0.50 0.11 215)');
    expect(CHART_COLORS.success).toBe('oklch(0.65 0.15 155)');
    expect(CHART_COLORS.destructive).toBe('oklch(0.60 0.22 25)');
    expect(CHART_COLORS.warning).toBe('oklch(0.78 0.15 75)');
    expect(CHART_COLORS.accent).toBe('oklch(0.72 0.14 40)');
    expect(CHART_COLORS.muted).toBe('oklch(0.96 0.01 220)');
    expect(CHART_COLORS.mutedForeground).toBe('oklch(0.50 0.03 230)');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx ng test --watch=false --include='src/app/shared/ui/echart/echart-theme.spec.ts'`
Expected: FAIL — cannot find module `@app/shared/ui/echart/echart-theme`.

- [ ] **Step 3: Write the implementation**

Create `frontend/src/app/shared/ui/echart/echart-theme.ts`:

```ts
export const CHART_COLORS = {
  primary: 'oklch(0.50 0.11 215)',
  success: 'oklch(0.65 0.15 155)',
  destructive: 'oklch(0.60 0.22 25)',
  warning: 'oklch(0.78 0.15 75)',
  accent: 'oklch(0.72 0.14 40)',
  muted: 'oklch(0.96 0.01 220)',
  mutedForeground: 'oklch(0.50 0.03 230)',
} as const;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx ng test --watch=false --include='src/app/shared/ui/echart/echart-theme.spec.ts'`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
cd frontend
git add src/app/shared/ui/echart/echart-theme.ts src/app/shared/ui/echart/echart-theme.spec.ts
git commit -m "feat(charts): add shared ECharts theme color constants"
```

---

## Task 2: EChart wrapper component

**Files:**
- Modify: `frontend/package.json`, `frontend/package-lock.json` (add `echarts` dependency)
- Create: `frontend/src/app/shared/ui/echart/echart.ts`
- Create: `frontend/src/app/shared/ui/echart/echart.html`
- Test: `frontend/src/app/shared/ui/echart/echart.spec.ts`

**Interfaces:**
- Consumes: nothing from prior tasks.
- Produces: `EChart` standalone component, selector `app-echart`, inputs `options: input.required<EChartsCoreOption>()` and `height: input<string>()` (default `'256px'`). Every later task's template uses `<app-echart [options]="..." height="...px" />`.

- [ ] **Step 1: Install the dependency**

Run: `cd frontend && npm install echarts`
Expected: `package.json` gains an `"echarts": "^6.x.x"` entry under `dependencies`; `package-lock.json` updates.

- [ ] **Step 2: Write the failing test**

Create `frontend/src/app/shared/ui/echart/echart.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { getInstanceByDom } from 'echarts/core';
import { EChart } from '@app/shared/ui/echart/echart';

const BAR_OPTIONS = {
  xAxis: { type: 'category', data: ['a', 'b', 'c'] },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: [1, 2, 3] }],
} as const;

describe('EChart', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [EChart] }).compileComponents();
  });

  it('initializes an ECharts instance on the container after render', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', BAR_OPTIONS);
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    expect(getInstanceByDom(container)).toBeTruthy();
  });

  it('disposes the ECharts instance when the component is destroyed', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', BAR_OPTIONS);
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    fixture.destroy();
    expect(getInstanceByDom(container)).toBeFalsy();
  });

  it('applies the height input as an inline style on the container', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', { series: [] });
    fixture.componentRef.setInput('height', '320px');
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    expect(container.style.height).toBe('320px');
  });

  it('defaults height to 256px when not provided', async () => {
    const fixture = TestBed.createComponent(EChart);
    fixture.componentRef.setInput('options', { series: [] });
    fixture.detectChanges();
    await fixture.whenStable();

    const container = fixture.nativeElement.querySelector('div') as HTMLDivElement;
    expect(container.style.height).toBe('256px');
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd frontend && npx ng test --watch=false --include='src/app/shared/ui/echart/echart.spec.ts'`
Expected: FAIL — cannot find module `@app/shared/ui/echart/echart`.

- [ ] **Step 4: Write the implementation**

Create `frontend/src/app/shared/ui/echart/echart.html`:

```html
<div #container [style.height]="height()" class="w-full"></div>
```

Create `frontend/src/app/shared/ui/echart/echart.ts`:

```ts
import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  inject,
  input,
  viewChild,
} from '@angular/core';
import { dispose, init, use } from 'echarts/core';
import type { ECharts, EChartsCoreOption } from 'echarts/core';
import { SVGRenderer } from 'echarts/renderers';
import { BarChart, FunnelChart, HeatmapChart } from 'echarts/charts';
import {
  CalendarComponent,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components';

use([
  SVGRenderer,
  BarChart,
  FunnelChart,
  HeatmapChart,
  CalendarComponent,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
]);

@Component({
  selector: 'app-echart',
  templateUrl: './echart.html',
})
export class EChart {
  public readonly options = input.required<EChartsCoreOption>();
  public readonly height = input('256px');

  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('container');
  private chart: ECharts | null = null;

  constructor() {
    const destroyRef = inject(DestroyRef);

    afterNextRender(() => {
      const el = this.container().nativeElement;
      this.chart = init(el, undefined, { renderer: 'svg' });

      if (typeof ResizeObserver !== 'undefined') {
        const resizeObserver = new ResizeObserver(() => this.chart?.resize());
        resizeObserver.observe(el);
        destroyRef.onDestroy(() => resizeObserver.disconnect());
      }

      destroyRef.onDestroy(() => {
        if (this.chart) {
          dispose(this.chart);
          this.chart = null;
        }
      });

      this.chart.setOption(this.options());
    });
  }
}
```

Note: cleanup (`ResizeObserver`/dispose) is registered on `destroyRef` *before* calling `setOption` — if `setOption` were to throw (e.g. malformed options), cleanup must still be wired up so the chart instance isn't leaked. Also, under jsdom, `getInstanceByDom` returning a truthy instance and the wrapper's own `dispose` call are what the smoke test checks — you will also see benign console warnings like `Not implemented: HTMLCanvasElement's getContext()`; this is expected (ECharts probes for canvas text-measurement even under the SVG renderer) and does not affect correctness. This was verified directly in a scratch script before writing this plan.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd frontend && npx ng test --watch=false --include='src/app/shared/ui/echart/echart.spec.ts'`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
cd frontend
git add package.json package-lock.json src/app/shared/ui/echart/echart.ts src/app/shared/ui/echart/echart.html src/app/shared/ui/echart/echart.spec.ts
git commit -m "feat(charts): add echarts dependency and EChart wrapper component"
```

---

## Task 3: Shared ranking-bar-chart builder

**Files:**
- Create: `frontend/src/app/shared/ui/echart/ranking-bar-chart.ts`
- Test: `frontend/src/app/shared/ui/echart/ranking-bar-chart.spec.ts`

**Interfaces:**
- Consumes: nothing (pure function, no dependency on Task 1/2 code, only the `echarts/core` type).
- Produces: `RankingBarItem { label: string; value: number }` and `buildRankingBarOption(items: RankingBarItem[], color: string, unit?: '%' | ''): EChartsCoreOption`. Consumed by Tasks 4, 5, 6, 7.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/shared/ui/echart/ranking-bar-chart.spec.ts`:

```ts
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';

describe('buildRankingBarOption', () => {
  const items = [
    { label: 'Goa', value: 92 },
    { label: 'Manali', value: 74 },
  ];

  it('puts the first item at the top of the category axis (reversed for ECharts bottom-up rendering)', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const yAxis = option['yAxis'] as { data: string[] };
    expect(yAxis.data).toEqual(['Manali', 'Goa']);
  });

  it('reverses the series data to match the reversed category order', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.data).toEqual([74, 92]);
  });

  it('applies the given color to the bar itemStyle', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.itemStyle.color).toBe('oklch(0.5 0.1 200)');
  });

  it('formats labels with the given unit suffix', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)', '%');
    const series = (option['series'] as any[])[0];
    expect(series.label.formatter).toBe('{c}%');
  });

  it('defaults to no unit suffix', () => {
    const option = buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.label.formatter).toBe('{c}');
  });

  it('does not mutate the input items array', () => {
    const original = [...items];
    buildRankingBarOption(items, 'oklch(0.5 0.1 200)');
    expect(items).toEqual(original);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx ng test --watch=false --include='src/app/shared/ui/echart/ranking-bar-chart.spec.ts'`
Expected: FAIL — cannot find module `@app/shared/ui/echart/ranking-bar-chart`.

- [ ] **Step 3: Write the implementation**

Create `frontend/src/app/shared/ui/echart/ranking-bar-chart.ts`:

```ts
import type { EChartsCoreOption } from 'echarts/core';

export interface RankingBarItem {
  label: string;
  value: number;
}

export function buildRankingBarOption(
  items: RankingBarItem[],
  color: string,
  unit: '%' | '' = '',
): EChartsCoreOption {
  const reversed = [...items].reverse();

  return {
    grid: { left: 100, right: 40, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category',
      data: reversed.map((i) => i.label),
      axisLine: { show: false },
      axisTick: { show: false },
    },
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v}${unit}` },
    series: [
      {
        type: 'bar',
        data: reversed.map((i) => i.value),
        barWidth: 14,
        itemStyle: { color, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', formatter: `{c}${unit}` },
      },
    ],
  };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx ng test --watch=false --include='src/app/shared/ui/echart/ranking-bar-chart.spec.ts'`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
cd frontend
git add src/app/shared/ui/echart/ranking-bar-chart.ts src/app/shared/ui/echart/ranking-bar-chart.spec.ts
git commit -m "feat(charts): add shared ranking-bar-chart option builder"
```

---

## Task 4: Migrate admin-dashboard

**Files:**
- Modify: `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.ts`
- Modify: `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.html`
- Modify: `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts`

**Interfaces:**
- Consumes: `EChart` (Task 2), `CHART_COLORS` (Task 1), `buildRankingBarOption`/`RankingBarItem` (Task 3).
- Produces: exported `buildBookingsBarOption(bars: number[]): EChartsCoreOption`; component fields `bookingsChartOptions: EChartsCoreOption`, `destinationsChartOptions: EChartsCoreOption`.

- [ ] **Step 1: Write the failing tests**

Replace `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts` in full:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideBus,
  lucideHotel,
  lucidePlane,
  lucideTrendingUp,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import {
  AdminDashboard,
  bookingBarHeight,
  buildBookingsBarOption,
} from '@app/features/admin/components/admin-dashboard/admin-dashboard';

describe('bookingBarHeight', () => {
  it('matches the sine-based formula from the React source', () => {
    for (let i = 0; i < 30; i++) {
      expect(bookingBarHeight(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.7) * 70) + (i % 4) * 5);
    }
  });
});

describe('buildBookingsBarOption', () => {
  it('puts the bars array directly into the bar series data, unmodified', () => {
    const bars = [10, 50, 90];
    const option = buildBookingsBarOption(bars);
    const series = (option['series'] as any[])[0];
    expect(series.data).toEqual(bars);
  });

  it('labels the x-axis with 1-based day numbers', () => {
    const option = buildBookingsBarOption([10, 50, 90]);
    const xAxis = option['xAxis'] as { data: string[] };
    expect(xAxis.data).toEqual(['1', '2', '3']);
  });
});

describe('AdminDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [
        provideIcons({
          lucideBus,
          lucideHotel,
          lucidePlane,
          lucideTrendingUp,
          lucideUsers,
          lucideWallet,
        }),
      ],
    }).compileComponents();
  });

  it('renders all 6 stat labels, values, and trends', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Total Trips');
    expect(text).toContain('248');
    expect(text).toContain('+12%');
    expect(text).toContain('Active Users');
    expect(text).toContain('1,842');
    expect(text).toContain('+8%');
    expect(text).toContain('Revenue (MTD)');
    expect(text).toContain('₹6.4L');
    expect(text).toContain('+18%');
    expect(text).toContain('Buses');
    expect(text).toContain('36');
    expect(text).toContain('—');
    expect(text).toContain('Hotels');
    expect(text).toContain('89');
    expect(text).toContain('+3');
    expect(text).toContain('Bus Occupancy');
    expect(text).toContain('82%');
    expect(text).toContain('+5%');
  });

  it('builds 30 bars matching the sine formula, fed into the bookings chart options', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    const c = fixture.componentInstance;
    expect(c.bars).toHaveLength(30);
    c.bars.forEach((h, i) => {
      expect(h).toBeCloseTo(bookingBarHeight(i));
    });
    const series = (c.bookingsChartOptions['series'] as any[])[0];
    expect(series.data).toEqual(c.bars);
  });

  it('builds a destinations chart with all 5 names and percentages', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.destinationsChartOptions['yAxis'] as { data: string[] };
    const series = (c.destinationsChartOptions['series'] as any[])[0];

    for (const d of [
      { name: 'Goa', pct: 92 },
      { name: 'Manali', pct: 74 },
      { name: 'Kerala', pct: 68 },
      { name: 'Pondicherry', pct: 55 },
      { name: 'Coorg', pct: 41 },
    ]) {
      const idx = yAxis.data.indexOf(d.name);
      expect(idx).toBeGreaterThanOrEqual(0);
      expect(series.data[idx]).toBe(d.pct);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts'`
Expected: FAIL — `buildBookingsBarOption` is not exported, `destinationsChartOptions`/`bookingsChartOptions` do not exist on `AdminDashboard`.

- [ ] **Step 3: Write the implementation**

Replace `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.ts` in full:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import type { EChartsCoreOption } from 'echarts/core';

interface StatCard {
  label: string;
  value: string;
  icon: string;
  trend: string;
}

interface DestinationRow {
  name: string;
  pct: number;
}

const STATS: StatCard[] = [
  { label: 'Total Trips', value: '248', icon: 'lucidePlane', trend: '+12%' },
  { label: 'Active Users', value: '1,842', icon: 'lucideUsers', trend: '+8%' },
  { label: 'Revenue (MTD)', value: '₹6.4L', icon: 'lucideWallet', trend: '+18%' },
  { label: 'Buses', value: '36', icon: 'lucideBus', trend: '—' },
  { label: 'Hotels', value: '89', icon: 'lucideHotel', trend: '+3' },
  { label: 'Bus Occupancy', value: '82%', icon: 'lucideTrendingUp', trend: '+5%' },
];

const POPULAR_DESTINATIONS: DestinationRow[] = [
  { name: 'Goa', pct: 92 },
  { name: 'Manali', pct: 74 },
  { name: 'Kerala', pct: 68 },
  { name: 'Pondicherry', pct: 55 },
  { name: 'Coorg', pct: 41 },
];

export function bookingBarHeight(i: number): number {
  return 30 + Math.abs(Math.sin(i * 0.7) * 70) + (i % 4) * 5;
}

export function buildBookingsBarOption(bars: number[]): EChartsCoreOption {
  return {
    grid: { left: 8, right: 8, top: 8, bottom: 24, containLabel: false },
    xAxis: {
      type: 'category',
      data: bars.map((_, i) => `${i + 1}`),
      axisLabel: { interval: 4, color: CHART_COLORS.mutedForeground },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: { type: 'value', show: false, max: 100 },
    tooltip: { trigger: 'axis' },
    series: [
      {
        type: 'bar',
        data: bars,
        itemStyle: { color: CHART_COLORS.primary, opacity: 0.8, borderRadius: [4, 4, 0, 0] },
        barCategoryGap: '30%',
      },
    ],
  };
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader, EChart],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard {
  public readonly stats = STATS;
  public readonly destinations = POPULAR_DESTINATIONS;
  public readonly bars = Array.from({ length: 30 }, (_, i) => bookingBarHeight(i));

  public readonly bookingsChartOptions: EChartsCoreOption = buildBookingsBarOption(this.bars);
  public readonly destinationsChartOptions: EChartsCoreOption = buildRankingBarOption(
    this.destinations.map((d) => ({ label: d.name, value: d.pct })),
    CHART_COLORS.primary,
    '%',
  );
}
```

Replace `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.html` in full:

```html
<app-page-header title="Admin Dashboard" subtitle="Platform health, bookings and inventory at a glance." />

<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
  @for (s of stats; track s.label) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <ng-icon [name]="s.icon" class="h-4 w-4 text-primary mb-2" />
        <p class="text-xs text-muted-foreground">{{ s.label }}</p>
        <p class="text-xl font-semibold mt-0.5">{{ s.value }}</p>
        <p class="text-[11px] text-success mt-1">{{ s.trend }}</p>
      </div>
    </div>
  }
</div>

<div class="grid lg:grid-cols-3 gap-6">
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader>
      <h3 hlmCardTitle>Bookings (last 30 days)</h3>
    </div>
    <div hlmCardContent>
      <app-echart [options]="bookingsChartOptions" height="256px" />
    </div>
  </div>
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Popular Destinations</h3>
    </div>
    <div hlmCardContent>
      <app-echart [options]="destinationsChartOptions" height="200px" />
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts'`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
cd frontend
git add src/app/features/admin/components/admin-dashboard/
git commit -m "feat(charts): migrate admin-dashboard bookings and destinations charts to ECharts"
```

---

## Task 5: Migrate admin-funnel

**Files:**
- Modify: `frontend/src/app/features/admin/components/admin-funnel/admin-funnel.ts`
- Modify: `frontend/src/app/features/admin/components/admin-funnel/admin-funnel.html`
- Modify: `frontend/src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts`

**Interfaces:**
- Consumes: `EChart` (Task 2), `CHART_COLORS` (Task 1), `buildRankingBarOption` (Task 3).
- Produces: exported `buildFunnelOption(stages: FunnelStageRow[], color: string): EChartsCoreOption`; component fields `funnelOptions: EChartsCoreOption`, `dropReasonsOptions: EChartsCoreOption`.

- [ ] **Step 1: Write the failing tests**

Replace `frontend/src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts` in full:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideTrendingDown } from '@ng-icons/lucide';
import { dropReasons, funnelStages } from '@app/core/mock-data';
import { AdminFunnel, buildFunnelOption } from '@app/features/admin/components/admin-funnel/admin-funnel';

describe('AdminFunnel', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminFunnel],
      providers: [provideIcons({ lucideTrendingDown })],
    }).compileComponents();
  });

  it('computes conversion and total drop-off correctly', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    const c = fixture.componentInstance;
    const total = funnelStages[0].users;
    const completed = funnelStages[funnelStages.length - 1].users;
    const conversion = ((completed / total) * 100).toFixed(1);
    expect(c.conversion).toBe(conversion);
    expect(c.totalDropOff).toBe((100 - parseFloat(conversion)).toFixed(1));
  });

  it('computes the drop-off percentage between each consecutive stage pair', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    const c = fixture.componentInstance;
    for (let i = 1; i < funnelStages.length; i++) {
      const prev = funnelStages[i - 1].users;
      const curr = funnelStages[i].users;
      const expected = (((prev - curr) / prev) * 100).toFixed(1);
      expect(c.stages[i].dropPct).toBe(expected);
    }
    expect(c.stages[0].dropPct).toBeNull();
  });

  it('builds a drop reasons chart with all 5 reasons and percentages', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    const c = fixture.componentInstance;
    const yAxis = c.dropReasonsOptions['yAxis'] as { data: string[] };
    const series = (c.dropReasonsOptions['series'] as any[])[0];

    for (const r of dropReasons) {
      const idx = yAxis.data.indexOf(r.reason);
      expect(idx).toBeGreaterThanOrEqual(0);
      expect(series.data[idx]).toBe(r.pct);
    }
  });
});

describe('buildFunnelOption', () => {
  const stages = funnelStages.map((s, i) => ({
    stage: s.stage,
    users: s.users,
    dropReason: s.dropReason,
    widthPct: 0,
    pctOfTotal: '0',
    dropPct: i > 0 ? '10.0' : null,
  }));

  it('maps each stage to a funnel data point with its stage name and user count', () => {
    const option = buildFunnelOption(stages, 'oklch(0.5 0.1 200)');
    const series = (option['series'] as any[])[0];
    expect(series.data).toEqual(stages.map((s) => ({ name: s.stage, value: s.users })));
  });

  it('includes the drop-off percentage and reason in the tooltip for stages after the first', () => {
    const option = buildFunnelOption(stages, 'oklch(0.5 0.1 200)');
    const tooltipFormatter = (option['tooltip'] as any).formatter as (p: any) => string;
    const html = tooltipFormatter({ dataIndex: 1 });
    expect(html).toContain('10.0% drop-off');
    expect(html).toContain(stages[1].dropReason);
  });

  it('omits the drop-off line in the tooltip for the first stage', () => {
    const option = buildFunnelOption(stages, 'oklch(0.5 0.1 200)');
    const tooltipFormatter = (option['tooltip'] as any).formatter as (p: any) => string;
    const html = tooltipFormatter({ dataIndex: 0 });
    expect(html).not.toContain('drop-off');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts'`
Expected: FAIL — `buildFunnelOption` is not exported, `dropReasonsOptions` does not exist on `AdminFunnel`.

- [ ] **Step 3: Write the implementation**

Replace `frontend/src/app/features/admin/components/admin-funnel/admin-funnel.ts` in full:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import { dropReasons, funnelStages } from '@app/core/mock-data';
import type { EChartsCoreOption } from 'echarts/core';

interface FunnelStageRow {
  stage: string;
  users: number;
  dropReason: string;
  widthPct: number;
  pctOfTotal: string;
  dropPct: string | null;
}

function buildStages(total: number): FunnelStageRow[] {
  return funnelStages.map((s, i) => {
    const prev = i > 0 ? funnelStages[i - 1].users : s.users;
    const dropPct = i > 0 ? (((prev - s.users) / prev) * 100).toFixed(1) : null;
    return {
      stage: s.stage,
      users: s.users,
      dropReason: s.dropReason,
      widthPct: (s.users / total) * 100,
      pctOfTotal: ((s.users / total) * 100).toFixed(0),
      dropPct,
    };
  });
}

export function buildFunnelOption(stages: FunnelStageRow[], color: string): EChartsCoreOption {
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const stage = stages[params.dataIndex];
        const dropLine = stage.dropPct
          ? `<br/><span style="color:${CHART_COLORS.destructive}">${stage.dropPct}% drop-off — ${stage.dropReason}</span>`
          : '';
        return `<strong>${stage.stage}</strong><br/>${stage.users.toLocaleString()} users${dropLine}`;
      },
    },
    series: [
      {
        type: 'funnel',
        left: '10%',
        right: '10%',
        top: 8,
        bottom: 8,
        gap: 4,
        minSize: '40%',
        maxSize: '100%',
        sort: 'none',
        itemStyle: { color, borderColor: '#fff', borderWidth: 1 },
        label: { show: true, position: 'inside', formatter: '{b}\n{c}' },
        data: stages.map((s) => ({ name: s.stage, value: s.users })),
      },
    ],
  };
}

@Component({
  selector: 'app-admin-funnel',
  imports: [NgIcon, HlmCardImports, PageHeader, EChart],
  templateUrl: './admin-funnel.html',
})
export class AdminFunnel {
  public readonly total = funnelStages[0].users;
  public readonly completed = funnelStages[funnelStages.length - 1].users;
  public readonly conversion = ((this.completed / this.total) * 100).toFixed(1);
  public readonly totalDropOff = (100 - parseFloat(this.conversion)).toFixed(1);
  public readonly stages: FunnelStageRow[] = buildStages(this.total);
  public readonly dropReasons = dropReasons;

  public readonly funnelOptions: EChartsCoreOption = buildFunnelOption(this.stages, CHART_COLORS.primary);
  public readonly dropReasonsOptions: EChartsCoreOption = buildRankingBarOption(
    dropReasons.map((r) => ({ label: r.reason, value: r.pct })),
    CHART_COLORS.accent,
    '%',
  );
}
```

Replace `frontend/src/app/features/admin/components/admin-funnel/admin-funnel.html` in full:

```html
<app-page-header title="Booking Funnel Analytics" subtitle="Where users enter, progress, and drop off in the journey." />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Searches</p>
      <p class="text-2xl font-semibold mt-1">{{ total.toLocaleString() }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Bookings Completed</p>
      <p class="text-2xl font-semibold mt-1">{{ completed.toLocaleString() }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Overall Conversion</p>
      <p class="text-2xl font-semibold mt-1">{{ conversion }}%</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Total Drop-off</p>
      <p class="text-2xl font-semibold mt-1">{{ totalDropOff }}%</p>
    </div>
  </div>
</div>

<div class="grid lg:grid-cols-3 gap-6">
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader>
      <h3 hlmCardTitle>Conversion Funnel</h3>
    </div>
    <div hlmCardContent>
      <app-echart [options]="funnelOptions" height="320px" />
    </div>
  </div>

  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle class="flex items-center gap-2">
        <ng-icon name="lucideTrendingDown" class="h-4 w-4 text-destructive" />Drop-off Reasons
      </h3>
    </div>
    <div hlmCardContent>
      <app-echart [options]="dropReasonsOptions" height="200px" />
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts'`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
cd frontend
git add src/app/features/admin/components/admin-funnel/
git commit -m "feat(charts): migrate admin-funnel to ECharts funnel and ranking-bar charts"
```

---

## Task 6: Migrate admin-route-analytics

**Files:**
- Modify: `frontend/src/app/features/admin/components/admin-route-analytics/admin-route-analytics.ts`
- Modify: `frontend/src/app/features/admin/components/admin-route-analytics/admin-route-analytics.html`
- Modify: `frontend/src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts`

**Interfaces:**
- Consumes: `EChart` (Task 2), `CHART_COLORS` (Task 1), `buildRankingBarOption` (Task 3).
- Produces: component fields `topChartOptions: EChartsCoreOption`, `bottomChartOptions: EChartsCoreOption`. Removes the now-unused `max`/`barWidth` members (dead code once the CSS bars are gone).

- [ ] **Step 1: Write the failing test**

Replace `frontend/src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts` in full:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideClock, lucideTrendingDown, lucideTrendingUp, lucideWallet } from '@ng-icons/lucide';
import { routeAnalytics } from '@app/core/mock-data';
import { AdminRouteAnalytics } from '@app/features/admin/components/admin-route-analytics/admin-route-analytics';

describe('AdminRouteAnalytics', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminRouteAnalytics],
      providers: [
        provideIcons({ lucideClock, lucideTrendingDown, lucideTrendingUp, lucideWallet }),
      ],
    }).compileComponents();
  });

  it('computes total routes, bookings, revenue, and average cancellation correctly', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;
    const totalBookings = routeAnalytics.reduce((s, r) => s + r.bookings, 0);
    const totalRevenue = routeAnalytics.reduce((s, r) => s + r.revenue, 0);
    const avgCancel = (
      routeAnalytics.reduce((s, r) => s + r.cancellation, 0) / routeAnalytics.length
    ).toFixed(1);

    expect(c.totalRoutes).toBe(routeAnalytics.length);
    expect(c.totalBookings).toBe(totalBookings);
    expect(c.totalRevenueLabel).toBe(`₹${(totalRevenue / 100000).toFixed(1)}L`);
    expect(c.avgCancellation).toBe(avgCancel);
  });

  it('selects the top 3 and bottom 3 routes by bookings, in the right order', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;
    const sorted = [...routeAnalytics].sort((a, b) => b.bookings - a.bookings);
    expect(c.top.map((r) => r.route)).toEqual(sorted.slice(0, 3).map((r) => r.route));
    expect(c.bottom.map((r) => r.route)).toEqual(
      [...sorted].slice(-3).reverse().map((r) => r.route),
    );
  });

  it('gives a >7% cancellation route the destructive tone and a <=7% route the success tone', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;
    const high = c.rows.find((r) => r.cancellation > 7)!;
    const low = c.rows.find((r) => r.cancellation <= 7)!;
    expect(high.cancellationClass).toContain('text-destructive');
    expect(low.cancellationClass).toContain('text-success');
  });

  it('builds top/bottom charts with bookings values matching the top/bottom route lists', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;

    const topSeries = (c.topChartOptions['series'] as any[])[0];
    const topYAxis = c.topChartOptions['yAxis'] as { data: string[] };
    for (const r of c.top) {
      const idx = topYAxis.data.indexOf(r.route);
      expect(topSeries.data[idx]).toBe(r.bookings);
    }

    const bottomSeries = (c.bottomChartOptions['series'] as any[])[0];
    const bottomYAxis = c.bottomChartOptions['yAxis'] as { data: string[] };
    for (const r of c.bottom) {
      const idx = bottomYAxis.data.indexOf(r.route);
      expect(bottomSeries.data[idx]).toBe(r.bookings);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts'`
Expected: FAIL — `topChartOptions`/`bottomChartOptions` do not exist on `AdminRouteAnalytics`.

- [ ] **Step 3: Write the implementation**

Replace `frontend/src/app/features/admin/components/admin-route-analytics/admin-route-analytics.ts` in full:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import { routeAnalytics } from '@app/core/mock-data';
import type { EChartsCoreOption } from 'echarts/core';

interface RouteRow {
  route: string;
  bookings: number;
  revenue: number;
  cancellation: number;
  cancellationClass: string;
  duration: string;
}

function cancellationClass(cancellation: number): string {
  return cancellation > 7
    ? 'bg-destructive/10 text-destructive border-destructive/20'
    : 'bg-success/10 text-success border-success/20';
}

function buildRows(): RouteRow[] {
  return [...routeAnalytics]
    .sort((a, b) => b.bookings - a.bookings)
    .map((r) => ({ ...r, cancellationClass: cancellationClass(r.cancellation) }));
}

@Component({
  selector: 'app-admin-route-analytics',
  imports: [NgIcon, HlmCardImports, HlmBadgeImports, PageHeader, EChart],
  templateUrl: './admin-route-analytics.html',
})
export class AdminRouteAnalytics {
  public readonly rows: RouteRow[] = buildRows();
  public readonly top = this.rows.slice(0, 3);
  public readonly bottom = [...this.rows].slice(-3).reverse();

  public readonly totalRoutes = this.rows.length;
  public readonly totalBookings = this.rows.reduce((s, r) => s + r.bookings, 0);
  private readonly totalRevenue = this.rows.reduce((s, r) => s + r.revenue, 0);
  public readonly avgCancellation = (
    this.rows.reduce((s, r) => s + r.cancellation, 0) / this.rows.length
  ).toFixed(1);

  public readonly totalRevenueLabel = `₹${(this.totalRevenue / 100000).toFixed(1)}L`;

  public readonly topChartOptions: EChartsCoreOption = buildRankingBarOption(
    this.top.map((r) => ({ label: r.route, value: r.bookings })),
    CHART_COLORS.success,
  );
  public readonly bottomChartOptions: EChartsCoreOption = buildRankingBarOption(
    this.bottom.map((r) => ({ label: r.route, value: r.bookings })),
    CHART_COLORS.destructive,
  );
}
```

In `frontend/src/app/features/admin/components/admin-route-analytics/admin-route-analytics.html`, replace the "Most Booked Routes" and "Least Booked Routes" card bodies. Change:

```html
    <div hlmCardContent class="space-y-3">
      @for (r of top; track r.route) {
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="font-medium">{{ r.route }}</span>
            <span class="text-muted-foreground tabular-nums">{{ r.bookings }}</span>
          </div>
          <div class="h-2 rounded-full bg-muted overflow-hidden">
            <div class="h-full bg-success" [style.width.%]="barWidth(r.bookings)"></div>
          </div>
        </div>
      }
    </div>
```

to:

```html
    <div hlmCardContent>
      <app-echart [options]="topChartOptions" height="200px" />
    </div>
```

And change:

```html
    <div hlmCardContent class="space-y-3">
      @for (r of bottom; track r.route) {
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="font-medium">{{ r.route }}</span>
            <span class="text-muted-foreground tabular-nums">{{ r.bookings }}</span>
          </div>
          <div class="h-2 rounded-full bg-muted overflow-hidden">
            <div class="h-full bg-destructive/70" [style.width.%]="barWidth(r.bookings)"></div>
          </div>
        </div>
      }
    </div>
```

to:

```html
    <div hlmCardContent>
      <app-echart [options]="bottomChartOptions" height="200px" />
    </div>
```

Leave the stat cards and the "All Routes" table at the bottom of the file unchanged.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts'`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
cd frontend
git add src/app/features/admin/components/admin-route-analytics/
git commit -m "feat(charts): migrate admin-route-analytics ranking bars to ECharts"
```

---

## Task 7: Migrate hotel-dashboard

**Files:**
- Modify: `frontend/src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.ts`
- Modify: `frontend/src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.html`
- Modify: `frontend/src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts`

**Interfaces:**
- Consumes: `EChart` (Task 2), `CHART_COLORS` (Task 1), `buildRankingBarOption` (Task 3).
- Produces: exported `currentMonthDates(referenceDate?: Date): string[]` and `buildOccupancyCalendarOption(dates: string[], color: string, mutedColor: string): EChartsCoreOption`; component fields `calendarOptions`, `calendarMonthLabel`, `roomInventoryOptions`, `ratingOptions`.

- [ ] **Step 1: Write the failing tests**

Replace `frontend/src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts` in full:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideStar,
  lucideWallet,
} from '@ng-icons/lucide';
import { hotelBookings, rooms } from '@app/core/mock-data';
import {
  HotelDashboard,
  buildOccupancyCalendarOption,
  calendarOccupancy,
  currentMonthDates,
} from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';

describe('calendarOccupancy', () => {
  it('matches the sine-based formula from the React source for a few indices', () => {
    for (const i of [0, 5, 13, 27]) {
      expect(calendarOccupancy(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.9) * 60) + (i % 5) * 4);
    }
  });
});

describe('currentMonthDates', () => {
  it('returns one ISO date string per day in a 28-day month', () => {
    const dates = currentMonthDates(new Date(2026, 1, 1));
    expect(dates).toHaveLength(28);
    expect(dates[0]).toBe('2026-02-01');
    expect(dates[27]).toBe('2026-02-28');
  });

  it('handles a 31-day month', () => {
    const dates = currentMonthDates(new Date(2026, 6, 1));
    expect(dates).toHaveLength(31);
    expect(dates[0]).toBe('2026-07-01');
    expect(dates[30]).toBe('2026-07-31');
  });
});

describe('buildOccupancyCalendarOption', () => {
  it('sets the calendar range to the first and last date, and one heatmap point per date', () => {
    const dates = currentMonthDates(new Date(2026, 6, 1));
    const option = buildOccupancyCalendarOption(dates, 'oklch(0.5 0.1 200)', 'oklch(0.9 0.01 200)');
    const calendar = option['calendar'] as any;
    const series = (option['series'] as any[])[0];

    expect(calendar.range).toEqual(['2026-07-01', '2026-07-31']);
    expect(series.data).toHaveLength(31);
    expect(series.data[0]).toEqual(['2026-07-01', calendarOccupancy(0)]);
  });
});

describe('HotelDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HotelDashboard],
      providers: [
        provideIcons({ lucideCalendarDays, lucideDoorOpen, lucideHotel, lucideStar, lucideWallet }),
      ],
    }).compileComponents();
  });

  it('computes all 4 stat values from rooms and hotelBookings', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const totalRooms = rooms.reduce((s, r) => s + r.total, 0);
    const availableRooms = rooms.reduce((s, r) => s + r.available, 0);
    const revenue = hotelBookings.reduce((s, b) => s + b.total, 0);

    expect(c.totalRooms).toBe(totalRooms);
    expect(c.availableRooms).toBe(availableRooms);
    expect(c.bookingsToday).toBe(hotelBookings.length);
    expect(c.revenueMtd).toBe(`₹${(revenue / 1000).toFixed(0)}k`);
  });

  it('builds a calendar chart spanning the current month', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const expectedDates = currentMonthDates();
    const calendar = c.calendarOptions['calendar'] as any;
    expect(calendar.range).toEqual([expectedDates[0], expectedDates[expectedDates.length - 1]]);
  });

  it('renders every hotelBookings entry in Recent Bookings (slice(0,4) keeps all 4)', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of hotelBookings) {
      expect(text).toContain(b.guest);
    }
    expect(fixture.componentInstance.recentBookings).toHaveLength(4);
  });

  it('renders every room type with the correct available/total numbers', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of rooms) {
      expect(text).toContain(r.type);
      expect(text).toContain(`${r.available} / ${r.total}`);
    }
  });

  it('builds a room occupancy chart with the occupied percentage for every room type', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.roomInventoryOptions['yAxis'] as { data: string[] };
    const series = (c.roomInventoryOptions['series'] as any[])[0];

    for (const r of c.roomInventory) {
      const idx = yAxis.data.indexOf(r.type);
      expect(series.data[idx]).toBe(Math.round(r.pct));
    }
  });

  it('renders the hardcoded rating average and review count', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('4.7');
    expect(text).toContain('182 reviews');
  });

  it('builds a rating distribution chart with all 5 star percentages', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.ratingOptions['yAxis'] as { data: string[] };
    const series = (c.ratingOptions['series'] as any[])[0];

    for (const row of c.ratingRows) {
      const idx = yAxis.data.indexOf(`${row.stars}★`);
      expect(series.data[idx]).toBe(row.pct);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts'`
Expected: FAIL — `currentMonthDates`/`buildOccupancyCalendarOption` are not exported, `calendarOptions`/`roomInventoryOptions`/`ratingOptions` do not exist on `HotelDashboard`, and the old `calendar` (28-cell array) property is gone.

- [ ] **Step 3: Write the implementation**

Replace `frontend/src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.ts` in full:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import { hotelBookings, rooms } from '@app/core/mock-data';
import type { EChartsCoreOption } from 'echarts/core';

const RATING_PERCENTAGES = [72, 18, 6, 2, 2];

interface RoomInventoryView {
  id: string;
  type: string;
  price: number;
  available: number;
  total: number;
  pct: number;
}

interface RatingRow {
  stars: number;
  pct: number;
}

export function calendarOccupancy(i: number): number {
  return 30 + Math.abs(Math.sin(i * 0.9) * 60) + (i % 5) * 4;
}

export function currentMonthDates(referenceDate = new Date()): string[] {
  const year = referenceDate.getFullYear();
  const month = referenceDate.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const pad = (n: number) => String(n).padStart(2, '0');
  return Array.from({ length: daysInMonth }, (_, i) => `${year}-${pad(month + 1)}-${pad(i + 1)}`);
}

export function buildOccupancyCalendarOption(
  dates: string[],
  color: string,
  mutedColor: string,
): EChartsCoreOption {
  return {
    tooltip: {
      formatter: (params: any) => `${params.value[0]}<br/>${params.value[1].toFixed(0)}% occupancy`,
    },
    visualMap: {
      show: false,
      min: 0,
      max: 100,
      inRange: { color: [mutedColor, color] },
    },
    calendar: {
      range: [dates[0], dates[dates.length - 1]],
      cellSize: ['auto', 28],
      itemStyle: { borderWidth: 2, borderColor: '#fff' },
      yearLabel: { show: false },
      monthLabel: { show: false },
      dayLabel: { firstDay: 1 },
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: dates.map((date, i) => [date, calendarOccupancy(i)]),
      },
    ],
  };
}

@Component({
  selector: 'app-hotel-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader, StatusBadge, EChart],
  templateUrl: './hotel-dashboard.html',
})
export class HotelDashboard {
  public readonly totalRooms = rooms.reduce((s, r) => s + r.total, 0);
  public readonly availableRooms = rooms.reduce((s, r) => s + r.available, 0);
  public readonly bookingsToday = hotelBookings.length;
  public readonly revenueMtd = `₹${(hotelBookings.reduce((s, b) => s + b.total, 0) / 1000).toFixed(0)}k`;

  private readonly calendarDates = currentMonthDates();
  public readonly calendarMonthLabel = new Date().toLocaleString('en-US', { month: 'long' });
  public readonly calendarOptions: EChartsCoreOption = buildOccupancyCalendarOption(
    this.calendarDates,
    CHART_COLORS.primary,
    CHART_COLORS.muted,
  );

  public readonly recentBookings = hotelBookings.slice(0, 4);

  public readonly roomInventory: RoomInventoryView[] = rooms.map((r) => ({
    id: r.id,
    type: r.type,
    price: r.price,
    available: r.available,
    total: r.total,
    pct: ((r.total - r.available) / r.total) * 100,
  }));

  public readonly roomInventoryOptions: EChartsCoreOption = buildRankingBarOption(
    this.roomInventory.map((r) => ({ label: r.type, value: Math.round(r.pct) })),
    CHART_COLORS.primary,
    '%',
  );

  public readonly ratingAverage = 4.7;
  public readonly ratingCount = 182;
  public readonly ratingRows: RatingRow[] = [5, 4, 3, 2, 1].map((s) => ({
    stars: s,
    pct: RATING_PERCENTAGES[5 - s],
  }));

  public readonly ratingOptions: EChartsCoreOption = buildRankingBarOption(
    this.ratingRows.map((r) => ({ label: `${r.stars}★`, value: r.pct })),
    CHART_COLORS.warning,
    '%',
  );
}
```

Replace `frontend/src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.html` in full:

```html
<app-page-header title="Hotel Partner Dashboard" subtitle="Sea Breeze Resort · Baga Beach, Goa" />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideDoorOpen" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Total Rooms</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ totalRooms }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideHotel" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Available Rooms</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ availableRooms }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideCalendarDays" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Bookings Today</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ bookingsToday }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideWallet" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Revenue (MTD)</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ revenueMtd }}</p>
    </div>
  </div>
</div>

<div class="grid lg:grid-cols-3 gap-6">
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader>
      <h3 hlmCardTitle>Occupancy Calendar — {{ calendarMonthLabel }}</h3>
    </div>
    <div hlmCardContent>
      <app-echart [options]="calendarOptions" height="220px" />
    </div>
  </div>

  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Recent Bookings</h3>
    </div>
    <div hlmCardContent class="space-y-3">
      @for (b of recentBookings; track b.id) {
        <div class="flex items-center justify-between text-sm">
          <div>
            <p class="font-medium">{{ b.guest }}</p>
            <p class="text-xs text-muted-foreground">{{ b.room }} · {{ b.checkIn }}</p>
          </div>
          <app-status-badge [status]="b.status" />
        </div>
      }
    </div>
  </div>
</div>

<div class="grid lg:grid-cols-2 gap-6 mt-6">
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Room Inventory</h3>
    </div>
    <div hlmCardContent class="grid grid-cols-2 gap-4">
      @for (r of roomInventory; track r.id) {
        <div class="rounded-md border p-4">
          <p class="font-medium">{{ r.type }}</p>
          <p class="text-xs text-muted-foreground">₹{{ r.price.toLocaleString() }}/night</p>
          <div class="flex items-center justify-between mt-3 text-sm">
            <span class="text-muted-foreground">Available</span>
            <span class="font-semibold tabular-nums">{{ r.available }} / {{ r.total }}</span>
          </div>
        </div>
      }
    </div>
  </div>
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Room Occupancy</h3>
    </div>
    <div hlmCardContent>
      <app-echart [options]="roomInventoryOptions" height="200px" />
    </div>
  </div>
</div>

<div hlmCard class="mt-6">
  <div hlmCardHeader>
    <h3 hlmCardTitle class="flex items-center gap-2">
      <ng-icon name="lucideStar" class="h-4 w-4 text-warning fill-warning" />Guest Rating Snapshot
    </h3>
  </div>
  <div hlmCardContent class="flex items-center gap-8">
    <div class="text-center">
      <p class="text-4xl font-semibold">{{ ratingAverage }}</p>
      <p class="text-xs text-muted-foreground">{{ ratingCount }} reviews</p>
    </div>
    <div class="flex-1">
      <app-echart [options]="ratingOptions" height="160px" />
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx ng test --watch=false --include='src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts'`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
cd frontend
git add src/app/features/hotel/components/hotel-dashboard/
git commit -m "feat(charts): migrate hotel-dashboard to ECharts calendar heatmap and ranking bars"
```

---

## Task 8: Full-suite verification

**Files:** none (verification only, no code changes).

**Interfaces:** none.

- [ ] **Step 1: Run the full test suite**

Run: `cd frontend && npx ng test --watch=false`
Expected: PASS — all suites green, including the 4 migrated feature components and the 3 new `shared/ui/echart/*` suites. Console warnings about `HTMLCanvasElement's getContext()` are expected and benign (see Task 2 note).

- [ ] **Step 2: Run a production build**

Run: `cd frontend && npx ng build`
Expected: Build succeeds with no errors. This specifically exercises the SSR/server bundle — since `echarts/core` and its chart/component/renderer modules are imported directly in `echart.ts`, a successful build confirms nothing in that import graph breaks server-side bundling (the `afterNextRender` guard means the code that touches `document`/`ResizeObserver` never executes during the actual SSR render pass, but the modules still need to *load* cleanly under Node).

- [ ] **Step 3: Confirm no leftover references to removed CSS-bar code**

Run: `cd frontend && grep -rn "style.width.%\]=\"d.pct\"\|style.height.%\]=\"h\"\|style.width.%\]=\"barWidth\|style.width.%\]=\"s.widthPct\|style.width.%\]=\"r.pct\"\|style.background\]=\"c.background\"" src/app/features`
Expected: no output (all the CSS-bar bindings this plan replaces are gone).

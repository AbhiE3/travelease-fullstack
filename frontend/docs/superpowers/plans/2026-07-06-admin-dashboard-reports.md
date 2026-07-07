# Admin — Dashboard + Reports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/admin` and `/admin/reports` with real, fully-hardcoded pages ported 1:1 from the React source (except one Angular-specific routing simplification, same as every prior dashboard sub-project).

**Architecture:** Two independent standalone components under `features/admin/components/`, wired into the existing `admin.routes.ts`'s `''` and `'reports'` children only. The other 8 `/admin/*` children stay on `RoutePlaceholder` — they're separate, later sub-projects.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks, with the same one deliberate exception used in every prior dashboard sub-project: the pathname-check + `<Outlet/>` hack on the dashboard route is dropped since Angular's nested child routes don't need it.
- Both pages are entirely hardcoded — neither imports any mock data. This is intentional (platform-aggregate stats, no natural single data source).
- `AdminDashboard`'s "Popular Destinations" (5 entries, field `pct`) and `AdminReports`'s "Top Destinations" (6 entries, field `trips`) are two separate hardcoded arrays with overlapping-but-different data — kept as two separate local constants, not unified, matching React.
- No new icons — `Users`, `Plane`, `Bus`, `Hotel`, `TrendingUp`, `Wallet` are all already registered in `app.config.ts`.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `AdminDashboard`

**Files:**
- Create: `src/app/features/admin/components/admin-dashboard/admin-dashboard.ts`
- Create: `src/app/features/admin/components/admin-dashboard/admin-dashboard.html`
- Test: `src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `AdminDashboard` (standalone component, no inputs) and `bookingBarHeight(i: number): number` (exported pure function), both importable from `@app/features/admin/components/admin-dashboard/admin-dashboard`. Consumed by Task 3's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts`:

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
} from '@app/features/admin/components/admin-dashboard/admin-dashboard';

describe('bookingBarHeight', () => {
  it('matches the sine-based formula from the React source', () => {
    for (let i = 0; i < 30; i++) {
      expect(bookingBarHeight(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.7) * 70) + (i % 4) * 5);
    }
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

  it('renders all 30 bars with heights matching the sine formula', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    expect(fixture.componentInstance.bars).toHaveLength(30);
    fixture.componentInstance.bars.forEach((h, i) => {
      expect(h).toBeCloseTo(bookingBarHeight(i));
    });
  });

  it('renders all 5 Popular Destinations names and percentages', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const d of [
      { name: 'Goa', pct: 92 },
      { name: 'Manali', pct: 74 },
      { name: 'Kerala', pct: 68 },
      { name: 'Pondicherry', pct: 55 },
      { name: 'Coorg', pct: 41 },
    ]) {
      expect(text).toContain(d.name);
      expect(text).toContain(`${d.pct}%`);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-dashboard` not found).

- [ ] **Step 3: Implement `AdminDashboard`**

Create `src/app/features/admin/components/admin-dashboard/admin-dashboard.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

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

@Component({
  selector: 'app-admin-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard {
  public readonly stats = STATS;
  public readonly destinations = POPULAR_DESTINATIONS;
  public readonly bars = Array.from({ length: 30 }, (_, i) => bookingBarHeight(i));
}
```

Create `src/app/features/admin/components/admin-dashboard/admin-dashboard.html`:

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
      <div class="h-64 flex items-end gap-1.5">
        @for (h of bars; track $index) {
          <div class="flex-1 bg-primary/80 rounded-t" [style.height.%]="h"></div>
        }
      </div>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Popular Destinations</h3>
    </div>
    <div hlmCardContent class="space-y-3">
      @for (d of destinations; track d.name) {
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span>{{ d.name }}</span>
            <span class="text-muted-foreground tabular-nums">{{ d.pct }}%</span>
          </div>
          <div class="h-2 rounded-full bg-muted overflow-hidden">
            <div class="h-full bg-primary" [style.width.%]="d.pct"></div>
          </div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-dashboard/admin-dashboard.spec.ts' --watch=false`
Expected: PASS (4 tests)

---

### Task 2: Build `AdminReports`

**Files:**
- Create: `src/app/features/admin/components/admin-reports/admin-reports.ts`
- Create: `src/app/features/admin/components/admin-reports/admin-reports.html`
- Test: `src/app/features/admin/components/admin-reports/admin-reports.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `AdminReports` (standalone component, no inputs), `REVENUE_TREND_LINE_POINTS: string`, `REVENUE_TREND_AREA_POINTS: string` (exported constants), all importable from `@app/features/admin/components/admin-reports/admin-reports`. Consumed by Task 3's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-reports/admin-reports.spec.ts`:

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
  AdminReports,
  REVENUE_TREND_AREA_POINTS,
  REVENUE_TREND_LINE_POINTS,
} from '@app/features/admin/components/admin-reports/admin-reports';

describe('AdminReports', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminReports],
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

  it('renders all 6 stat labels and values', () => {
    const fixture = TestBed.createComponent(AdminReports);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Total Trips');
    expect(text).toContain('248');
    expect(text).toContain('Active Users');
    expect(text).toContain('1,842');
    expect(text).toContain('Revenue');
    expect(text).toContain('₹6.4L');
    expect(text).toContain('Bus Occupancy');
    expect(text).toContain('82%');
    expect(text).toContain('Hotel Occupancy');
    expect(text).toContain('76%');
    expect(text).toContain('Growth (MoM)');
    expect(text).toContain('+18%');
  });

  it('renders the exact SVG polyline points', () => {
    const fixture = TestBed.createComponent(AdminReports);
    fixture.detectChanges();
    const polylines = (fixture.nativeElement as HTMLElement).querySelectorAll('polyline');
    expect(polylines[0].getAttribute('points')).toBe(REVENUE_TREND_LINE_POINTS);
    expect(polylines[1].getAttribute('points')).toBe(REVENUE_TREND_AREA_POINTS);
  });

  it('renders all 6 Top Destinations names and trip counts', () => {
    const fixture = TestBed.createComponent(AdminReports);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const d of [
      { name: 'Goa', trips: 92 },
      { name: 'Manali', trips: 74 },
      { name: 'Kerala', trips: 68 },
      { name: 'Pondicherry', trips: 55 },
      { name: 'Coorg', trips: 41 },
      { name: 'Jaipur', trips: 38 },
    ]) {
      expect(text).toContain(d.name);
      expect(text).toContain(String(d.trips));
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-reports/admin-reports.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-reports` not found).

- [ ] **Step 3: Implement `AdminReports`**

Create `src/app/features/admin/components/admin-reports/admin-reports.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

interface ReportStat {
  label: string;
  value: string;
  icon: string;
}

interface TopDestinationRow {
  name: string;
  trips: number;
}

const STATS: ReportStat[] = [
  { label: 'Total Trips', value: '248', icon: 'lucidePlane' },
  { label: 'Active Users', value: '1,842', icon: 'lucideUsers' },
  { label: 'Revenue', value: '₹6.4L', icon: 'lucideWallet' },
  { label: 'Bus Occupancy', value: '82%', icon: 'lucideBus' },
  { label: 'Hotel Occupancy', value: '76%', icon: 'lucideHotel' },
  { label: 'Growth (MoM)', value: '+18%', icon: 'lucideTrendingUp' },
];

const TOP_DESTINATIONS: TopDestinationRow[] = [
  { name: 'Goa', trips: 92 },
  { name: 'Manali', trips: 74 },
  { name: 'Kerala', trips: 68 },
  { name: 'Pondicherry', trips: 55 },
  { name: 'Coorg', trips: 41 },
  { name: 'Jaipur', trips: 38 },
];

export const REVENUE_TREND_LINE_POINTS =
  '0,160 40,140 80,150 120,110 160,120 200,80 240,90 280,60 320,70 360,40 400,30';
export const REVENUE_TREND_AREA_POINTS = `${REVENUE_TREND_LINE_POINTS} 400,200 0,200`;

@Component({
  selector: 'app-admin-reports',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './admin-reports.html',
})
export class AdminReports {
  public readonly stats = STATS;
  public readonly destinations = TOP_DESTINATIONS;
  public readonly linePoints = REVENUE_TREND_LINE_POINTS;
  public readonly areaPoints = REVENUE_TREND_AREA_POINTS;
}
```

Create `src/app/features/admin/components/admin-reports/admin-reports.html`:

```html
<app-page-header title="Reports & Analytics" subtitle="Platform performance, revenue and occupancy." />

<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
  @for (s of stats; track s.label) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <ng-icon [name]="s.icon" class="h-4 w-4 text-primary mb-2" />
        <p class="text-xs text-muted-foreground">{{ s.label }}</p>
        <p class="text-xl font-semibold mt-0.5">{{ s.value }}</p>
      </div>
    </div>
  }
</div>

<div class="grid lg:grid-cols-2 gap-6">
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Revenue Trend</h3>
    </div>
    <div hlmCardContent>
      <svg viewBox="0 0 400 200" class="w-full h-56">
        <polyline fill="none" stroke="var(--primary)" stroke-width="2.5" [attr.points]="linePoints" />
        <polyline fill="var(--primary)" opacity="0.12" stroke="none" [attr.points]="areaPoints" />
      </svg>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Top Destinations</h3>
    </div>
    <div hlmCardContent class="space-y-3">
      @for (d of destinations; track d.name) {
        <div class="flex items-center gap-3">
          <span class="w-24 text-sm">{{ d.name }}</span>
          <div class="flex-1 h-2 rounded-full bg-muted overflow-hidden">
            <div class="h-full bg-primary" [style.width.%]="d.trips"></div>
          </div>
          <span class="text-xs text-muted-foreground w-10 text-right tabular-nums">{{ d.trips }}</span>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-reports/admin-reports.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 3: Wire `AdminDashboard` and `AdminReports` into `admin.routes.ts`

**Files:**
- Modify: `src/app/features/admin/admin.routes.ts`
- Modify: `src/app/features/admin/admin.routes.spec.ts`

**Interfaces:**
- Consumes: `AdminDashboard` (Task 1), `AdminReports` (Task 2).
- Produces: `ADMIN_ROUTES`'s `''` and `'reports'` children now `loadComponent` the real pages instead of `RoutePlaceholder`, with their `data: { title }` removed. The other 8 children are untouched.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/admin/admin.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AdminDashboard } from '@app/features/admin/components/admin-dashboard/admin-dashboard';
import { AdminReports } from '@app/features/admin/components/admin-reports/admin-reports';
import { ADMIN_ROUTES } from './admin.routes';

describe('ADMIN_ROUTES', () => {
  it('wraps the admin pages in the AppShell with the admin role', async () => {
    expect(ADMIN_ROUTES).toHaveLength(1);
    const shellRoute = ADMIN_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('admin');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all admin paths as children', () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'route-analytics',
      'partners',
      'funnel',
      'approvals',
      'users',
      'trips',
      'buses',
      'hotels',
      'reports',
    ]);
  });

  it('sets a human-readable title for each still-placeholder child route', () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter((r) => r.path !== '' && r.path !== 'reports');
    expect(stillPlaceholder.map((r) => r.data?.['title'])).toEqual([
      'Route Analytics',
      'Partner Analytics',
      'Booking Funnel',
      'Partner Approvals',
      'Users',
      'Trips',
      'Bus Management',
      'Hotel Management',
    ]);
  });

  it('lazily loads the real components for the dashboard and reports routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const dashboardChild = children.find((r) => r.path === '')!;
    expect(await dashboardChild.loadComponent!()).toBe(AdminDashboard);
    const reportsChild = children.find((r) => r.path === 'reports')!;
    expect(await reportsChild.loadComponent!()).toBe(AdminReports);
  });

  it('lazily loads RoutePlaceholder for the remaining 8 child routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter((r) => r.path !== '' && r.path !== 'reports');
    for (const route of stillPlaceholder) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: FAIL — the `''` and `'reports'` children still resolve to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `admin.routes.ts`**

In `src/app/features/admin/admin.routes.ts`, replace the `''` child:

```ts
      {
        path: '',
        loadComponent: () =>
          import('@app/features/admin/components/admin-dashboard/admin-dashboard').then(
            (m) => m.AdminDashboard,
          ),
      },
```

And replace the `'reports'` child:

```ts
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/admin/components/admin-reports/admin-reports').then(
            (m) => m.AdminReports,
          ),
      },
```

Leave the `route-analytics`/`partners`/`funnel`/`approvals`/`users`/`trips`/`buses`/`hotels` children exactly as they are.

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: PASS (5 tests)

---

### Task 4: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–3.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the checks below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/admin" -o /tmp/admin-dashboard-check.html
curl -s "http://localhost:4200/admin/reports" -o /tmp/admin-reports-check.html

echo "Dashboard — Popular Destinations heading: $(grep -c 'Popular Destinations' /tmp/admin-dashboard-check.html)"
echo "Reports — Top Destinations heading: $(grep -c 'Top Destinations' /tmp/admin-reports-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/admin-dashboard-check.html /tmp/admin-reports-check.html | wc -l)"
```

Expected: the first two lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

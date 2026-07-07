# Admin — Route Analytics + Partner Analytics + Booking Funnel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/admin/route-analytics`, `/admin/partners`, `/admin/funnel` with real, mock-data-backed pages, ported 1:1 from the React source — closing out the entire Admin role and the whole migration.

**Architecture:** `PartnerRankingTable` is a standalone, reusable component (built first) taking `data`/`label` inputs, consumed 3 times by `AdminPartners`'s Tabs shell. `AdminRouteAnalytics` and `AdminFunnel` are independent standalone components with their own computed sort/aggregation logic. All 3 wire into `admin.routes.ts`'s final 3 `RoutePlaceholder` children.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Badge`/`Tabs` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- 3 new icons — `TrendingDown`, `Award`, `ArrowDown` — must be registered in `app.config.ts`. Everything else needed (`TrendingUp`, `Wallet`, `Clock`, `Star`) is already registered.
- The `>7% → destructive, else → success` cancellation-badge threshold is defined locally in each of `PartnerRankingTable` and `AdminRouteAnalytics` (small, self-contained pure functions — matching this migration's established pattern of not cross-importing tiny utilities between sibling components) rather than shared via a common utils module.
- Partner status reuses `StatusBadge`'s existing `Accepted`/`Pending` entries via `status === 'Active' ? 'Accepted' : 'Pending'` — no new `StatusBadge` changes.
- Tabs reuse the already-verified client-side `hlmTabs` pattern (`[tab]`/`(tabActivated)` on `hlmTabs`, `[hlmTabsTrigger]`, `[hlmTabsContent]`; inactive content stays in the DOM via the native `hidden` attribute, so `textContent` still includes it).
- No real filtering, partner actions, or persisted tab selection — matching React.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `PartnerRankingTable` and register `Award`/`TrendingDown`

**Files:**
- Create: `src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.ts`
- Create: `src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.html`
- Test: `src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `StatusBadge`; `HlmCardImports`/`HlmBadgeImports` (spartan-ng); `NgIcon`.
- Produces: `PartnerRankingTable` (standalone component, inputs `data: input.required<readonly Partner[]>()`, `label: input.required<string>()`), the exported `Partner` interface (`{ id: string; name: string; city: string; bookings: number; cancellation: number; rating: number; revenue: number; status: string }`), `cancellationClass(cancellation: number): string`, and `partnerBadgeStatus(status: string): string` (exported pure functions), all importable from `@app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table`. Consumed by Task 3's `AdminPartners`.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideAward, lucideStar, lucideTrendingDown } from '@ng-icons/lucide';
import {
  PartnerRankingTable,
  cancellationClass,
  partnerBadgeStatus,
  type Partner,
} from '@app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table';

const SAMPLE_DATA: Partner[] = [
  {
    id: 'p1',
    name: 'Alpha Hotel',
    city: 'Goa',
    bookings: 100,
    cancellation: 3,
    rating: 4.5,
    revenue: 500000,
    status: 'Active',
  },
  {
    id: 'p2',
    name: 'Beta Hotel',
    city: 'Manali',
    bookings: 50,
    cancellation: 12,
    rating: 3.9,
    revenue: 200000,
    status: 'Review',
  },
  {
    id: 'p3',
    name: 'Gamma Hotel',
    city: 'Goa',
    bookings: 80,
    cancellation: 5,
    rating: 4.2,
    revenue: 800000,
    status: 'Active',
  },
];

describe('cancellationClass', () => {
  it('gives >7% a destructive tone and <=7% a success tone', () => {
    expect(cancellationClass(8)).toContain('text-destructive');
    expect(cancellationClass(7)).toContain('text-success');
  });
});

describe('partnerBadgeStatus', () => {
  it('maps Active to Accepted and anything else to Pending', () => {
    expect(partnerBadgeStatus('Active')).toBe('Accepted');
    expect(partnerBadgeStatus('Review')).toBe('Pending');
  });
});

describe('PartnerRankingTable', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PartnerRankingTable],
      providers: [provideIcons({ lucideAward, lucideStar, lucideTrendingDown })],
    }).compileComponents();
  });

  it('shows the highest-revenue entry as Top and the highest-cancellation entry as Needs Attention', () => {
    const fixture = TestBed.createComponent(PartnerRankingTable);
    fixture.componentRef.setInput('data', SAMPLE_DATA);
    fixture.componentRef.setInput('label', 'Hotel');
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Top Hotel');
    expect(text).toContain('Gamma Hotel');
    expect(text).toContain('Needs Attention');
    expect(text).toContain('Beta Hotel');
  });

  it('renders every partner ranked by revenue descending', () => {
    const fixture = TestBed.createComponent(PartnerRankingTable);
    fixture.componentRef.setInput('data', SAMPLE_DATA);
    fixture.componentRef.setInput('label', 'Hotel');
    fixture.detectChanges();
    expect(fixture.componentInstance.sorted().map((p) => p.name)).toEqual([
      'Gamma Hotel',
      'Alpha Hotel',
      'Beta Hotel',
    ]);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`partner-ranking-table` not found).

- [ ] **Step 3: Implement `PartnerRankingTable`**

Create `src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.ts`:

```ts
import { Component, computed, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

export interface Partner {
  id: string;
  name: string;
  city: string;
  bookings: number;
  cancellation: number;
  rating: number;
  revenue: number;
  status: string;
}

export function cancellationClass(cancellation: number): string {
  return cancellation > 7
    ? 'bg-destructive/10 text-destructive border-destructive/20'
    : 'bg-success/10 text-success border-success/20';
}

export function partnerBadgeStatus(status: string): string {
  return status === 'Active' ? 'Accepted' : 'Pending';
}

interface PartnerRow extends Partner {
  rank: number;
  cancellationClass: string;
  badgeStatus: string;
}

@Component({
  selector: 'app-partner-ranking-table',
  imports: [NgIcon, HlmCardImports, HlmBadgeImports, StatusBadge],
  templateUrl: './partner-ranking-table.html',
})
export class PartnerRankingTable {
  public readonly data = input.required<readonly Partner[]>();
  public readonly label = input.required<string>();

  public readonly sorted = computed<PartnerRow[]>(() =>
    [...this.data()]
      .sort((a, b) => b.revenue - a.revenue)
      .map((p, i) => ({
        ...p,
        rank: i + 1,
        cancellationClass: cancellationClass(p.cancellation),
        badgeStatus: partnerBadgeStatus(p.status),
      })),
  );

  public readonly top = computed(() => this.sorted()[0]);
  public readonly needsAttention = computed(
    () => [...this.data()].sort((a, b) => b.cancellation - a.cancellation)[0],
  );
}
```

Create `src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.html`:

```html
<div class="space-y-6">
  <div class="grid md:grid-cols-2 gap-4">
    <div hlmCard class="border-success/30 bg-success/5">
      <div hlmCardContent class="pt-5">
        <div class="flex items-center gap-2 text-success mb-1">
          <ng-icon name="lucideAward" class="h-4 w-4" />
          <p class="text-xs font-medium uppercase tracking-wide">Top {{ label() }}</p>
        </div>
        <p class="text-lg font-semibold">{{ top().name }}</p>
        <p class="text-xs text-muted-foreground">
          {{ top().city }} · ₹{{ top().revenue.toLocaleString() }} revenue · ★ {{ top().rating }}
        </p>
      </div>
    </div>
    <div hlmCard class="border-destructive/30 bg-destructive/5">
      <div hlmCardContent class="pt-5">
        <div class="flex items-center gap-2 text-destructive mb-1">
          <ng-icon name="lucideTrendingDown" class="h-4 w-4" />
          <p class="text-xs font-medium uppercase tracking-wide">Needs Attention</p>
        </div>
        <p class="text-lg font-semibold">{{ needsAttention().name }}</p>
        <p class="text-xs text-muted-foreground">
          {{ needsAttention().city }} · {{ needsAttention().cancellation }}% cancellations · ★
          {{ needsAttention().rating }}
        </p>
      </div>
    </div>
  </div>

  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle>Ranking</h3>
    </div>
    <div hlmCardContent>
      <div class="rounded-md border">
        <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
          <div class="col-span-1">#</div>
          <div class="col-span-3">Partner</div>
          <div class="col-span-2">City</div>
          <div class="col-span-1 text-right">Bookings</div>
          <div class="col-span-2 text-right">Revenue</div>
          <div class="col-span-1 text-right">Cancel</div>
          <div class="col-span-1 text-right">Rating</div>
          <div class="col-span-1 text-right">Status</div>
        </div>
        @for (p of sorted(); track p.id) {
          <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
            <div class="col-span-1 font-semibold text-muted-foreground">#{{ p.rank }}</div>
            <div class="col-span-3 font-medium">{{ p.name }}</div>
            <div class="col-span-2 text-muted-foreground">{{ p.city }}</div>
            <div class="col-span-1 text-right tabular-nums">{{ p.bookings }}</div>
            <div class="col-span-2 text-right tabular-nums">₹{{ p.revenue.toLocaleString() }}</div>
            <div class="col-span-1 text-right">
              <span hlmBadge variant="outline" [class]="p.cancellationClass">{{ p.cancellation }}%</span>
            </div>
            <div class="col-span-1 text-right text-xs">
              <span class="inline-flex items-center gap-1">
                <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ p.rating }}
              </span>
            </div>
            <div class="col-span-1 text-right"><app-status-badge [status]="p.badgeStatus" /></div>
          </div>
        }
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Register `Award` and `TrendingDown` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideAward` and `lucideTrendingDown` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table.spec.ts' --watch=false`
Expected: PASS (4 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 2: Build `AdminRouteAnalytics`

**Files:**
- Create: `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.ts`
- Create: `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.html`
- Test: `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts`

**Interfaces:**
- Consumes: `routeAnalytics` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmBadgeImports` (spartan-ng); `NgIcon`.
- Produces: `AdminRouteAnalytics` (standalone component, no inputs), importable from `@app/features/admin/components/admin-route-analytics/admin-route-analytics`. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts`:

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
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-route-analytics` not found).

- [ ] **Step 3: Implement `AdminRouteAnalytics`**

Create `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { routeAnalytics } from '@app/core/mock-data';

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
  imports: [NgIcon, HlmCardImports, HlmBadgeImports, PageHeader],
  templateUrl: './admin-route-analytics.html',
})
export class AdminRouteAnalytics {
  public readonly rows: RouteRow[] = buildRows();
  public readonly max = this.rows[0].bookings;
  public readonly top = this.rows.slice(0, 3);
  public readonly bottom = [...this.rows].slice(-3).reverse();

  public readonly totalRoutes = this.rows.length;
  public readonly totalBookings = this.rows.reduce((s, r) => s + r.bookings, 0);
  private readonly totalRevenue = this.rows.reduce((s, r) => s + r.revenue, 0);
  public readonly avgCancellation = (
    this.rows.reduce((s, r) => s + r.cancellation, 0) / this.rows.length
  ).toFixed(1);

  public readonly totalRevenueLabel = `₹${(this.totalRevenue / 100000).toFixed(1)}L`;

  public barWidth(bookings: number): number {
    return (bookings / this.max) * 100;
  }
}
```

Create `src/app/features/admin/components/admin-route-analytics/admin-route-analytics.html`:

```html
<app-page-header title="Route Analytics" subtitle="Performance across every booked corridor on the platform." />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideTrendingUp" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Total Routes</p>
      <p class="text-xl font-semibold mt-0.5">{{ totalRoutes }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideTrendingUp" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Total Bookings</p>
      <p class="text-xl font-semibold mt-0.5">{{ totalBookings.toLocaleString() }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideWallet" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Total Revenue</p>
      <p class="text-xl font-semibold mt-0.5">{{ totalRevenueLabel }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideClock" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Avg Cancellation</p>
      <p class="text-xl font-semibold mt-0.5">{{ avgCancellation }}%</p>
    </div>
  </div>
</div>

<div class="grid lg:grid-cols-2 gap-6 mb-6">
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle class="flex items-center gap-2">
        <ng-icon name="lucideTrendingUp" class="h-4 w-4 text-success" />Most Booked Routes
      </h3>
    </div>
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
  </div>
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle class="flex items-center gap-2">
        <ng-icon name="lucideTrendingDown" class="h-4 w-4 text-destructive" />Least Booked Routes
      </h3>
    </div>
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
  </div>
</div>

<div hlmCard>
  <div hlmCardHeader>
    <h3 hlmCardTitle>All Routes</h3>
  </div>
  <div hlmCardContent>
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-4">Route</div>
        <div class="col-span-2 text-right">Bookings</div>
        <div class="col-span-2 text-right">Revenue</div>
        <div class="col-span-2 text-right">Cancel %</div>
        <div class="col-span-2 text-right">Avg Duration</div>
      </div>
      @for (r of rows; track r.route) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-4 font-medium">{{ r.route }}</div>
          <div class="col-span-2 text-right tabular-nums">{{ r.bookings }}</div>
          <div class="col-span-2 text-right tabular-nums">₹{{ r.revenue.toLocaleString() }}</div>
          <div class="col-span-2 text-right">
            <span hlmBadge variant="outline" [class]="r.cancellationClass">{{ r.cancellation }}%</span>
          </div>
          <div class="col-span-2 text-right text-muted-foreground tabular-nums">{{ r.duration }}</div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-route-analytics/admin-route-analytics.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 3: Build `AdminPartners`

**Files:**
- Create: `src/app/features/admin/components/admin-partners/admin-partners.ts`
- Create: `src/app/features/admin/components/admin-partners/admin-partners.html`
- Test: `src/app/features/admin/components/admin-partners/admin-partners.spec.ts`

**Interfaces:**
- Consumes: `PartnerRankingTable` (Task 1); `hotelPartners`/`transportPartners`/`activityPartners` from `@app/core/mock-data`; `PageHeader`; `HlmTabsImports` (spartan-ng).
- Produces: `AdminPartners` (standalone component, no inputs), importable from `@app/features/admin/components/admin-partners/admin-partners`. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-partners/admin-partners.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideAward, lucideStar, lucideTrendingDown } from '@ng-icons/lucide';
import { activityPartners, hotelPartners, transportPartners } from '@app/core/mock-data';
import { AdminPartners } from '@app/features/admin/components/admin-partners/admin-partners';

describe('AdminPartners', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPartners],
      providers: [provideIcons({ lucideAward, lucideStar, lucideTrendingDown })],
    }).compileComponents();
  });

  it('renders all 3 tab triggers', () => {
    const fixture = TestBed.createComponent(AdminPartners);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Hotels');
    expect(text).toContain('Transport');
    expect(text).toContain('Activity Providers');
  });

  it("renders each tab's own partner list (inactive tab content stays in the DOM)", () => {
    const fixture = TestBed.createComponent(AdminPartners);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain(hotelPartners[0].name);
    expect(text).toContain(transportPartners[0].name);
    expect(text).toContain(activityPartners[0].name);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-partners/admin-partners.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-partners` not found).

- [ ] **Step 3: Implement `AdminPartners`**

Create `src/app/features/admin/components/admin-partners/admin-partners.ts`:

```ts
import { Component, signal } from '@angular/core';
import { HlmTabsImports } from '@spartan-ng/helm/tabs';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { PartnerRankingTable } from './partner-ranking-table/partner-ranking-table';
import { activityPartners, hotelPartners, transportPartners } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-partners',
  imports: [HlmTabsImports, PageHeader, PartnerRankingTable],
  templateUrl: './admin-partners.html',
})
export class AdminPartners {
  protected readonly activeTab = signal('hotels');

  public readonly hotelPartners = hotelPartners;
  public readonly transportPartners = transportPartners;
  public readonly activityPartners = activityPartners;
}
```

Create `src/app/features/admin/components/admin-partners/admin-partners.html`:

```html
<app-page-header title="Partner Analytics" subtitle="Performance ranking across hotel, transport and activity partners." />

<div hlmTabs [tab]="activeTab()" (tabActivated)="activeTab.set($event)" class="w-full">
  <div hlmTabsList class="bg-muted/50 p-1">
    <button [hlmTabsTrigger]="'hotels'">Hotels</button>
    <button [hlmTabsTrigger]="'transport'">Transport</button>
    <button [hlmTabsTrigger]="'activity'">Activity Providers</button>
  </div>

  <div [hlmTabsContent]="'hotels'" class="mt-6">
    <app-partner-ranking-table [data]="hotelPartners" label="Hotel" />
  </div>
  <div [hlmTabsContent]="'transport'" class="mt-6">
    <app-partner-ranking-table [data]="transportPartners" label="Transport" />
  </div>
  <div [hlmTabsContent]="'activity'" class="mt-6">
    <app-partner-ranking-table [data]="activityPartners" label="Activity Provider" />
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-partners/admin-partners.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 4: Build `AdminFunnel` and register `ArrowDown`

**Files:**
- Create: `src/app/features/admin/components/admin-funnel/admin-funnel.ts`
- Create: `src/app/features/admin/components/admin-funnel/admin-funnel.html`
- Test: `src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `funnelStages`/`dropReasons` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `AdminFunnel` (standalone component, no inputs), importable from `@app/features/admin/components/admin-funnel/admin-funnel`. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideArrowDown, lucideTrendingDown } from '@ng-icons/lucide';
import { dropReasons, funnelStages } from '@app/core/mock-data';
import { AdminFunnel } from '@app/features/admin/components/admin-funnel/admin-funnel';

describe('AdminFunnel', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminFunnel],
      providers: [provideIcons({ lucideArrowDown, lucideTrendingDown })],
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

  it('renders all 5 drop reasons with the correct bar width', () => {
    const fixture = TestBed.createComponent(AdminFunnel);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of dropReasons) {
      expect(text).toContain(r.reason);
      expect(text).toContain(`${r.pct}%`);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-funnel` not found).

- [ ] **Step 3: Implement `AdminFunnel`**

Create `src/app/features/admin/components/admin-funnel/admin-funnel.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { dropReasons, funnelStages } from '@app/core/mock-data';

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

@Component({
  selector: 'app-admin-funnel',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './admin-funnel.html',
})
export class AdminFunnel {
  public readonly total = funnelStages[0].users;
  public readonly completed = funnelStages[funnelStages.length - 1].users;
  public readonly conversion = ((this.completed / this.total) * 100).toFixed(1);
  public readonly totalDropOff = (100 - parseFloat(this.conversion)).toFixed(1);
  public readonly stages: FunnelStageRow[] = buildStages(this.total);
  public readonly dropReasons = dropReasons;
}
```

Create `src/app/features/admin/components/admin-funnel/admin-funnel.html`:

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
    <div hlmCardContent class="space-y-2">
      @for (s of stages; track s.stage) {
        <div>
          <div
            class="relative mx-auto rounded-lg bg-gradient-to-r from-primary to-primary/70 text-primary-foreground py-4 px-5 shadow-sm"
            [style.width.%]="s.widthPct"
            style="min-width: 240px"
          >
            <div class="flex justify-between items-center">
              <div>
                <p class="text-sm font-medium">{{ s.stage }}</p>
                <p class="text-xs opacity-80">{{ s.dropReason }}</p>
              </div>
              <div class="text-right">
                <p class="text-xl font-semibold tabular-nums">{{ s.users }}</p>
                <p class="text-[11px] opacity-80">{{ s.pctOfTotal }}% of total</p>
              </div>
            </div>
          </div>
          @if (s.dropPct) {
            <div class="flex items-center justify-center gap-1.5 text-xs text-destructive py-1">
              <ng-icon name="lucideArrowDown" class="h-3 w-3" /> {{ s.dropPct }}% drop-off
            </div>
          }
        </div>
      }
    </div>
  </div>

  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle class="flex items-center gap-2">
        <ng-icon name="lucideTrendingDown" class="h-4 w-4 text-destructive" />Drop-off Reasons
      </h3>
    </div>
    <div hlmCardContent class="space-y-4">
      @for (r of dropReasons; track r.reason) {
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span>{{ r.reason }}</span>
            <span class="tabular-nums text-muted-foreground">{{ r.pct }}%</span>
          </div>
          <div class="h-2 rounded-full bg-muted overflow-hidden">
            <div class="h-full bg-accent" [style.width.%]="r.pct * 2"></div>
          </div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Register `ArrowDown` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideArrowDown` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-funnel/admin-funnel.spec.ts' --watch=false`
Expected: PASS (3 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 5: Wire `AdminRouteAnalytics`, `AdminPartners`, and `AdminFunnel` into `admin.routes.ts`

**Files:**
- Modify: `src/app/features/admin/admin.routes.ts`
- Modify: `src/app/features/admin/admin.routes.spec.ts`

**Interfaces:**
- Consumes: `AdminRouteAnalytics` (Task 2), `AdminPartners` (Task 3), `AdminFunnel` (Task 4), plus the already-real `AdminDashboard`/`AdminApprovals`/`AdminUsers`/`AdminTrips`/`AdminBuses`/`AdminHotels`/`AdminReports`.
- Produces: `ADMIN_ROUTES`'s final 3 `RoutePlaceholder` children swap for the real components — every child in `ADMIN_ROUTES` now resolves to a real component, with `data: { title }` removed from all of them.

- [ ] **Step 1: Replace the failing test**

Replace the contents of `src/app/features/admin/admin.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { AdminDashboard } from '@app/features/admin/components/admin-dashboard/admin-dashboard';
import { AdminRouteAnalytics } from '@app/features/admin/components/admin-route-analytics/admin-route-analytics';
import { AdminPartners } from '@app/features/admin/components/admin-partners/admin-partners';
import { AdminFunnel } from '@app/features/admin/components/admin-funnel/admin-funnel';
import { AdminApprovals } from '@app/features/admin/components/admin-approvals/admin-approvals';
import { AdminUsers } from '@app/features/admin/components/admin-users/admin-users';
import { AdminTrips } from '@app/features/admin/components/admin-trips/admin-trips';
import { AdminBuses } from '@app/features/admin/components/admin-buses/admin-buses';
import { AdminHotels } from '@app/features/admin/components/admin-hotels/admin-hotels';
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

  it('lazily loads the real component for every child route', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const expected = [
      AdminDashboard,
      AdminRouteAnalytics,
      AdminPartners,
      AdminFunnel,
      AdminApprovals,
      AdminUsers,
      AdminTrips,
      AdminBuses,
      AdminHotels,
      AdminReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: FAIL — the `'route-analytics'`/`'partners'`/`'funnel'` children still resolve to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `admin.routes.ts`**

Replace the contents of `src/app/features/admin/admin.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'admin' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/admin/components/admin-dashboard/admin-dashboard').then(
            (m) => m.AdminDashboard,
          ),
      },
      {
        path: 'route-analytics',
        loadComponent: () =>
          import(
            '@app/features/admin/components/admin-route-analytics/admin-route-analytics'
          ).then((m) => m.AdminRouteAnalytics),
      },
      {
        path: 'partners',
        loadComponent: () =>
          import('@app/features/admin/components/admin-partners/admin-partners').then(
            (m) => m.AdminPartners,
          ),
      },
      {
        path: 'funnel',
        loadComponent: () =>
          import('@app/features/admin/components/admin-funnel/admin-funnel').then(
            (m) => m.AdminFunnel,
          ),
      },
      {
        path: 'approvals',
        loadComponent: () =>
          import('@app/features/admin/components/admin-approvals/admin-approvals').then(
            (m) => m.AdminApprovals,
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('@app/features/admin/components/admin-users/admin-users').then(
            (m) => m.AdminUsers,
          ),
      },
      {
        path: 'trips',
        loadComponent: () =>
          import('@app/features/admin/components/admin-trips/admin-trips').then(
            (m) => m.AdminTrips,
          ),
      },
      {
        path: 'buses',
        loadComponent: () =>
          import('@app/features/admin/components/admin-buses/admin-buses').then(
            (m) => m.AdminBuses,
          ),
      },
      {
        path: 'hotels',
        loadComponent: () =>
          import('@app/features/admin/components/admin-hotels/admin-hotels').then(
            (m) => m.AdminHotels,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/admin/components/admin-reports/admin-reports').then(
            (m) => m.AdminReports,
          ),
      },
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 6: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–5.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the checks below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/admin/route-analytics" -o /tmp/admin-route-analytics-check.html
curl -s "http://localhost:4200/admin/partners" -o /tmp/admin-partners-check.html
curl -s "http://localhost:4200/admin/funnel" -o /tmp/admin-funnel-check.html

echo "Route Analytics — All Routes heading: $(grep -c 'All Routes' /tmp/admin-route-analytics-check.html)"
echo "Partners — Hotels tab: $(grep -c 'Hotels' /tmp/admin-partners-check.html)"
echo "Partners — a hotel partner name: $(grep -c 'Sea Breeze Resort' /tmp/admin-partners-check.html)"
echo "Funnel — Conversion Funnel heading: $(grep -c 'Conversion Funnel' /tmp/admin-funnel-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/admin-route-analytics-check.html /tmp/admin-partners-check.html /tmp/admin-funnel-check.html | wc -l)"
```

Expected: the first four lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

This is the final sub-project of the entire migration — once this passes, every `/admin/*` route (and every route in the app) renders real content instead of `RoutePlaceholder`.

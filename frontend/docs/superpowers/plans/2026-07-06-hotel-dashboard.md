# Hotel Partner Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/hotel` with a real, mock-data-backed dashboard, ported 1:1 from the React source (except one Angular-specific routing simplification, same as Activity/Transport).

**Architecture:** A single standalone component (`HotelDashboard`) under `features/hotel/components/`, wired into the existing `hotel.routes.ts`'s `''` child only. The other 5 `/hotel/*` children stay on `RoutePlaceholder` — they're a separate, immediately-following sub-project.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks, with the same one deliberate exception used in Activity/Transport: the pathname-check + `<Outlet/>` hack is dropped since Angular's nested child routes don't need it.
- "Bookings Today" is `hotelBookings.length` verbatim — not filtered by any date, despite the label.
- The 28-day occupancy calendar uses a synthetic formula (`30 + abs(sin(i*0.9)*60) + (i%5)*4`), not real data — ported verbatim, including the exact formula and the `color-mix` CSS background.
- Recent Bookings uses `hotelBookings.slice(0, 4)` — currently a no-op since the mock array has exactly 4 entries, kept as-is (matching React) rather than simplified away.
- Guest Rating Snapshot is fully hardcoded (`4.7`, `182 reviews`, star-distribution `[72, 18, 6, 2, 2]` indexed by `[5 - s]`) — ported verbatim, including the index-flip trick.
- No new icons — `DoorOpen`, `Hotel`, `CalendarDays`, `Wallet`, `Star` are all already registered in `app.config.ts`.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `HotelDashboard`

**Files:**
- Create: `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.ts`
- Create: `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.html`
- Test: `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts`

**Interfaces:**
- Consumes: `rooms`, `hotelBookings` from `@app/core/mock-data`; `PageHeader`; `StatusBadge` (already supports `'Confirmed'`/`'Pending'`); `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `HotelDashboard` (standalone component, no inputs) and `calendarOccupancy(i: number): number` (exported pure function), both importable from `@app/features/hotel/components/hotel-dashboard/hotel-dashboard`. Consumed by Task 2's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts`:

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
  calendarOccupancy,
} from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';

describe('calendarOccupancy', () => {
  it('matches the sine-based formula from the React source for a few indices', () => {
    for (const i of [0, 5, 13, 27]) {
      expect(calendarOccupancy(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.9) * 60) + (i % 5) * 4);
    }
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

  it('renders all 28 calendar cells with the correct day numbers', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    expect(fixture.componentInstance.calendar).toHaveLength(28);
    expect(fixture.componentInstance.calendar.map((c) => c.day)).toEqual(
      Array.from({ length: 28 }, (_, i) => i + 1),
    );
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

  it('renders the hardcoded rating snapshot values', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('4.7');
    expect(text).toContain('182 reviews');
    for (const pct of [72, 18, 6, 2, 2]) {
      expect(text).toContain(`${pct}%`);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`hotel-dashboard` not found).

- [ ] **Step 3: Implement `HotelDashboard`**

Create `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { hotelBookings, rooms } from '@app/core/mock-data';

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const RATING_PERCENTAGES = [72, 18, 6, 2, 2];

interface CalendarCell {
  day: number;
  occ: number;
  background: string;
  textToneClass: string;
  subTextToneClass: string;
}

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

function buildCalendar(): CalendarCell[] {
  return Array.from({ length: 28 }, (_, i) => {
    const occ = calendarOccupancy(i);
    return {
      day: i + 1,
      occ,
      background: `color-mix(in oklab, var(--primary) ${occ * 0.6}%, var(--card))`,
      textToneClass:
        occ > 60 ? 'text-primary-foreground font-semibold' : 'text-foreground font-medium',
      subTextToneClass: occ > 60 ? 'text-primary-foreground/80' : 'text-muted-foreground',
    };
  });
}

@Component({
  selector: 'app-hotel-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './hotel-dashboard.html',
})
export class HotelDashboard {
  public readonly days = DAYS;

  public readonly totalRooms = rooms.reduce((s, r) => s + r.total, 0);
  public readonly availableRooms = rooms.reduce((s, r) => s + r.available, 0);
  public readonly bookingsToday = hotelBookings.length;
  public readonly revenueMtd = `₹${(hotelBookings.reduce((s, b) => s + b.total, 0) / 1000).toFixed(0)}k`;

  public readonly calendar: CalendarCell[] = buildCalendar();

  public readonly recentBookings = hotelBookings.slice(0, 4);

  public readonly roomInventory: RoomInventoryView[] = rooms.map((r) => ({
    id: r.id,
    type: r.type,
    price: r.price,
    available: r.available,
    total: r.total,
    pct: ((r.total - r.available) / r.total) * 100,
  }));

  public readonly ratingAverage = 4.7;
  public readonly ratingCount = 182;
  public readonly ratingRows: RatingRow[] = [5, 4, 3, 2, 1].map((s) => ({
    stars: s,
    pct: RATING_PERCENTAGES[5 - s],
  }));
}
```

Create `src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.html`:

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
      <h3 hlmCardTitle>Occupancy Calendar — June</h3>
    </div>
    <div hlmCardContent>
      <div class="grid grid-cols-7 gap-2 text-xs">
        @for (d of days; track d) {
          <div class="text-center text-muted-foreground font-medium">{{ d }}</div>
        }
        @for (c of calendar; track c.day) {
          <div
            class="aspect-square rounded-md border p-1.5 flex flex-col justify-between"
            [style.background]="c.background"
          >
            <span [class]="c.textToneClass">{{ c.day }}</span>
            <span class="text-[10px]" [class]="c.subTextToneClass">{{ c.occ.toFixed(0) }}%</span>
          </div>
        }
      </div>
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

<div hlmCard class="mt-6">
  <div hlmCardHeader>
    <h3 hlmCardTitle>Room Inventory</h3>
  </div>
  <div hlmCardContent>
    <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
      @for (r of roomInventory; track r.id) {
        <div class="rounded-md border p-4">
          <p class="font-medium">{{ r.type }}</p>
          <p class="text-xs text-muted-foreground">₹{{ r.price.toLocaleString() }}/night</p>
          <div class="flex items-center justify-between mt-3 text-sm">
            <span class="text-muted-foreground">Available</span>
            <span class="font-semibold tabular-nums">{{ r.available }} / {{ r.total }}</span>
          </div>
          <div class="h-1.5 rounded-full bg-muted mt-2 overflow-hidden">
            <div class="h-full bg-primary" [style.width.%]="r.pct"></div>
          </div>
        </div>
      }
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
    <div class="flex-1 space-y-1.5">
      @for (row of ratingRows; track row.stars) {
        <div class="flex items-center gap-3 text-xs">
          <span class="w-4">{{ row.stars }}★</span>
          <div class="flex-1 h-1.5 rounded-full bg-muted overflow-hidden">
            <div class="h-full bg-warning" [style.width.%]="row.pct"></div>
          </div>
          <span class="w-8 text-right text-muted-foreground tabular-nums">{{ row.pct }}%</span>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-dashboard/hotel-dashboard.spec.ts' --watch=false`
Expected: PASS (6 tests)

---

### Task 2: Wire `HotelDashboard` into `hotel.routes.ts`'s dashboard route

**Files:**
- Modify: `src/app/features/hotel/hotel.routes.ts`
- Modify: `src/app/features/hotel/hotel.routes.spec.ts`

**Interfaces:**
- Consumes: `HotelDashboard` (Task 1).
- Produces: `HOTEL_ROUTES`'s `''` child now `loadComponent`s the real `HotelDashboard` instead of `RoutePlaceholder`, with its `data: { title }` removed. The other 5 children (`properties`/`rooms`/`bookings`/`reviews`/`reports`) are untouched — still `RoutePlaceholder`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/hotel/hotel.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { HotelDashboard } from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';
import { HOTEL_ROUTES } from './hotel.routes';

describe('HOTEL_ROUTES', () => {
  it('wraps the hotel pages in the AppShell with the hotel role', async () => {
    expect(HOTEL_ROUTES).toHaveLength(1);
    const shellRoute = HOTEL_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('hotel');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all hotel paths as children', () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'properties',
      'rooms',
      'bookings',
      'reviews',
      'reports',
    ]);
  });

  it('sets a human-readable title for each still-placeholder child route', () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter((r) => r.path !== '');
    expect(stillPlaceholder.map((r) => r.data?.['title'])).toEqual([
      'Hotels',
      'Rooms',
      'Bookings',
      'Reviews',
      'Reports',
    ]);
  });

  it('lazily loads the real HotelDashboard for the dashboard route', async () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    const dashboardChild = children.find((r) => r.path === '')!;
    expect(await dashboardChild.loadComponent!()).toBe(HotelDashboard);
  });

  it('lazily loads RoutePlaceholder for the remaining 5 child routes', async () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter((r) => r.path !== '');
    for (const route of stillPlaceholder) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: FAIL — the `''` child still resolves to `RoutePlaceholder`, not `HotelDashboard`.

- [ ] **Step 3: Update `hotel.routes.ts`**

In `src/app/features/hotel/hotel.routes.ts`, replace only the first child (path `''`):

```ts
      {
        path: '',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-dashboard/hotel-dashboard').then(
            (m) => m.HotelDashboard,
          ),
      },
```

Leave the `properties`/`rooms`/`bookings`/`reviews`/`reports` children exactly as they are.

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: PASS (5 tests)

---

### Task 3: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–2.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the check below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/hotel" -o /tmp/hotel-dashboard-check.html

echo "Occupancy Calendar heading: $(grep -c 'Occupancy Calendar' /tmp/hotel-dashboard-check.html)"
echo "Room Inventory heading: $(grep -c 'Room Inventory' /tmp/hotel-dashboard-check.html)"
echo "Guest Rating Snapshot heading: $(grep -c 'Guest Rating Snapshot' /tmp/hotel-dashboard-check.html)"
echo "Coming-soon placeholder still on dashboard (should be 0): $(grep -c 'This section is coming soon.' /tmp/hotel-dashboard-check.html)"
```

Expected: the first three lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

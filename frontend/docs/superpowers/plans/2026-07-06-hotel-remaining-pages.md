# Hotel Partner — Remaining Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/hotel/properties`, `/hotel/rooms`, `/hotel/bookings`, `/hotel/reviews`, `/hotel/reports` with real, mock-data-backed pages, ported 1:1 from the React source, completing the entire Hotel Partner role.

**Architecture:** Five independent standalone components alongside the already-built `hotel-dashboard`, under `features/hotel/components/`, wired into the existing `hotel.routes.ts`'s remaining 5 children.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Avatar` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks.
- `HotelReviews`'s review list stays component-local (hardcoded), not promoted to `@app/core/mock-data` — hardcoded in the React route file too.
- `HotelReports` is entirely hardcoded (4 stat cards + an inline SVG polyline chart with fixed coordinates) — no real data source, ported verbatim including the exact point coordinates.
- `HotelProperties`'s "Live" badge is plain markup (`bg-success/10 text-success border-success/20`), not routed through `StatusBadge` — it's a constant on every card, not derived from any per-hotel field.
- No new icons — `Plus`, `MapPin`, `Star` are all already registered in `app.config.ts`. `StatusBadge` needs no further changes (`Confirmed`/`Pending` already supported).
- No click handlers on non-functional buttons ("Add Property", "Add Room Type", "Manage") — none have one in React.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `HotelProperties`

**Files:**
- Create: `src/app/features/hotel/components/hotel-properties/hotel-properties.ts`
- Create: `src/app/features/hotel/components/hotel-properties/hotel-properties.html`
- Test: `src/app/features/hotel/components/hotel-properties/hotel-properties.spec.ts`

**Interfaces:**
- Consumes: `hotels` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports` (spartan-ng); `NgIcon`.
- Produces: `HotelProperties` (standalone component, no inputs), importable from `@app/features/hotel/components/hotel-properties/hotel-properties`. Consumed by Task 6's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/hotel/components/hotel-properties/hotel-properties.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucidePlus, lucideStar } from '@ng-icons/lucide';
import { hotels } from '@app/core/mock-data';
import { HotelProperties } from '@app/features/hotel/components/hotel-properties/hotel-properties';

describe('HotelProperties', () => {
  it('renders every hotel name, price, and rating', async () => {
    await TestBed.configureTestingModule({
      imports: [HotelProperties],
      providers: [provideIcons({ lucideMapPin, lucidePlus, lucideStar })],
    }).compileComponents();

    const fixture = TestBed.createComponent(HotelProperties);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of hotels) {
      expect(text).toContain(h.name);
      expect(text).toContain(String(h.price));
      expect(text).toContain(String(h.rating));
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-properties/hotel-properties.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`hotel-properties` not found).

- [ ] **Step 3: Implement `HotelProperties`**

Create `src/app/features/hotel/components/hotel-properties/hotel-properties.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-hotel-properties',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, PageHeader],
  templateUrl: './hotel-properties.html',
})
export class HotelProperties {
  public readonly hotels = hotels;
}
```

Create `src/app/features/hotel/components/hotel-properties/hotel-properties.html`:

```html
<app-page-header title="My Hotels" subtitle="Properties you manage on TravelEase.">
  <button hlmBtn action>
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Property
  </button>
</app-page-header>

<div class="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
  @for (h of hotels; track h.id) {
    <div hlmCard class="overflow-hidden">
      <img [src]="h.image" alt="" class="h-40 w-full object-cover" />
      <div hlmCardContent class="pt-4 space-y-2">
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold">{{ h.name }}</h3>
            <p class="text-xs text-muted-foreground">
              <ng-icon name="lucideMapPin" class="inline h-3 w-3" /> {{ h.area }}
            </p>
          </div>
          <span class="text-xs inline-flex items-center gap-1">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ h.rating }}
          </span>
        </div>
        <div class="flex justify-between text-xs text-muted-foreground">
          <span>{{ h.rooms }} rooms</span>
          <span hlmBadge variant="outline" class="bg-success/10 text-success border-success/20">Live</span>
        </div>
        <div class="flex justify-between items-center pt-2 border-t">
          <p class="font-semibold">
            ₹{{ h.price }}<span class="text-xs text-muted-foreground font-normal">/night</span>
          </p>
          <button hlmBtn size="sm" variant="outline">Manage</button>
        </div>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-properties/hotel-properties.spec.ts' --watch=false`
Expected: PASS

---

### Task 2: Build `ManageRooms`

**Files:**
- Create: `src/app/features/hotel/components/manage-rooms/manage-rooms.ts`
- Create: `src/app/features/hotel/components/manage-rooms/manage-rooms.html`
- Test: `src/app/features/hotel/components/manage-rooms/manage-rooms.spec.ts`

**Interfaces:**
- Consumes: `rooms` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `ManageRooms` (standalone component, no inputs) and `roomOccupancy(total: number, available: number): number` (exported pure function), both importable from `@app/features/hotel/components/manage-rooms/manage-rooms`. Consumed by Task 6's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/hotel/components/manage-rooms/manage-rooms.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { rooms } from '@app/core/mock-data';
import {
  ManageRooms,
  roomOccupancy,
} from '@app/features/hotel/components/manage-rooms/manage-rooms';

describe('roomOccupancy', () => {
  it('matches the formula from the React source', () => {
    expect(roomOccupancy(12, 4)).toBeCloseTo(((12 - 4) / 12) * 100);
    expect(roomOccupancy(6, 1)).toBeCloseTo(((6 - 1) / 6) * 100);
  });
});

describe('ManageRooms', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageRooms],
      providers: [provideIcons({ lucidePlus })],
    }).compileComponents();
  });

  it('renders every room type, and price', () => {
    const fixture = TestBed.createComponent(ManageRooms);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of rooms) {
      expect(text).toContain(r.type);
      expect(text).toContain(r.price.toLocaleString());
    }
  });

  it('computes the correct occupancy percentage per room', () => {
    const fixture = TestBed.createComponent(ManageRooms);
    const rows = fixture.componentInstance.rows;
    for (const r of rooms) {
      const row = rows.find((x) => x.id === r.id)!;
      expect(row.occ).toBeCloseTo(((r.total - r.available) / r.total) * 100);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/hotel/components/manage-rooms/manage-rooms.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`manage-rooms` not found).

- [ ] **Step 3: Implement `ManageRooms`**

Create `src/app/features/hotel/components/manage-rooms/manage-rooms.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { rooms } from '@app/core/mock-data';

interface RoomRow {
  id: string;
  type: string;
  total: number;
  available: number;
  price: number;
  occ: number;
}

export function roomOccupancy(total: number, available: number): number {
  return ((total - available) / total) * 100;
}

@Component({
  selector: 'app-manage-rooms',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './manage-rooms.html',
})
export class ManageRooms {
  public readonly rows: RoomRow[] = rooms.map((r) => ({
    id: r.id,
    type: r.type,
    total: r.total,
    available: r.available,
    price: r.price,
    occ: roomOccupancy(r.total, r.available),
  }));
}
```

Create `src/app/features/hotel/components/manage-rooms/manage-rooms.html`:

```html
<app-page-header title="Manage Rooms" subtitle="Room types, pricing and live availability.">
  <button hlmBtn action>
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Room Type
  </button>
</app-page-header>

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-4">Room Type</div>
        <div class="col-span-2 text-right">Total</div>
        <div class="col-span-2 text-right">Available</div>
        <div class="col-span-2 text-right">Price</div>
        <div class="col-span-2 text-right">Occupancy</div>
      </div>
      @for (r of rows; track r.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-4 font-medium">{{ r.type }}</div>
          <div class="col-span-2 text-right tabular-nums">{{ r.total }}</div>
          <div class="col-span-2 text-right tabular-nums">{{ r.available }}</div>
          <div class="col-span-2 text-right tabular-nums">₹{{ r.price.toLocaleString() }}</div>
          <div class="col-span-2 text-right">
            <div class="inline-flex items-center gap-2">
              <span class="text-xs tabular-nums">{{ r.occ.toFixed(0) }}%</span>
              <div class="w-16 h-1.5 rounded-full bg-muted overflow-hidden">
                <div class="h-full bg-primary" [style.width.%]="r.occ"></div>
              </div>
            </div>
          </div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/hotel/components/manage-rooms/manage-rooms.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 3: Build `HotelBookings`

**Files:**
- Create: `src/app/features/hotel/components/hotel-bookings/hotel-bookings.ts`
- Create: `src/app/features/hotel/components/hotel-bookings/hotel-bookings.html`
- Test: `src/app/features/hotel/components/hotel-bookings/hotel-bookings.spec.ts`

**Interfaces:**
- Consumes: `hotelBookings` from `@app/core/mock-data`; `PageHeader`; `StatusBadge` (already supports `'Confirmed'`/`'Pending'`); `HlmCardImports` (spartan-ng).
- Produces: `HotelBookings` (standalone component, no inputs) with public `bookings` field, importable from `@app/features/hotel/components/hotel-bookings/hotel-bookings`. Consumed by Task 6's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/hotel/components/hotel-bookings/hotel-bookings.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { hotelBookings } from '@app/core/mock-data';
import { HotelBookings } from '@app/features/hotel/components/hotel-bookings/hotel-bookings';

describe('HotelBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HotelBookings] }).compileComponents();
  });

  it('renders every hotelBookings entry guest and room', () => {
    const fixture = TestBed.createComponent(HotelBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of hotelBookings) {
      expect(text).toContain(b.guest);
      expect(text).toContain(b.room);
    }
  });

  it('gives Confirmed and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(HotelBookings);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const confirmedBadge = badges.find((b) => b.textContent === 'Confirmed')!;
    const pendingBadge = badges.find((b) => b.textContent === 'Pending')!;
    expect(confirmedBadge.className).toContain('text-success');
    expect(pendingBadge.className).toContain('border-warning/20');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-bookings/hotel-bookings.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`hotel-bookings` not found).

- [ ] **Step 3: Implement `HotelBookings`**

Create `src/app/features/hotel/components/hotel-bookings/hotel-bookings.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { hotelBookings } from '@app/core/mock-data';

@Component({
  selector: 'app-hotel-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './hotel-bookings.html',
})
export class HotelBookings {
  public readonly bookings = hotelBookings;
}
```

Create `src/app/features/hotel/components/hotel-bookings/hotel-bookings.html`:

```html
<app-page-header title="Bookings" subtitle="All confirmed and pending reservations." />

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-3">Guest</div>
        <div class="col-span-3">Room</div>
        <div class="col-span-2">Check-in</div>
        <div class="col-span-1 text-right">Guests</div>
        <div class="col-span-2 text-right">Total</div>
        <div class="col-span-1 text-right">Status</div>
      </div>
      @for (b of bookings; track b.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-3 font-medium">{{ b.guest }}</div>
          <div class="col-span-3 text-muted-foreground">{{ b.room }}</div>
          <div class="col-span-2">{{ b.checkIn }} → {{ b.checkOut }}</div>
          <div class="col-span-1 text-right tabular-nums">{{ b.guests }}</div>
          <div class="col-span-2 text-right tabular-nums">₹{{ b.total.toLocaleString() }}</div>
          <div class="col-span-1 text-right"><app-status-badge [status]="b.status" /></div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-bookings/hotel-bookings.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 4: Build `HotelReviews`

**Files:**
- Create: `src/app/features/hotel/components/hotel-reviews/hotel-reviews.ts`
- Create: `src/app/features/hotel/components/hotel-reviews/hotel-reviews.html`
- Test: `src/app/features/hotel/components/hotel-reviews/hotel-reviews.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `HlmCardImports`/`HlmAvatarImports` (spartan-ng, `hlm-avatar`/`[hlmAvatarFallback]` selectors); `NgIcon`.
- Produces: `HotelReviews` (standalone component, no inputs), importable from `@app/features/hotel/components/hotel-reviews/hotel-reviews`. Consumed by Task 6's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/hotel/components/hotel-reviews/hotel-reviews.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideStar } from '@ng-icons/lucide';
import { HotelReviews } from '@app/features/hotel/components/hotel-reviews/hotel-reviews';

describe('HotelReviews', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HotelReviews],
      providers: [provideIcons({ lucideStar })],
    }).compileComponents();
  });

  it('renders all 4 hardcoded reviewer names', () => {
    const fixture = TestBed.createComponent(HotelReviews);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Sarathy R');
    expect(text).toContain('Anjali V');
    expect(text).toContain('Raj Patel');
    expect(text).toContain('Priya Sharma');
  });

  it('renders exactly as many stars as each review\'s own rating', () => {
    const fixture = TestBed.createComponent(HotelReviews);
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('[hlmCard]');
    const starCounts = Array.from(cards).map(
      (card) => card.querySelectorAll('ng-icon[name="lucideStar"]').length,
    );
    expect(starCounts).toEqual([5, 4, 5, 3]);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-reviews/hotel-reviews.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`hotel-reviews` not found).

- [ ] **Step 3: Implement `HotelReviews`**

Create `src/app/features/hotel/components/hotel-reviews/hotel-reviews.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

interface Review {
  id: number;
  name: string;
  rating: number;
  text: string;
  date: string;
}

const REVIEWS: Review[] = [
  {
    id: 1,
    name: 'Sarathy R',
    rating: 5,
    text: 'Stunning sea view, spotless rooms, attentive staff. The breakfast spread was top-notch.',
    date: '2 days ago',
  },
  {
    id: 2,
    name: 'Anjali V',
    rating: 4,
    text: 'Great location near Baga. Air-con in our room was a bit slow but service made up for it.',
    date: '1 week ago',
  },
  {
    id: 3,
    name: 'Raj Patel',
    rating: 5,
    text: 'Perfect for a group of 6. The family suite is spacious and the pool is fantastic.',
    date: '2 weeks ago',
  },
  {
    id: 4,
    name: 'Priya Sharma',
    rating: 3,
    text: 'Decent stay but the wifi was patchy in some rooms.',
    date: '3 weeks ago',
  },
];

@Component({
  selector: 'app-hotel-reviews',
  imports: [NgIcon, HlmCardImports, HlmAvatarImports, PageHeader],
  templateUrl: './hotel-reviews.html',
})
export class HotelReviews {
  public readonly reviews = REVIEWS.map((r) => ({ ...r, stars: Array.from({ length: r.rating }) }));
}
```

Create `src/app/features/hotel/components/hotel-reviews/hotel-reviews.html`:

```html
<app-page-header title="Guest Reviews" subtitle="What recent guests said about your property." />

<div class="grid md:grid-cols-2 gap-5">
  @for (r of reviews; track r.id) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <div class="flex items-center gap-3 mb-3">
          <hlm-avatar class="h-9 w-9">
            <span hlmAvatarFallback class="bg-primary/10 text-primary">{{ r.name[0] }}</span>
          </hlm-avatar>
          <div class="flex-1">
            <p class="font-medium text-sm">{{ r.name }}</p>
            <p class="text-xs text-muted-foreground">{{ r.date }}</p>
          </div>
          <div class="flex">
            @for (s of r.stars; track $index) {
              <ng-icon name="lucideStar" class="h-3.5 w-3.5 fill-warning text-warning" />
            }
          </div>
        </div>
        <p class="text-sm text-muted-foreground">{{ r.text }}</p>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-reviews/hotel-reviews.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 5: Build `HotelReports`

**Files:**
- Create: `src/app/features/hotel/components/hotel-reports/hotel-reports.ts`
- Create: `src/app/features/hotel/components/hotel-reports/hotel-reports.html`
- Test: `src/app/features/hotel/components/hotel-reports/hotel-reports.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `HlmCardImports` (spartan-ng).
- Produces: `HotelReports` (standalone component, no inputs), `REVENUE_TREND_LINE_POINTS: string`, `REVENUE_TREND_AREA_POINTS: string` (exported constants), all importable from `@app/features/hotel/components/hotel-reports/hotel-reports`. Consumed by Task 6's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/hotel/components/hotel-reports/hotel-reports.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import {
  HotelReports,
  REVENUE_TREND_AREA_POINTS,
  REVENUE_TREND_LINE_POINTS,
} from '@app/features/hotel/components/hotel-reports/hotel-reports';

describe('HotelReports', () => {
  it('renders all 4 hardcoded stat values and the exact SVG polyline points', async () => {
    await TestBed.configureTestingModule({ imports: [HotelReports] }).compileComponents();
    const fixture = TestBed.createComponent(HotelReports);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';

    expect(text).toContain('78%');
    expect(text).toContain('₹9.4L');
    expect(text).toContain('₹4,820');
    expect(text).toContain('4.7');

    const polylines = el.querySelectorAll('polyline');
    expect(polylines[0].getAttribute('points')).toBe(REVENUE_TREND_LINE_POINTS);
    expect(polylines[1].getAttribute('points')).toBe(REVENUE_TREND_AREA_POINTS);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-reports/hotel-reports.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`hotel-reports` not found).

- [ ] **Step 3: Implement `HotelReports`**

Create `src/app/features/hotel/components/hotel-reports/hotel-reports.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

interface ReportStat {
  label: string;
  value: string;
}

const STATS: ReportStat[] = [
  { label: 'Occupancy', value: '78%' },
  { label: 'Revenue MTD', value: '₹9.4L' },
  { label: 'ADR', value: '₹4,820' },
  { label: 'Avg Rating', value: '4.7' },
];

export const REVENUE_TREND_LINE_POINTS =
  '0,150 40,130 80,140 120,100 160,110 200,80 240,90 280,60 320,70 360,40 400,30';
export const REVENUE_TREND_AREA_POINTS = `${REVENUE_TREND_LINE_POINTS} 400,200 0,200`;

@Component({
  selector: 'app-hotel-reports',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './hotel-reports.html',
})
export class HotelReports {
  public readonly stats = STATS;
  public readonly linePoints = REVENUE_TREND_LINE_POINTS;
  public readonly areaPoints = REVENUE_TREND_AREA_POINTS;
}
```

Create `src/app/features/hotel/components/hotel-reports/hotel-reports.html`:

```html
<app-page-header title="Performance Reports" subtitle="Revenue, occupancy and rating trends." />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  @for (s of stats; track s.label) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <p class="text-xs text-muted-foreground">{{ s.label }}</p>
        <p class="text-2xl font-semibold mt-1">{{ s.value }}</p>
      </div>
    </div>
  }
</div>

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/hotel/components/hotel-reports/hotel-reports.spec.ts' --watch=false`
Expected: PASS

---

### Task 6: Wire the 5 real components into `hotel.routes.ts`

**Files:**
- Modify: `src/app/features/hotel/hotel.routes.ts`
- Modify: `src/app/features/hotel/hotel.routes.spec.ts`

**Interfaces:**
- Consumes: `HotelProperties` (Task 1), `ManageRooms` (Task 2), `HotelBookings` (Task 3), `HotelReviews` (Task 4), `HotelReports` (Task 5).
- Produces: `HOTEL_ROUTES`'s remaining 5 children now `loadComponent` the real pages instead of `RoutePlaceholder`, with `data: { title }` removed.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/hotel/hotel.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { HotelDashboard } from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';
import { HotelProperties } from '@app/features/hotel/components/hotel-properties/hotel-properties';
import { ManageRooms } from '@app/features/hotel/components/manage-rooms/manage-rooms';
import { HotelBookings } from '@app/features/hotel/components/hotel-bookings/hotel-bookings';
import { HotelReviews } from '@app/features/hotel/components/hotel-reviews/hotel-reviews';
import { HotelReports } from '@app/features/hotel/components/hotel-reports/hotel-reports';
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

  it('lazily loads the real component for each child route', async () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    const expected = [
      HotelDashboard,
      HotelProperties,
      ManageRooms,
      HotelBookings,
      HotelReviews,
      HotelReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: FAIL — the 5 remaining children still resolve to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `hotel.routes.ts`**

Replace the contents of `src/app/features/hotel/hotel.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const HOTEL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'hotel' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-dashboard/hotel-dashboard').then(
            (m) => m.HotelDashboard,
          ),
      },
      {
        path: 'properties',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-properties/hotel-properties').then(
            (m) => m.HotelProperties,
          ),
      },
      {
        path: 'rooms',
        loadComponent: () =>
          import('@app/features/hotel/components/manage-rooms/manage-rooms').then(
            (m) => m.ManageRooms,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-bookings/hotel-bookings').then(
            (m) => m.HotelBookings,
          ),
      },
      {
        path: 'reviews',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-reviews/hotel-reviews').then(
            (m) => m.HotelReviews,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-reports/hotel-reports').then(
            (m) => m.HotelReports,
          ),
      },
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 7: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–6.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the checks below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/hotel/properties" -o /tmp/hotel-properties-check.html
curl -s "http://localhost:4200/hotel/rooms" -o /tmp/hotel-rooms-check.html
curl -s "http://localhost:4200/hotel/bookings" -o /tmp/hotel-bookings-check.html
curl -s "http://localhost:4200/hotel/reviews" -o /tmp/hotel-reviews-check.html
curl -s "http://localhost:4200/hotel/reports" -o /tmp/hotel-reports-check.html

echo "Properties — Sea Breeze Resort card: $(grep -c 'Sea Breeze Resort' /tmp/hotel-properties-check.html)"
echo "Rooms — Manage Rooms heading: $(grep -c 'Manage Rooms' /tmp/hotel-rooms-check.html)"
echo "Bookings — a hotelBookings guest name: $(grep -c 'Sarathy R' /tmp/hotel-bookings-check.html)"
echo "Reviews — Guest Reviews heading: $(grep -c 'Guest Reviews' /tmp/hotel-reviews-check.html)"
echo "Reports — Revenue Trend heading: $(grep -c 'Revenue Trend' /tmp/hotel-reports-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/hotel-properties-check.html /tmp/hotel-rooms-check.html /tmp/hotel-bookings-check.html /tmp/hotel-reviews-check.html /tmp/hotel-reports-check.html | wc -l)"
```

Expected: the first five lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

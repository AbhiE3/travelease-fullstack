# Dashboard Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `/dashboard` route's bare `RoutePlaceholder` with the real Dashboard page — hero banner, stat cards, upcoming trips, recent activity, pending invitations, budget summary, and notifications — plus the two shared helpers it needs (`StatusBadge`, `DestinationPill`).

**Architecture:** Two small standalone presentational components (`StatusBadge`, `DestinationPill`) in `src/app/shared/ui/`, consumed by one page component (`DashboardPage`) in `src/app/features/dashboard/dashboard-page/`. `DashboardPage` reads `trips`, `invitations`, `notifications` directly from `@app/core/mock-data` — no service layer, matching the React original. The page itself is pure content; shell wrapping already happens at the route level (`traveler.routes.ts`, from the App Shell sub-project).

**Tech Stack:** Angular 21.2 (standalone, signals), Angular Router, `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Progress`/`Avatar`/`Badge` (all already generated in `libs/ui/` — no new components needed).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Port hardcoded copy verbatim from the React source — don't "improve" static numbers into computed logic (e.g. stat card values, the hero banner's "28 days" line, and the "Recent Activity" feed are all authored content in React, not derived from `mock-data.ts`, and stay that way here).
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.
- Import alias `@app/*` → `src/app/*`.

---

### Task 1: Build `StatusBadge`

**Files:**
- Create: `src/app/shared/ui/status-badge/status-badge.ts`
- Test: `src/app/shared/ui/status-badge/status-badge.spec.ts`

**Interfaces:**
- Consumes: `HlmBadgeImports` (`@spartan-ng/helm/badge`).
- Produces: `StatusBadge` (standalone component, selector `app-status-badge`), importable from `@app/shared/ui/status-badge/status-badge`. Takes a required `status: string` input. Consumed by Task 3 (`DashboardPage`).

- [ ] **Step 1: Write the failing tests**

Create `src/app/shared/ui/status-badge/status-badge.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

function render(status: string) {
  const fixture = TestBed.createComponent(StatusBadge);
  fixture.componentRef.setInput('status', status);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('StatusBadge', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StatusBadge] }).compileComponents();
  });

  it('applies the success color classes for Accepted', () => {
    const el = render('Accepted');
    expect(el.querySelector('span')?.className).toContain('text-success');
  });

  it('applies the warning color classes for Pending', () => {
    const el = render('Pending');
    expect(el.querySelector('span')?.className).toContain('border-warning/20');
  });

  it('applies the destructive color classes for Rejected', () => {
    const el = render('Rejected');
    expect(el.querySelector('span')?.className).toContain('text-destructive');
  });

  it('applies the primary color classes for upcoming', () => {
    const el = render('upcoming');
    expect(el.querySelector('span')?.className).toContain('text-primary');
  });

  it('falls back to no extra color classes for an unmatched status', () => {
    const el = render('SomeUnknownStatus');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('capitalize');
    expect(className).not.toContain('text-success');
    expect(className).not.toContain('text-destructive');
  });

  it('renders the status text', () => {
    const el = render('Accepted');
    expect(el.textContent?.trim()).toBe('Accepted');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`status-badge` not found).

- [ ] **Step 3: Implement `StatusBadge`**

Create `src/app/shared/ui/status-badge/status-badge.ts`:

```ts
import { Component, computed, input } from '@angular/core';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';

const STATUS_CLASS_MAP: Record<string, string> = {
  Accepted: 'bg-success/15 text-success border-success/20',
  Pending: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  Rejected: 'bg-destructive/15 text-destructive border-destructive/20',
  Paid: 'bg-success/15 text-success border-success/20',
  upcoming: 'bg-primary/10 text-primary border-primary/20',
  planning: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  ongoing: 'bg-accent/15 text-accent border-accent/20',
  completed: 'bg-muted text-muted-foreground border-border',
};

@Component({
  selector: 'app-status-badge',
  imports: [HlmBadgeImports],
  template: `<span hlmBadge variant="outline" [class]="badgeClass()">{{ status() }}</span>`,
})
export class StatusBadge {
  public readonly status = input.required<string>();

  protected readonly badgeClass = computed(
    () => `${STATUS_CLASS_MAP[this.status()] ?? ''} capitalize font-medium`,
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: PASS (all 6 tests)

---

### Task 2: Build `DestinationPill` and register `lucideMapPin`

**Files:**
- Create: `src/app/shared/ui/destination-pill/destination-pill.ts`
- Test: `src/app/shared/ui/destination-pill/destination-pill.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `NgIcon`/`provideIcons` (`@ng-icons/core`), `lucideMapPin` (`@ng-icons/lucide`).
- Produces: `DestinationPill` (standalone component, selector `app-destination-pill`), importable from `@app/shared/ui/destination-pill/destination-pill`. Takes required `from: string` and `to: string` inputs. Consumed by Task 3.

- [ ] **Step 1: Write the failing test**

Create `src/app/shared/ui/destination-pill/destination-pill.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin } from '@ng-icons/lucide';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';

describe('DestinationPill', () => {
  it('renders the from and to text with an arrow between them', async () => {
    await TestBed.configureTestingModule({
      imports: [DestinationPill],
      providers: [provideIcons({ lucideMapPin })],
    }).compileComponents();

    const fixture = TestBed.createComponent(DestinationPill);
    fixture.componentRef.setInput('from', 'Bengaluru');
    fixture.componentRef.setInput('to', 'Goa');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Bengaluru');
    expect(text).toContain('Goa');
    expect(text).toContain('→');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/shared/ui/destination-pill/destination-pill.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`destination-pill` not found).

- [ ] **Step 3: Implement `DestinationPill`**

Create `src/app/shared/ui/destination-pill/destination-pill.ts`:

```ts
import { Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-destination-pill',
  imports: [NgIcon],
  template: `
    <span class="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
      <ng-icon name="lucideMapPin" class="h-3 w-3" /> {{ from() }} → {{ to() }}
    </span>
  `,
})
export class DestinationPill {
  public readonly from = input.required<string>();
  public readonly to = input.required<string>();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/shared/ui/destination-pill/destination-pill.spec.ts' --watch=false`
Expected: PASS

- [ ] **Step 5: Register `lucideMapPin` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideMapPin` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call (alongside the 21 icons already there from the App Shell sub-project).

- [ ] **Step 6: Run the full suite**

Run: `npx ng test --watch=false`
Expected: all tests pass (confirms the `app.config.ts` change didn't break anything).

---

### Task 3: Build `DashboardPage`

**Files:**
- Create: `src/app/features/dashboard/dashboard-page/dashboard-page.ts`
- Create: `src/app/features/dashboard/dashboard-page/dashboard-page.html`
- Test: `src/app/features/dashboard/dashboard-page/dashboard-page.spec.ts`

**Interfaces:**
- Consumes: `StatusBadge` (Task 1), `DestinationPill` (Task 2), `trips`/`invitations`/`notifications` from `@app/core/mock-data`, `HlmCardImports`/`HlmButtonImports`/`HlmProgressImports`/`HlmAvatarImports` (spartan-ng), `NgIcon`.
- Produces: `DashboardPage` (standalone component), importable from `@app/features/dashboard/dashboard-page/dashboard-page`. Public/protected fields `upcomingTrips`, `notifications`, `invitations`, `recentActivity`, `stats` (all plain arrays, no signals needed — the underlying mock data doesn't change at runtime). Consumed by Task 4.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/dashboard/dashboard-page/dashboard-page.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendar,
  lucideMapPin,
  lucidePlane,
  lucidePlus,
  lucideTrendingUp,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { invitations, notifications, trips } from '@app/core/mock-data';
import { DashboardPage } from '@app/features/dashboard/dashboard-page/dashboard-page';

describe('DashboardPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        provideIcons({
          lucidePlus,
          lucidePlane,
          lucideUsers,
          lucideWallet,
          lucideCalendar,
          lucideTrendingUp,
          lucideMapPin,
        }),
      ],
    }).compileComponents();
  });

  it('filters upcoming trips to only upcoming and planning statuses', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;

    expect(
      component.upcomingTrips.every((t) => t.status === 'upcoming' || t.status === 'planning'),
    ).toBe(true);
    expect(component.upcomingTrips.some((t) => t.status === 'completed')).toBe(false);
    expect(component.upcomingTrips.length).toBeGreaterThan(0);
    expect(component.upcomingTrips.length).toBeLessThan(trips.length);
  });

  it('renders every pending invitation', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const inv of invitations) {
      expect(text).toContain(inv.trip);
    }
  });

  it('caps notifications at 3 even though mock-data has more', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;

    expect(notifications.length).toBeGreaterThan(3);
    expect(component.notifications).toHaveLength(3);
    expect(component.notifications).toEqual(notifications.slice(0, 3));
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/dashboard/dashboard-page/dashboard-page.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`dashboard-page` not found).

- [ ] **Step 3: Implement `DashboardPage`**

Create `src/app/features/dashboard/dashboard-page/dashboard-page.ts`:

```ts
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';
import { invitations, notifications, trips } from '@app/core/mock-data';

interface StatCard {
  label: string;
  value: string;
  hint: string;
  icon: string;
}

interface ActivityItem {
  who: string;
  action: string;
  what: string;
  time: string;
}

const STATS: StatCard[] = [
  { label: 'Active Trips', value: '2', hint: '1 upcoming', icon: 'lucidePlane' },
  { label: 'Pending Invites', value: '2', hint: 'Respond soon', icon: 'lucideUsers' },
  { label: 'Budget Used', value: '68%', hint: '₹64,200 of ₹94,000', icon: 'lucideWallet' },
  { label: 'Next Trip', value: '28d', hint: 'Goa · Jul 12', icon: 'lucideCalendar' },
];

const RECENT_ACTIVITY: ActivityItem[] = [
  { who: 'Raj', action: 'added expense', what: "Dinner at Britto's · ₹4,200", time: '2h ago' },
  { who: 'Priya', action: 'joined', what: 'Goa Beach Escape', time: '5h ago' },
  { who: 'Sarathy', action: 'selected hotel', what: 'Sea Breeze Resort', time: '1d ago' },
  { who: 'VRL', action: 'flagged delay', what: 'Bus delayed by 180 min', time: '1d ago' },
];

@Component({
  selector: 'app-dashboard-page',
  imports: [
    RouterLink,
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmProgressImports,
    HlmAvatarImports,
    StatusBadge,
    DestinationPill,
  ],
  templateUrl: './dashboard-page.html',
})
export class DashboardPage {
  protected readonly stats = STATS;
  protected readonly recentActivity = RECENT_ACTIVITY;
  protected readonly invitations = invitations;
  protected readonly notifications = notifications.slice(0, 3);
  protected readonly upcomingTrips = trips.filter(
    (t) => t.status === 'upcoming' || t.status === 'planning',
  );
}
```

Create `src/app/features/dashboard/dashboard-page/dashboard-page.html`:

```html
<div
  class="rounded-2xl p-8 mb-8 text-primary-foreground relative overflow-hidden"
  style="background: var(--gradient-hero)"
>
  <div class="relative z-10 max-w-2xl">
    <p class="text-sm font-medium opacity-80">Welcome back, Sarathy 👋</p>
    <h2 class="text-3xl font-semibold mt-2 leading-tight">Your Goa trip starts in 28 days.</h2>
    <p class="mt-3 opacity-85">2 invitations pending, ₹14,800 to settle, and 3 itinerary slots open.</p>
    <div class="mt-6 flex flex-wrap gap-2">
      <a
        hlmBtn
        variant="secondary"
        class="bg-white/15 hover:bg-white/25 text-primary-foreground border-0 backdrop-blur"
        routerLink="/trips/new"
      >
        <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" /> Create Trip
      </a>
      <a hlmBtn variant="ghost" class="text-primary-foreground hover:bg-white/15" routerLink="/invitations">
        Join a Trip
      </a>
      <a hlmBtn variant="ghost" class="text-primary-foreground hover:bg-white/15" routerLink="/trips">
        View Trips
      </a>
    </div>
  </div>
</div>

<div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
  @for (s of stats; track s.label) {
    <div hlmCard class="border-border/60">
      <div hlmCardContent class="pt-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-muted-foreground">{{ s.label }}</p>
            <p class="text-2xl font-semibold mt-1">{{ s.value }}</p>
            <p class="text-xs text-muted-foreground mt-1">{{ s.hint }}</p>
          </div>
          <div class="h-10 w-10 rounded-lg bg-primary/10 text-primary grid place-items-center">
            <ng-icon [name]="s.icon" class="h-5 w-5" />
          </div>
        </div>
      </div>
    </div>
  }
</div>

<div class="grid lg:grid-cols-3 gap-6">
  <div class="lg:col-span-2 space-y-6">
    <div hlmCard>
      <div hlmCardHeader class="flex flex-row items-center justify-between">
        <h3 hlmCardTitle>Upcoming Trips</h3>
        <a hlmBtn variant="ghost" size="sm" routerLink="/trips">View all</a>
      </div>
      <div hlmCardContent class="space-y-3">
        @for (t of upcomingTrips; track t.id) {
          <a [routerLink]="['/trips', t.id]" class="flex gap-4 p-3 rounded-lg hover:bg-muted/50 transition-colors">
            <img [src]="t.image" alt="" class="h-20 w-28 rounded-md object-cover" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <h3 class="font-medium truncate">{{ t.name }}</h3>
                <app-status-badge [status]="t.status" />
              </div>
              <app-destination-pill [from]="t.source" [to]="t.destination" />
              <div class="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                <span><ng-icon name="lucideCalendar" class="inline h-3 w-3 mr-1" />{{ t.startDate }} → {{ t.endDate }}</span>
                <span><ng-icon name="lucideUsers" class="inline h-3 w-3 mr-1" />{{ t.members }} members</span>
              </div>
              <div class="mt-2 flex items-center gap-2">
                <hlm-progress [value]="t.progress" class="h-1.5"><hlm-progress-indicator /></hlm-progress>
                <span class="text-xs text-muted-foreground tabular-nums">{{ t.progress }}%</span>
              </div>
            </div>
          </a>
        }
      </div>
    </div>

    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle>Recent Activity</h3></div>
      <div hlmCardContent class="space-y-3">
        @for (a of recentActivity; track a.who + a.time) {
          <div class="flex items-start gap-3 text-sm">
            <hlm-avatar class="h-8 w-8">
              <span hlmAvatarFallback class="bg-primary/10 text-primary text-xs">{{ a.who[0] }}</span>
            </hlm-avatar>
            <div class="flex-1">
              <span class="font-medium">{{ a.who }}</span> {{ a.action }}
              <span class="text-muted-foreground">{{ a.what }}</span>
            </div>
            <span class="text-xs text-muted-foreground">{{ a.time }}</span>
          </div>
        }
      </div>
    </div>
  </div>

  <div class="space-y-6">
    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle>Pending Invitations</h3></div>
      <div hlmCardContent class="space-y-3">
        @for (inv of invitations; track inv.id) {
          <div class="p-3 rounded-lg border space-y-2">
            <div class="flex items-start justify-between gap-2">
              <div>
                <p class="font-medium text-sm">{{ inv.trip }}</p>
                <p class="text-xs text-muted-foreground">By {{ inv.organizer }} · {{ inv.dates }}</p>
              </div>
              <span class="text-xs text-muted-foreground">
                <ng-icon name="lucideUsers" class="inline h-3 w-3" /> {{ inv.members }}
              </span>
            </div>
            <div class="flex gap-2">
              <button hlmBtn size="sm" class="flex-1">Accept</button>
              <button hlmBtn variant="outline" size="sm" class="flex-1">Decline</button>
            </div>
          </div>
        }
      </div>
    </div>

    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle>Budget Summary</h3></div>
      <div hlmCardContent class="space-y-3">
        <div>
          <div class="flex justify-between text-sm mb-1.5">
            <span class="text-muted-foreground">Goa Beach Escape</span>
            <span class="font-medium">68%</span>
          </div>
          <hlm-progress [value]="68" class="h-2"><hlm-progress-indicator /></hlm-progress>
          <div class="flex justify-between text-xs text-muted-foreground mt-1.5">
            <span>₹64,200 spent</span><span>of ₹94,000</span>
          </div>
        </div>
        <div class="pt-3 border-t">
          <div class="flex items-center gap-2 text-sm">
            <ng-icon name="lucideTrendingUp" class="h-4 w-4 text-success" />You're tracking under budget
          </div>
        </div>
      </div>
    </div>

    <div hlmCard>
      <div hlmCardHeader class="flex flex-row items-center justify-between">
        <h3 hlmCardTitle>Notifications</h3>
        <a hlmBtn variant="ghost" size="sm" routerLink="/notifications">All</a>
      </div>
      <div hlmCardContent class="space-y-2">
        @for (n of notifications; track n.id) {
          <div class="text-sm py-1.5 border-b last:border-0">
            <p class="font-medium">{{ n.title }}</p>
            <p class="text-xs text-muted-foreground">{{ n.desc }} · {{ n.time }}</p>
          </div>
        }
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/dashboard/dashboard-page/dashboard-page.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 4: Wire `DashboardPage` into the route

**Files:**
- Modify: `src/app/features/dashboard/dashboard.routes.ts`
- Modify: `src/app/features/dashboard/dashboard.routes.spec.ts`

**Interfaces:**
- Consumes: `DashboardPage` (Task 3).
- Produces: `DASHBOARD_ROUTES` now lazily loads `DashboardPage` instead of `RoutePlaceholder`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/dashboard/dashboard.routes.spec.ts`:

```ts
import { DashboardPage } from '@app/features/dashboard/dashboard-page/dashboard-page';
import { DASHBOARD_ROUTES } from './dashboard.routes';

describe('DASHBOARD_ROUTES', () => {
  it('defines the dashboard index route', () => {
    expect(DASHBOARD_ROUTES.map((r) => r.path)).toEqual(['']);
  });

  it('lazily loads DashboardPage', async () => {
    const loaded = await DASHBOARD_ROUTES[0].loadComponent!();
    expect(loaded).toBe(DashboardPage);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/dashboard/dashboard.routes.spec.ts' --watch=false`
Expected: FAIL — the route still lazily loads `RoutePlaceholder`, not `DashboardPage`.

- [ ] **Step 3: Update `dashboard.routes.ts`**

Replace the contents of `src/app/features/dashboard/dashboard.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/dashboard/dashboard-page/dashboard-page').then((m) => m.DashboardPage),
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/dashboard/dashboard.routes.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 5: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–4.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the 4 new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
curl -s "http://localhost:4200/dashboard" -o /tmp/dashboard-check.html
echo "Upcoming Trips heading: $(grep -c 'Upcoming Trips' /tmp/dashboard-check.html)"
echo "Goa Beach Escape trip: $(grep -c 'Goa Beach Escape' /tmp/dashboard-check.html)"
echo "Pending Invitations heading: $(grep -c 'Pending Invitations' /tmp/dashboard-check.html)"
echo "Recent Activity heading: $(grep -c 'Recent Activity' /tmp/dashboard-check.html)"
echo "Sign out (AppShell still wraps it): $(grep -c 'Sign out' /tmp/dashboard-check.html)"
```

Expected: every line reports a count of at least 1.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).

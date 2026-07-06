# Trip Detail — Itinerary + Alerts + Reviews Tabs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the `TripDetail` shell's final 3 tabs (`itinerary`, `alerts`, `reviews`) real content, completing all 8 tabs and the entire Trips module.

**Architecture:** `TripItineraryTab`, `TripAlertsTab`, `TripReviewsTab` are independent presentational components taking no inputs (all backed by global mock data or, for Reviews, fully hardcoded), hosted by `TripDetail` in its final three tab slots. Since every tab now has real content, the "coming soon" placeholder mechanism (`TABS_WITH_REAL_CONTENT`/`placeholderTabs`) is removed entirely as dead code.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Textarea` (all already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Hardcoded content (the 3 Reviews cards, their sub-labels) stays hardcoded, matching the React source — not derived from data.
- The Available Activities list in `TripItineraryTab` uses the full `activities` array — do not slice it (contrast with the Overview tab's 4-item cap).
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.
- Import alias `@app/*` → `src/app/*`.

---

### Task 1: Build `TripItineraryTab` and register `lucideClock`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `itinerary`/`activities` from `@app/core/mock-data`; `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `TripItineraryTab` (standalone component, no inputs), importable from `@app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab`. Public `activities` field. Consumed by Task 4.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideClock, lucideMapPin, lucidePlus, lucideSparkles } from '@ng-icons/lucide';
import { activities, itinerary } from '@app/core/mock-data';
import { TripItineraryTab } from '@app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab';

describe('TripItineraryTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripItineraryTab],
      providers: [provideIcons({ lucideClock, lucideMapPin, lucidePlus, lucideSparkles })],
    }).compileComponents();
  });

  it('renders every itinerary day', () => {
    const fixture = TestBed.createComponent(TripItineraryTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const day of itinerary) {
      expect(text).toContain(day.title);
    }
  });

  it('renders every activity, not a sliced subset', () => {
    const fixture = TestBed.createComponent(TripItineraryTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of activities) {
      expect(text).toContain(a.name);
    }
    expect(fixture.componentInstance.activities).toHaveLength(activities.length);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-itinerary-tab` not found).

- [ ] **Step 3: Implement `TripItineraryTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { activities, itinerary } from '@app/core/mock-data';

@Component({
  selector: 'app-trip-itinerary-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports],
  templateUrl: './trip-itinerary-tab.html',
})
export class TripItineraryTab {
  public readonly itinerary = itinerary;
  public readonly activities = activities;
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.html`:

```html
<div class="grid lg:grid-cols-3 gap-6">
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader class="flex flex-row items-center justify-between">
      <h3 hlmCardTitle>Day-wise Itinerary</h3>
      <button hlmBtn size="sm"><ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Activity</button>
    </div>
    <div hlmCardContent class="space-y-6">
      @for (day of itinerary; track day.day) {
        <div class="flex gap-4">
          <div class="text-center w-16 shrink-0">
            <div class="rounded-md bg-primary text-primary-foreground p-2">
              <p class="text-xs uppercase">Day</p>
              <p class="text-xl font-bold">{{ day.day }}</p>
            </div>
            <p class="text-xs text-muted-foreground mt-1">{{ day.date }}</p>
          </div>
          <div class="flex-1 space-y-2">
            <p class="font-medium">{{ day.title }}</p>
            @for (it of day.items; track it.time) {
              <div class="flex items-start gap-3 p-3 rounded-md border bg-card">
                <ng-icon name="lucideClock" class="h-4 w-4 text-muted-foreground mt-0.5" />
                <div class="flex-1">
                  <p class="text-sm font-medium">{{ it.name }}</p>
                  <p class="text-xs text-muted-foreground">
                    <ng-icon name="lucideMapPin" class="inline h-3 w-3" /> {{ it.location }}
                  </p>
                </div>
                <span class="text-xs text-muted-foreground tabular-nums">{{ it.time }}</span>
              </div>
            }
          </div>
        </div>
      }
    </div>
  </div>
  <div hlmCard>
    <div hlmCardHeader>
      <h3 hlmCardTitle class="flex items-center gap-2">
        <ng-icon name="lucideSparkles" class="h-4 w-4 text-accent" />Available Activities
      </h3>
      <p class="text-xs text-muted-foreground">Drag or click + to slot into a day.</p>
    </div>
    <div hlmCardContent class="space-y-2">
      @for (a of activities; track a.id) {
        <div class="flex items-center gap-3 p-2.5 rounded-md border bg-card">
          <img [src]="a.image" alt="" class="h-12 w-12 rounded object-cover" />
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium truncate">{{ a.name }}</p>
            <p class="text-xs text-muted-foreground">{{ a.duration }} · ₹{{ a.price.toLocaleString() }}</p>
          </div>
          <button hlmBtn size="sm" variant="ghost" class="h-7 w-7 p-0">
            <ng-icon name="lucidePlus" class="h-4 w-4" />
          </button>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Register `lucideClock` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideClock` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab.spec.ts' --watch=false`
Expected: PASS (both tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 2: Build `TripAlertsTab`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.spec.ts`

**Interfaces:**
- Consumes: `alerts` from `@app/core/mock-data`; `HlmCardImports`/`HlmBadgeImports` (spartan-ng); `NgIcon`.
- Produces: `TripAlertsTab` (standalone component, no inputs), importable from `@app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab`. Public `alertViews` field (each entry has a `toneClass: string` derived from `level`). Consumed by Task 4.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideAlertTriangle } from '@ng-icons/lucide';
import { alerts } from '@app/core/mock-data';
import { TripAlertsTab } from '@app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab';

describe('TripAlertsTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripAlertsTab],
      providers: [provideIcons({ lucideAlertTriangle })],
    }).compileComponents();
  });

  it('renders every alert from mock data', () => {
    const fixture = TestBed.createComponent(TripAlertsTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of alerts) {
      expect(text).toContain(a.title);
    }
  });

  it('applies destructive tone classes to a Critical-level alert', () => {
    const fixture = TestBed.createComponent(TripAlertsTab);
    const critical = fixture.componentInstance.alertViews.find((a) => a.level === 'Critical')!;
    expect(critical.toneClass).toContain('text-destructive');
  });

  it('applies primary tone classes to a non-Critical, non-Medium alert', () => {
    const fixture = TestBed.createComponent(TripAlertsTab);
    const low = fixture.componentInstance.alertViews.find(
      (a) => a.level !== 'Critical' && a.level !== 'Medium',
    )!;
    expect(low.toneClass).toContain('text-primary');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-alerts-tab` not found).

- [ ] **Step 3: Implement `TripAlertsTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { alerts } from '@app/core/mock-data';

type Tone = 'destructive' | 'warning' | 'primary';

const TONE_CLASS: Record<Tone, string> = {
  destructive: 'bg-destructive/10 text-destructive border-destructive/20',
  warning: 'bg-warning/10 text-[oklch(0.40_0.12_75)] border-warning/20',
  primary: 'bg-primary/10 text-primary border-primary/20',
};

interface AlertView {
  id: string;
  title: string;
  desc: string;
  impact: string;
  action: string;
  level: string;
  toneClass: string;
}

function toneFor(level: string): Tone {
  if (level === 'Critical') return 'destructive';
  if (level === 'Medium') return 'warning';
  return 'primary';
}

@Component({
  selector: 'app-trip-alerts-tab',
  imports: [NgIcon, HlmCardImports, HlmBadgeImports],
  templateUrl: './trip-alerts-tab.html',
})
export class TripAlertsTab {
  public readonly alertViews: AlertView[] = alerts.map((a) => ({
    ...a,
    toneClass: TONE_CLASS[toneFor(a.level)],
  }));
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.html`:

```html
<div class="space-y-3">
  @for (a of alertViews; track a.id) {
    <div hlmCard>
      <div hlmCardContent class="pt-5 flex gap-4">
        <div class="h-10 w-10 rounded-md grid place-items-center border" [class]="a.toneClass">
          <ng-icon name="lucideAlertTriangle" class="h-5 w-5" />
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2">
            <h3 class="font-semibold">{{ a.title }}</h3>
            <span hlmBadge variant="outline" [class]="a.toneClass">{{ a.level }}</span>
          </div>
          <p class="text-sm text-muted-foreground mt-1">{{ a.desc }}</p>
          <div class="mt-3 grid md:grid-cols-2 gap-3 text-sm">
            <div class="p-3 rounded-md bg-muted/50">
              <p class="text-xs font-medium text-muted-foreground">Impact</p>
              <p>{{ a.impact }}</p>
            </div>
            <div class="p-3 rounded-md bg-muted/50">
              <p class="text-xs font-medium text-muted-foreground">Suggested action</p>
              <p>{{ a.action }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 3: Build `TripReviewsTab`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.spec.ts`

**Interfaces:**
- Consumes: `HlmCardImports`/`HlmButtonImports`/`HlmTextareaImports` (spartan-ng); `NgIcon`.
- Produces: `TripReviewsTab` (standalone component, no inputs), importable from `@app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab`. Consumed by Task 4.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideStar } from '@ng-icons/lucide';
import { TripReviewsTab } from '@app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab';

describe('TripReviewsTab', () => {
  it('renders all 3 hardcoded review card titles', async () => {
    await TestBed.configureTestingModule({
      imports: [TripReviewsTab],
      providers: [provideIcons({ lucideStar })],
    }).compileComponents();

    const fixture = TestBed.createComponent(TripReviewsTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Rate the Bus');
    expect(text).toContain('Rate the Hotel');
    expect(text).toContain('Rate the Trip Experience');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-reviews-tab` not found).

- [ ] **Step 3: Implement `TripReviewsTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmTextareaImports } from '@spartan-ng/helm/textarea';

interface ReviewCard {
  label: string;
  sub: string;
}

const REVIEW_CARDS: ReviewCard[] = [
  { label: 'Rate the Bus', sub: 'Volvo Multi-Axle Sleeper · VRL Travels' },
  { label: 'Rate the Hotel', sub: 'Sea Breeze Resort, Baga Beach' },
  { label: 'Rate the Trip Experience', sub: 'Goa Beach Escape' },
];

@Component({
  selector: 'app-trip-reviews-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmTextareaImports],
  templateUrl: './trip-reviews-tab.html',
})
export class TripReviewsTab {
  public readonly reviewCards = REVIEW_CARDS;
  protected readonly stars = [1, 2, 3, 4, 5];
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.html`:

```html
<div class="grid md:grid-cols-2 gap-6">
  @for (r of reviewCards; track r.label) {
    <div hlmCard>
      <div hlmCardHeader>
        <h3 hlmCardTitle class="text-base">{{ r.label }}</h3>
        <p class="text-xs text-muted-foreground">{{ r.sub }}</p>
      </div>
      <div hlmCardContent class="space-y-3">
        <div class="flex gap-1">
          @for (s of stars; track s) {
            <ng-icon name="lucideStar" class="h-6 w-6 text-warning fill-warning" />
          }
        </div>
        <textarea hlmTextarea class="w-full min-h-24" placeholder="Share your experience…"></textarea>
        <button hlmBtn size="sm">Submit Review</button>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab.spec.ts' --watch=false`
Expected: PASS

---

### Task 4: Host all three tabs in `TripDetail` and remove the placeholder mechanism

**Files:**
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.ts`
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.html`
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`

**Interfaces:**
- Consumes: `TripItineraryTab` (Task 1), `TripAlertsTab` (Task 2), `TripReviewsTab` (Task 3).
- Produces: `TripDetail` now shows real content on all 8 tabs; the `TABS_WITH_REAL_CONTENT`/`placeholderTabs` mechanism is removed (dead code once every tab has content).

- [ ] **Step 1: Update the failing test**

In `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`, add `lucideClock` to the icon imports and `ALL_ICONS`, and replace the test named `'shows the coming-soon placeholder for only 3 tabs now that accommodation and expenses have real content too'` with:

```ts
  it('shows no coming-soon placeholder now that every tab has real content', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('This section is coming soon.');
  });
```

The updated imports/`ALL_ICONS` at the top of the file should read:

```ts
import {
  lucideAlertTriangle,
  lucideArrowLeft,
  lucideArrowRight,
  lucideBus,
  lucideCalendar,
  lucideCheckCircle2,
  lucideClock,
  lucideMapPin,
  lucidePlus,
  lucideSparkles,
  lucideStar,
  lucideUserPlus,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
```
```ts
const ALL_ICONS = {
  lucideAlertTriangle,
  lucideArrowLeft,
  lucideArrowRight,
  lucideBus,
  lucideCalendar,
  lucideCheckCircle2,
  lucideClock,
  lucideMapPin,
  lucidePlus,
  lucideSparkles,
  lucideStar,
  lucideUserPlus,
  lucideUsers,
  lucideWallet,
};
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: FAIL — the itinerary/alerts/reviews tabs still show "This section is coming soon."

- [ ] **Step 3: Update `TripDetail` to host the three new tabs and drop the placeholder mechanism**

Replace the contents of `src/app/features/trips/components/trip-detail/trip-detail.ts`:

```ts
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmTabsImports } from '@spartan-ng/helm/tabs';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { trips } from '@app/core/mock-data';
import { TripOverviewTab } from './tabs/trip-overview-tab/trip-overview-tab';
import { TripMembersTab } from './tabs/trip-members-tab/trip-members-tab';
import { TripTravelTab } from './tabs/trip-travel-tab/trip-travel-tab';
import { TripAccommodationTab } from './tabs/trip-accommodation-tab/trip-accommodation-tab';
import { TripExpensesTab } from './tabs/trip-expenses-tab/trip-expenses-tab';
import { TripItineraryTab } from './tabs/trip-itinerary-tab/trip-itinerary-tab';
import { TripAlertsTab } from './tabs/trip-alerts-tab/trip-alerts-tab';
import { TripReviewsTab } from './tabs/trip-reviews-tab/trip-reviews-tab';

interface TabInfo {
  id: string;
  label: string;
}

const TABS: TabInfo[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'members', label: 'Members' },
  { id: 'travel', label: 'Travel' },
  { id: 'accommodation', label: 'Accommodation' },
  { id: 'expenses', label: 'Expenses' },
  { id: 'itinerary', label: 'Itinerary' },
  { id: 'alerts', label: 'Alerts' },
  { id: 'reviews', label: 'Reviews' },
];

@Component({
  selector: 'app-trip-detail',
  imports: [
    RouterLink,
    NgIcon,
    HlmButtonImports,
    HlmBadgeImports,
    HlmTabsImports,
    StatusBadge,
    TripOverviewTab,
    TripMembersTab,
    TripTravelTab,
    TripAccommodationTab,
    TripExpensesTab,
    TripItineraryTab,
    TripAlertsTab,
    TripReviewsTab,
  ],
  templateUrl: './trip-detail.html',
})
export class TripDetail {
  private readonly route = inject(ActivatedRoute);

  protected readonly tabs = TABS;
  protected readonly activeTab = signal('overview');

  private readonly tripId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('tripId'))),
    { initialValue: null },
  );

  public readonly trip = computed(() => trips.find((t) => t.id === this.tripId()) ?? trips[0]);
  public readonly totalBudget = computed(() => this.trip().budgetPerPerson * this.trip().members);
  public readonly pct = computed(() =>
    Math.round((this.trip().currentCost / this.totalBudget()) * 100),
  );
}
```

(Note: `TABS_WITH_REAL_CONTENT` and `placeholderTabs` are gone — every tab now has a named content block, so the dead-code filtering mechanism is removed.)

Replace the tab-content section of `src/app/features/trips/components/trip-detail/trip-detail.html` (everything from the `overview` content div through the end of the `hlmTabs` closing tag) with:

```html
  <div [hlmTabsContent]="'overview'" class="mt-6">
    <app-trip-overview-tab [trip]="trip()" [totalBudget]="totalBudget()" [pct]="pct()" />
  </div>

  <div [hlmTabsContent]="'members'" class="mt-6">
    <app-trip-members-tab />
  </div>

  <div [hlmTabsContent]="'travel'" class="mt-6">
    <app-trip-travel-tab [trip]="trip()" />
  </div>

  <div [hlmTabsContent]="'accommodation'" class="mt-6">
    <app-trip-accommodation-tab />
  </div>

  <div [hlmTabsContent]="'expenses'" class="mt-6">
    <app-trip-expenses-tab />
  </div>

  <div [hlmTabsContent]="'itinerary'" class="mt-6">
    <app-trip-itinerary-tab />
  </div>

  <div [hlmTabsContent]="'alerts'" class="mt-6">
    <app-trip-alerts-tab />
  </div>

  <div [hlmTabsContent]="'reviews'" class="mt-6">
    <app-trip-reviews-tab />
  </div>
</div>
```

(Leave the back-link, hero header, and `hlmTabsList`/`hlmTabsTrigger` bar above this section untouched.)

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: PASS (all 5 tests)

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
curl -s "http://localhost:4200/trips/goa-2026" -o /tmp/trip-detail-iar-check.html
echo "Day-wise Itinerary heading: $(grep -c 'Day-wise Itinerary' /tmp/trip-detail-iar-check.html)"
echo "Available Activities heading: $(grep -c 'Available Activities' /tmp/trip-detail-iar-check.html)"
echo "An alert title (Bus Delayed by 180 Minutes): $(grep -c 'Bus Delayed by 180 Minutes' /tmp/trip-detail-iar-check.html)"
echo "Rate the Bus review card: $(grep -c 'Rate the Bus' /tmp/trip-detail-iar-check.html)"
echo "No coming-soon placeholder left: $(grep -c 'This section is coming soon.' /tmp/trip-detail-iar-check.html)"
```

Expected: the first four lines report a count of at least 1; the last line reports exactly `0`.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).

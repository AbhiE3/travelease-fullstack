# Trip Detail — Accommodation + Expenses Tabs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the `TripDetail` shell's `accommodation` and `expenses` tabs real content, dropping the "coming soon" placeholder count from 5 to 3.

**Architecture:** `TripAccommodationTab` and `TripExpensesTab` are independent presentational components taking no inputs (both `hotels` and `expenses`/`members` are global mock data), hosted by `TripDetail` in place of two more placeholder slots. Both reuse the `Dialog`/`Select` patterns already verified in prior Trip Detail sub-projects — no new API verification needed.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide` (no new icons needed — `Star`/`MapPin`/`Wallet`/`Plus` are all already registered), spartan-ng `Card`/`Button`/`Badge`/`Dialog`/`Input`/`Label`/`Select` (all already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Hardcoded content (Settlement Summary amounts, Pending Settlements list, search-form defaults) stays hardcoded, matching the React source — not derived from data.
- No new icons need registering in `app.config.ts` this time.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.
- Import alias `@app/*` → `src/app/*`.

---

### Task 1: Build `TripAccommodationTab`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.spec.ts`

**Interfaces:**
- Consumes: `hotels` from `@app/core/mock-data`; `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports`/`HlmInputImports`/`HlmLabelImports`/`HlmSelectImports` (spartan-ng); `NgIcon`.
- Produces: `TripAccommodationTab` (standalone component, no inputs), importable from `@app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab`. Consumed by Task 3.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucideStar } from '@ng-icons/lucide';
import { hotels } from '@app/core/mock-data';
import { TripAccommodationTab } from '@app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab';

describe('TripAccommodationTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripAccommodationTab],
      providers: [provideIcons({ lucideMapPin, lucideStar })],
    }).compileComponents();
  });

  it('renders every hotel from mock data', () => {
    const fixture = TestBed.createComponent(TripAccommodationTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of hotels) {
      expect(text).toContain(h.name);
    }
  });

  it('shows the Best Match badge only for the first hotel', () => {
    const fixture = TestBed.createComponent(TripAccommodationTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const count = (text.match(/Best Match/g) ?? []).length;
    expect(count).toBe(1);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-accommodation-tab` not found).

- [ ] **Step 3: Implement `TripAccommodationTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-trip-accommodation-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
  ],
  templateUrl: './trip-accommodation-tab.html',
})
export class TripAccommodationTab {
  public readonly hotels = hotels;
  protected readonly areas = ['Baga Beach', 'Calangute', 'Vagator', 'Candolim'];
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.html`:

```html
<div hlmCard>
  <div hlmCardContent class="pt-5 grid md:grid-cols-5 gap-3">
    <div class="space-y-1.5">
      <label hlmLabel>Area</label>
      <hlm-select [value]="'Baga Beach'">
        <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
        <ng-template hlmSelectPortal>
          <hlm-select-content>
            @for (a of areas; track a) {
              <hlm-select-item [value]="a">{{ a }}</hlm-select-item>
            }
          </hlm-select-content>
        </ng-template>
      </hlm-select>
    </div>
    <div class="space-y-1.5">
      <label hlmLabel for="budget">Budget</label>
      <input hlmInput id="budget" value="₹3000–6000" />
    </div>
    <div class="space-y-1.5">
      <label hlmLabel for="capacity">Capacity</label>
      <input hlmInput id="capacity" value="6" />
    </div>
    <div class="space-y-1.5">
      <label hlmLabel for="min-rating">Min Rating</label>
      <input hlmInput id="min-rating" value="4.0" />
    </div>
    <div class="flex items-end"><button hlmBtn class="w-full">Search</button></div>
  </div>
</div>

<div class="grid md:grid-cols-2 lg:grid-cols-3 gap-5 mt-6">
  @for (h of hotels; track h.id; let first = $first) {
    <div hlmCard class="overflow-hidden">
      <div class="relative h-40">
        <img [src]="h.image" alt="" class="h-full w-full object-cover" />
        @if (first) {
          <span hlmBadge class="absolute top-3 left-3 bg-accent text-accent-foreground border-0">Best Match</span>
        }
      </div>
      <div hlmCardContent class="pt-4 space-y-2">
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold">{{ h.name }}</h3>
            <p class="text-xs text-muted-foreground">
              <ng-icon name="lucideMapPin" class="inline h-3 w-3" /> {{ h.area }} · {{ h.distance }}
            </p>
          </div>
          <span class="text-xs flex items-center gap-1">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ h.rating }}
          </span>
        </div>
        <div class="flex justify-between items-center text-xs text-muted-foreground">
          <span>Capacity {{ h.capacity }}</span><span>{{ h.rooms }} rooms left</span>
        </div>
        <div class="flex justify-between items-center pt-2 border-t">
          <p class="text-lg font-semibold">
            ₹{{ h.price }}<span class="text-xs text-muted-foreground font-normal">/night</span>
          </p>
          <div class="flex gap-2">
            <button hlmBtn variant="outline" size="sm">Details</button>
            <button hlmBtn size="sm">Select</button>
          </div>
        </div>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 2: Build `TripExpensesTab`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.spec.ts`

**Interfaces:**
- Consumes: `expenses`/`members` from `@app/core/mock-data`; `HlmCardImports`/`HlmButtonImports`/`HlmDialogImports`/`HlmInputImports`/`HlmLabelImports`/`HlmSelectImports` (spartan-ng); `StatusBadge`; `NgIcon`.
- Produces: `TripExpensesTab` (standalone component, no inputs), importable from `@app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab`. Consumed by Task 3.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { expenses } from '@app/core/mock-data';
import { TripExpensesTab } from '@app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab';

describe('TripExpensesTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripExpensesTab],
      providers: [provideIcons({ lucidePlus, lucideWallet })],
    }).compileComponents();
  });

  it('renders every expense with the correct split-with count', () => {
    const fixture = TestBed.createComponent(TripExpensesTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const e of expenses) {
      expect(text).toContain(e.name);
      expect(text).toContain(`split with ${e.participants.length}`);
    }
  });

  it('renders the hardcoded settlement summary amounts', () => {
    const fixture = TestBed.createComponent(TripExpensesTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('₹2,300');
    expect(text).toContain('₹5,400');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-expenses-tab` not found).

- [ ] **Step 3: Implement `TripExpensesTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { expenses, members } from '@app/core/mock-data';

interface Settlement {
  from: string;
  to: string;
  amount: number;
}

const PENDING_SETTLEMENTS: Settlement[] = [
  { from: 'Priya', to: 'You', amount: 2700 },
  { from: 'Neha', to: 'You', amount: 2700 },
  { from: 'You', to: 'Raj', amount: 1400 },
];

@Component({
  selector: 'app-trip-expenses-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
    StatusBadge,
  ],
  templateUrl: './trip-expenses-tab.html',
})
export class TripExpensesTab {
  public readonly expenses = expenses;
  protected readonly members = members;
  protected readonly defaultPaidBy = members[0].name;
  protected readonly pendingSettlements = PENDING_SETTLEMENTS;
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.html`:

```html
<div class="grid lg:grid-cols-3 gap-6">
  <div class="lg:col-span-2 space-y-6">
    <div hlmCard>
      <div hlmCardHeader class="flex flex-row items-center justify-between">
        <h3 hlmCardTitle>Expenses</h3>
        <hlm-dialog>
          <button hlmDialogTrigger hlmBtn size="sm">
            <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Expense
          </button>
          <ng-template hlmDialogPortal>
            <hlm-dialog-content>
              <div hlmDialogHeader>
                <h3 hlmDialogTitle>New Expense</h3>
              </div>
              <div class="space-y-3">
                <div class="space-y-2">
                  <label hlmLabel for="expense-name">Expense Name</label>
                  <input hlmInput id="expense-name" placeholder="Water Sports" />
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <div class="space-y-2">
                    <label hlmLabel for="expense-amount">Amount</label>
                    <input hlmInput id="expense-amount" type="number" placeholder="3000" />
                  </div>
                  <div class="space-y-2">
                    <label hlmLabel>Paid By</label>
                    <hlm-select [value]="defaultPaidBy">
                      <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
                      <ng-template hlmSelectPortal>
                        <hlm-select-content>
                          @for (m of members; track m.id) {
                            <hlm-select-item [value]="m.name">{{ m.name }}</hlm-select-item>
                          }
                        </hlm-select-content>
                      </ng-template>
                    </hlm-select>
                  </div>
                </div>
                <div class="space-y-2">
                  <label hlmLabel for="participants">Participants</label>
                  <input hlmInput id="participants" placeholder="Raj, Arun, Sarathy" />
                </div>
              </div>
              <div hlmDialogFooter>
                <button hlmBtn>Save Expense</button>
              </div>
            </hlm-dialog-content>
          </ng-template>
        </hlm-dialog>
      </div>
      <div hlmCardContent class="space-y-2">
        @for (e of expenses; track e.id) {
          <div class="flex items-center gap-4 p-3 rounded-lg border">
            <div class="h-10 w-10 rounded-md bg-primary/10 text-primary grid place-items-center">
              <ng-icon name="lucideWallet" class="h-5 w-5" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium">{{ e.name }}</p>
              <p class="text-xs text-muted-foreground">
                Paid by {{ e.paidBy }} · split with {{ e.participants.length }}
              </p>
            </div>
            <div class="text-right">
              <p class="font-semibold tabular-nums">₹{{ e.amount.toLocaleString() }}</p>
              <app-status-badge [status]="e.status" />
            </div>
          </div>
        }
      </div>
    </div>
  </div>

  <div class="space-y-4">
    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle class="text-base">Settlement Summary</h3></div>
      <div hlmCardContent class="space-y-3">
        <div class="p-3 rounded-md bg-destructive/10">
          <p class="text-xs text-muted-foreground">You owe</p>
          <p class="text-xl font-semibold text-destructive">₹2,300</p>
          <p class="text-xs">to Raj and Arun</p>
        </div>
        <div class="p-3 rounded-md bg-success/10">
          <p class="text-xs text-muted-foreground">You'll receive</p>
          <p class="text-xl font-semibold text-success">₹5,400</p>
          <p class="text-xs">from Priya and Neha</p>
        </div>
      </div>
    </div>
    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle class="text-base">Pending Settlements</h3></div>
      <div hlmCardContent class="space-y-2">
        @for (s of pendingSettlements; track s.from + s.to) {
          <div class="flex items-center justify-between text-sm py-1.5">
            <span>{{ s.from }} → {{ s.to }}</span>
            <div class="flex items-center gap-2">
              <span class="font-medium tabular-nums">₹{{ s.amount }}</span>
              <button hlmBtn size="sm" variant="outline">Mark Paid</button>
            </div>
          </div>
        }
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 3: Host `TripAccommodationTab` and `TripExpensesTab` in the `TripDetail` shell

**Files:**
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.ts`
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.html`
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`

**Interfaces:**
- Consumes: `TripAccommodationTab` (Task 1), `TripExpensesTab` (Task 2).
- Produces: `TripDetail`'s `accommodation` and `expenses` tabs now show real content; the "coming soon" placeholder loop covers only the final 3 tabs (`itinerary`, `alerts`, `reviews`).

- [ ] **Step 1: Update the failing test**

In `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`, find the test named `'shows the coming-soon placeholder for only 5 tabs now that members and travel have real content'` and replace it with:

```ts
  it('shows the coming-soon placeholder for only 3 tabs now that accommodation and expenses have real content too', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const comingSoonCount = (text.match(/This section is coming soon\./g) ?? []).length;
    expect(comingSoonCount).toBe(3);
  });
```

(No new icons are needed in this file's `ALL_ICONS` — `lucideMapPin`, `lucideStar`, `lucidePlus`, and `lucideWallet` are already present from prior tasks.)

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: FAIL — currently 5 tabs show the placeholder, not 3.

- [ ] **Step 3: Update `TripDetail` to host the two new tabs**

In `src/app/features/trips/components/trip-detail/trip-detail.ts`, add the two new imports and expand `TABS_WITH_REAL_CONTENT`:

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

const TABS_WITH_REAL_CONTENT = new Set([
  'overview',
  'members',
  'travel',
  'accommodation',
  'expenses',
]);

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

  protected readonly placeholderTabs = computed(() =>
    this.tabs.filter((t) => !TABS_WITH_REAL_CONTENT.has(t.id)),
  );
}
```

In `src/app/features/trips/components/trip-detail/trip-detail.html`, insert the two new tab-content divs right after the `travel` one (before the `@for (t of placeholderTabs(); ...)` loop):

```html
  <div [hlmTabsContent]="'accommodation'" class="mt-6">
    <app-trip-accommodation-tab />
  </div>

  <div [hlmTabsContent]="'expenses'" class="mt-6">
    <app-trip-expenses-tab />
  </div>
```

(The full tab-content section should now read: `overview` → `members` → `travel` → `accommodation` → `expenses` → the `@for` placeholder loop, in that order. Leave everything above this section — the back-link, hero, and tab trigger bar — untouched.)

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: PASS (all 5 tests)

---

### Task 4: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–3.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the 3 new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
curl -s "http://localhost:4200/trips/goa-2026" -o /tmp/trip-detail-ae-check.html
echo "A hotel name (Sea Breeze Resort): $(grep -c 'Sea Breeze Resort' /tmp/trip-detail-ae-check.html)"
echo "Best Match badge count (expect 1): $(grep -o 'Best Match' /tmp/trip-detail-ae-check.html | wc -l | tr -d ' ')"
echo "Settlement Summary heading: $(grep -c 'Settlement Summary' /tmp/trip-detail-ae-check.html)"
echo "An expense name (Water Sports): $(grep -c 'Water Sports' /tmp/trip-detail-ae-check.html)"
echo "Coming-soon placeholder count (expect 3): $(grep -o 'This section is coming soon.' /tmp/trip-detail-ae-check.html | wc -l | tr -d ' ')"
```

Expected: every heading/content line reports a count of at least 1; the "Best Match" line reports exactly `1`; the last line reports exactly `3`.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).

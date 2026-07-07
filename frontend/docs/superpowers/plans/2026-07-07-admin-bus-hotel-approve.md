# Admin Bus/Hotel Per-Item Approve Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local, per-item Pending→Accepted approval toggle to `AdminBuses` and `AdminHotels` — every bus/hotel starts Pending (shows an "Approve" button); clicking it flips that item to "Accepted" (shows a `StatusBadge`).

**Architecture:** Each component gets a `signal<ReadonlySet<string>>` of approved ids and an `approve(id)` method. The template conditionally renders "Approve" or an "Accepted" `StatusBadge` per item based on membership in that set. Purely local UI state — no persistence, no connection to any other page.

**Tech Stack:** Angular 21.2 (standalone, signals), existing `StatusBadge` shared component (already supports `Accepted`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- This is new functionality (not a React-source port) — the "Approve" click handler is genuinely functional, unlike most other buttons in this app.
- Approval state is local component state only — resets on page reload, no connection to `AdminApprovals` or any Partner dashboard.
- No new icons, no `StatusBadge` changes (`Accepted` already exists).
- The existing bus On Time/Delayed status column and hotel Edit/Manage buttons are untouched.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Add per-bus approve to `AdminBuses`

**Files:**
- Modify: `src/app/features/admin/components/admin-buses/admin-buses.ts`
- Modify: `src/app/features/admin/components/admin-buses/admin-buses.html`
- Modify: `src/app/features/admin/components/admin-buses/admin-buses.spec.ts`

**Interfaces:**
- Consumes: `StatusBadge` (already supports `'Accepted'`).
- Produces: `AdminBuses` gains a public `approvedIds: Signal<ReadonlySet<string>>` and a public `approve(id: string): void` method.

- [ ] **Step 1: Add the failing tests**

In `src/app/features/admin/components/admin-buses/admin-buses.spec.ts`, add these two tests inside the existing `describe('AdminBuses', ...)` block (after the `'shows the Add Bus dialog trigger'` test):

```ts
  it('shows every bus as pending (Approve button) by default', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(buses.length);
  });

  it('approves only the clicked bus, leaving the others pending', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[];
    const firstApprove = buttons.find((b) => b.textContent?.trim() === 'Approve')!;
    firstApprove.click();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Accepted');
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(buses.length - 1);
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-buses/admin-buses.spec.ts' --watch=false`
Expected: FAIL — no "Approve" text exists anywhere yet.

- [ ] **Step 3: Update `AdminBuses`**

In `src/app/features/admin/components/admin-buses/admin-buses.ts`, add the `signal` import, the `StatusBadge` import, and the approval state/method:

```ts
import { Component, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { buses } from '@app/core/mock-data';

const STATUS_BY_INDEX = ['On Time', 'Delayed', 'On Time'];

export function busStatus(i: number): string {
  return STATUS_BY_INDEX[i] ?? 'On Time';
}

export function busStatusClass(status: string): string {
  return status === 'Delayed'
    ? 'bg-destructive/15 text-destructive border-destructive/20'
    : 'bg-success/15 text-success border-success/20';
}

interface BusRow {
  id: string;
  name: string;
  operator: string;
  seats: number;
  price: number;
  rating: number;
  status: string;
  statusClass: string;
}

@Component({
  selector: 'app-admin-buses',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-buses.html',
})
export class AdminBuses {
  public readonly rows: BusRow[] = buses.map((b, i) => {
    const status = busStatus(i);
    return {
      id: b.id,
      name: b.name,
      operator: b.operator,
      seats: b.seats,
      price: b.price,
      rating: b.rating,
      status,
      statusClass: busStatusClass(status),
    };
  });

  public readonly approvedIds = signal<ReadonlySet<string>>(new Set());

  public approve(id: string): void {
    this.approvedIds.update((ids) => new Set(ids).add(id));
  }
}
```

In `src/app/features/admin/components/admin-buses/admin-buses.html`, replace the table header row and the per-row template:

```html
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-3">Bus</div>
        <div class="col-span-2">Operator</div>
        <div class="col-span-1">Route</div>
        <div class="col-span-1">Seats</div>
        <div class="col-span-1">Price</div>
        <div class="col-span-1">Rating</div>
        <div class="col-span-2">Status</div>
        <div class="col-span-1">Approval</div>
      </div>
      @for (b of rows; track b.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-3"><p class="font-medium">{{ b.name }}</p></div>
          <div class="col-span-2">{{ b.operator }}</div>
          <div class="col-span-1 text-xs">Bengaluru → Goa</div>
          <div class="col-span-1">{{ b.seats }}</div>
          <div class="col-span-1">₹{{ b.price }}</div>
          <div class="col-span-1 flex items-center gap-1">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ b.rating }}
          </div>
          <div class="col-span-2">
            <span hlmBadge variant="outline" [class]="b.statusClass">{{ b.status }}</span>
          </div>
          <div class="col-span-1">
            @if (approvedIds().has(b.id)) {
              <app-status-badge status="Accepted" />
            } @else {
              <button hlmBtn size="sm" (click)="approve(b.id)">Approve</button>
            }
          </div>
        </div>
      }
```

(The column spans now total `3+2+1+1+1+1+2+1 = 12`, still filling the 12-column grid.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-buses/admin-buses.spec.ts' --watch=false`
Expected: PASS (6 tests)

---

### Task 2: Add per-hotel approve to `AdminHotels`

**Files:**
- Modify: `src/app/features/admin/components/admin-hotels/admin-hotels.ts`
- Modify: `src/app/features/admin/components/admin-hotels/admin-hotels.html`
- Modify: `src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts`

**Interfaces:**
- Consumes: `StatusBadge` (already supports `'Accepted'`).
- Produces: `AdminHotels` gains a public `approvedIds: Signal<ReadonlySet<string>>` and a public `approve(id: string): void` method.

- [ ] **Step 1: Add the failing tests**

In `src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts`, add these two tests inside the existing `describe('AdminHotels', ...)` block (after the `'shows the Add Hotel dialog trigger'` test):

```ts
  it('shows every hotel as pending (Approve button) by default', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(hotels.length);
  });

  it('approves only the clicked hotel, leaving the others pending', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[];
    const firstApprove = buttons.find((b) => b.textContent?.trim() === 'Approve')!;
    firstApprove.click();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Accepted');
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(hotels.length - 1);
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts' --watch=false`
Expected: FAIL — no "Approve" text exists anywhere yet.

- [ ] **Step 3: Update `AdminHotels`**

Replace the contents of `src/app/features/admin/components/admin-hotels/admin-hotels.ts`:

```ts
import { Component, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-hotels',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-hotels.html',
})
export class AdminHotels {
  public readonly hotels = hotels;

  public readonly approvedIds = signal<ReadonlySet<string>>(new Set());

  public approve(id: string): void {
    this.approvedIds.update((ids) => new Set(ids).add(id));
  }
}
```

In `src/app/features/admin/components/admin-hotels/admin-hotels.html`, insert a new approval row above the existing Edit/Manage row:

```html
        <div class="pt-2">
          @if (approvedIds().has(h.id)) {
            <app-status-badge status="Accepted" />
          } @else {
            <button hlmBtn size="sm" class="w-full" (click)="approve(h.id)">Approve</button>
          }
        </div>
        <div class="flex gap-2 pt-2">
          <button hlmBtn variant="outline" size="sm" class="flex-1">Edit</button>
          <button hlmBtn size="sm" class="flex-1">Manage</button>
        </div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts' --watch=false`
Expected: PASS (4 tests)

---

### Task 3: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–2.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all pre-existing tests still pass, plus the new `admin-buses.spec.ts`/`admin-hotels.spec.ts` tests.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the check below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

Visit `http://localhost:4200/admin/buses` and `http://localhost:4200/admin/hotels` in a browser and confirm:
- Every bus row and every hotel card shows an "Approve" button.
- Clicking one item's "Approve" button changes only that item to an "Accepted" badge; the rest stay "Approve".

This is a manual interaction check — `curl` cannot click a button or observe the resulting DOM change, so this step cannot be fully automated; the behavior itself is already covered by the unit tests in Tasks 1–2.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

# Login + Register Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/login` and `/register` with real pages ported 1:1 from the React source, including their genuine navigate-on-submit behavior.

**Architecture:** Two independent standalone components under `features/auth/components/`, wired into the existing `auth.routes.ts`'s `'login'` and `'register'` children. Each renders its own title/subtitle/footer markup directly (since `AuthLayout` is a routed shell, not a content-projection wrapper), and injects Angular's `Router` to perform real navigation on submit/click — the one deliberate exception to the "non-functional button" pattern used throughout this migration, because it's what React itself does here.

**Tech Stack:** Angular 21.2 (standalone, signals), spartan-ng `Button`/`Input`/`Label` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- `AuthLayout` itself is not modified — it stays a routed shell (`<router-outlet/>`); each page renders its own title/subtitle/footer.
- Both forms use a native `(submit)` event with `event.preventDefault()` rather than Angular's `(ngSubmit)`, to avoid pulling in `FormsModule` for a case with no `ngModel` bindings — this mirrors React's own `onSubmit={(e) => { e.preventDefault(); ... }}` pattern exactly.
- The "Forgot password?" link on Login points to `/login` itself (a real no-op in React, kept verbatim) — not "fixed" to go anywhere more useful.
- No new icons, no new spartan-ng components.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `Login`

**Files:**
- Create: `src/app/features/auth/components/login/login.ts`
- Create: `src/app/features/auth/components/login/login.html`
- Test: `src/app/features/auth/components/login/login.spec.ts`

**Interfaces:**
- Consumes: Angular `Router` (injected); `RouterLink`; `HlmButtonImports`/`HlmInputImports`/`HlmLabelImports` (spartan-ng).
- Produces: `Login` (standalone component, no inputs), importable from `@app/features/auth/components/login/login`. Consumed by Task 3's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/auth/components/login/login.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Login } from '@app/features/auth/components/login/login';

describe('Login', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(Login);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigateSpy };
  }

  it('renders the prefilled email and password values', async () => {
    const { fixture } = await setup();
    const inputs = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('input'),
    ) as HTMLInputElement[];
    expect(inputs[0].value).toBe('sarathy@example.com');
    expect(inputs[1].value).toBe('password');
  });

  it('navigates to /dashboard when the form is submitted', async () => {
    const { fixture, navigateSpy } = await setup();
    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('navigates to the correct role dashboard for each enter-as button', async () => {
    const { fixture, navigateSpy } = await setup();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button[type="button"]'),
    ) as HTMLButtonElement[];
    const expectedPaths = ['/admin', '/hotel', '/transport', '/activity'];
    buttons.forEach((btn, i) => {
      btn.click();
      expect(navigateSpy).toHaveBeenCalledWith([expectedPaths[i]]);
    });
  });

  it('points the footer link to /register', async () => {
    const { fixture } = await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/register"]');
    expect(link).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/auth/components/login/login.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`login` not found).

- [ ] **Step 3: Implement `Login`**

Create `src/app/features/auth/components/login/login.ts`:

```ts
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';

@Component({
  selector: 'app-login',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './login.html',
})
export class Login {
  private readonly router = inject(Router);

  protected readonly email = 'sarathy@example.com';
  protected readonly password = 'password';

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.router.navigate(['/dashboard']);
  }

  protected enterAs(path: string): void {
    this.router.navigate([path]);
  }
}
```

Create `src/app/features/auth/components/login/login.html`:

```html
<h1 class="text-2xl font-semibold tracking-tight">Welcome back</h1>
<p class="text-sm text-muted-foreground mt-1 mb-6">Sign in to manage your trips and travel plans.</p>

<form class="space-y-4" (submit)="onSubmit($event)">
  <div class="space-y-2">
    <label hlmLabel for="email">Email</label>
    <input hlmInput id="email" type="email" placeholder="you@example.com" [value]="email" />
  </div>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <label hlmLabel for="password">Password</label>
      <a routerLink="/login" class="text-xs text-primary">Forgot password?</a>
    </div>
    <input hlmInput id="password" type="password" placeholder="••••••••" [value]="password" />
  </div>
  <button hlmBtn type="submit" class="w-full">Sign in</button>
  <div class="relative my-2">
    <div class="absolute inset-0 flex items-center"><span class="w-full border-t"></span></div>
    <div class="relative flex justify-center text-[11px] uppercase tracking-wide">
      <span class="bg-card px-2 text-muted-foreground">Or enter as</span>
    </div>
  </div>
  <div class="grid grid-cols-2 gap-2">
    <button hlmBtn type="button" variant="outline" size="sm" (click)="enterAs('/admin')">Admin</button>
    <button hlmBtn type="button" variant="outline" size="sm" (click)="enterAs('/hotel')">
      Hotel Partner
    </button>
    <button hlmBtn type="button" variant="outline" size="sm" (click)="enterAs('/transport')">
      Transport Partner
    </button>
    <button hlmBtn type="button" variant="outline" size="sm" (click)="enterAs('/activity')">
      Activity Provider
    </button>
  </div>
</form>

<p class="text-sm text-muted-foreground mt-6 text-center">
  Don't have an account? <a routerLink="/register" class="text-primary font-medium">Sign up</a>
</p>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/auth/components/login/login.spec.ts' --watch=false`
Expected: PASS (4 tests)

---

### Task 2: Build `Register`

**Files:**
- Create: `src/app/features/auth/components/register/register.ts`
- Create: `src/app/features/auth/components/register/register.html`
- Test: `src/app/features/auth/components/register/register.spec.ts`

**Interfaces:**
- Consumes: Angular `Router` (injected); `RouterLink`; `HlmButtonImports`/`HlmInputImports`/`HlmLabelImports` (spartan-ng).
- Produces: `Register` (standalone component, no inputs), importable from `@app/features/auth/components/register/register`. Consumed by Task 3's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/auth/components/register/register.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Register } from '@app/features/auth/components/register/register';

describe('Register', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(Register);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigateSpy };
  }

  it('renders all 5 fields empty', async () => {
    const { fixture } = await setup();
    const inputs = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('input'),
    ) as HTMLInputElement[];
    expect(inputs).toHaveLength(5);
    for (const input of inputs) {
      expect(input.value).toBe('');
    }
  });

  it('navigates to /dashboard when the form is submitted', async () => {
    const { fixture, navigateSpy } = await setup();
    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('points the footer link to /login', async () => {
    const { fixture } = await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/login"]');
    expect(link).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/auth/components/register/register.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`register` not found).

- [ ] **Step 3: Implement `Register`**

Create `src/app/features/auth/components/register/register.ts`:

```ts
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';

@Component({
  selector: 'app-register',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './register.html',
})
export class Register {
  private readonly router = inject(Router);

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.router.navigate(['/dashboard']);
  }
}
```

Create `src/app/features/auth/components/register/register.html`:

```html
<h1 class="text-2xl font-semibold tracking-tight">Create your account</h1>
<p class="text-sm text-muted-foreground mt-1 mb-6">Start planning your first group trip in minutes.</p>

<form class="space-y-4" (submit)="onSubmit($event)">
  <div class="grid grid-cols-2 gap-3">
    <div class="space-y-2">
      <label hlmLabel for="name">Name</label>
      <input hlmInput id="name" placeholder="Jane Doe" />
    </div>
    <div class="space-y-2">
      <label hlmLabel for="phone">Phone</label>
      <input hlmInput id="phone" placeholder="+91 9876543210" />
    </div>
  </div>
  <div class="space-y-2">
    <label hlmLabel for="email">Email</label>
    <input hlmInput id="email" type="email" placeholder="you@example.com" />
  </div>
  <div class="space-y-2">
    <label hlmLabel for="password">Password</label>
    <input hlmInput id="password" type="password" placeholder="At least 8 characters" />
  </div>
  <div class="space-y-2">
    <label hlmLabel for="confirm">Confirm password</label>
    <input hlmInput id="confirm" type="password" />
  </div>
  <button hlmBtn type="submit" class="w-full">Create account</button>
</form>

<p class="text-sm text-muted-foreground mt-6 text-center">
  Already have an account? <a routerLink="/login" class="text-primary font-medium">Sign in</a>
</p>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/auth/components/register/register.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 3: Wire `Login` and `Register` into `auth.routes.ts`

**Files:**
- Modify: `src/app/features/auth/auth.routes.ts`
- Modify: `src/app/features/auth/auth.routes.spec.ts`

**Interfaces:**
- Consumes: `Login` (Task 1), `Register` (Task 2).
- Produces: `AUTH_ROUTES`'s `'login'` and `'register'` children now `loadComponent` the real pages instead of `RoutePlaceholder`, with `data: { title }` removed from both.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/auth/auth.routes.spec.ts`:

```ts
import { AuthLayout } from '@app/shared/layout/auth-layout/auth-layout';
import { Login } from '@app/features/auth/components/login/login';
import { Register } from '@app/features/auth/components/register/register';
import { AUTH_ROUTES } from './auth.routes';

describe('AUTH_ROUTES', () => {
  it('wraps login and register in the AuthLayout', async () => {
    expect(AUTH_ROUTES).toHaveLength(1);
    const shellRoute = AUTH_ROUTES[0];
    expect(shellRoute.path).toBe('');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AuthLayout);
  });

  it('defines login and register as children', () => {
    const children = AUTH_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual(['login', 'register']);
  });

  it('lazily loads the real components for login and register', async () => {
    const children = AUTH_ROUTES[0].children ?? [];
    expect(await children[0].loadComponent!()).toBe(Login);
    expect(await children[1].loadComponent!()).toBe(Register);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/auth/auth.routes.spec.ts' --watch=false`
Expected: FAIL — both children still resolve to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `auth.routes.ts`**

Replace the contents of `src/app/features/auth/auth.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/layout/auth-layout/auth-layout').then((m) => m.AuthLayout),
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('@app/features/auth/components/login/login').then((m) => m.Login),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('@app/features/auth/components/register/register').then((m) => m.Register),
      },
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/auth/auth.routes.spec.ts' --watch=false`
Expected: PASS (3 tests)

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
curl -s "http://localhost:4200/login" -o /tmp/login-check.html
curl -s "http://localhost:4200/register" -o /tmp/register-check.html

echo "Login — Welcome back heading: $(grep -c 'Welcome back' /tmp/login-check.html)"
echo "Login — Or enter as divider: $(grep -c 'Or enter as' /tmp/login-check.html)"
echo "Register — Create your account heading: $(grep -c 'Create your account' /tmp/register-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/login-check.html /tmp/register-check.html | wc -l)"
```

Expected: the first three lines report a count of at least 1; the last line reports `0`.

This confirms the pages render — actual client-side navigation (form submit, role-switch buttons) is already covered by the unit tests in Tasks 1–2, since `curl` cannot execute JavaScript navigation.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

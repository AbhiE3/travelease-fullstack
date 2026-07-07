# Login — Temporary Role-Based Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `Login`'s quick-switch "Or enter as" buttons with a real (temporary, hardcoded) username/password gate that routes to the matching role's dashboard.

**Architecture:** A single-file change to `Login`. A local constant array of 5 role credentials and an exported pure `matchRole(username, password)` function live in `login.ts`; the template reads live input values via template reference variables at submit time (no `FormsModule` needed, consistent with the existing `(submit)` + `event.preventDefault()` pattern).

**Tech Stack:** Angular 21.2 (standalone, signals), spartan-ng `Button`/`Input`/`Label` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- This is an explicit temporary mechanism — no session storage, guards, or backend calls. The 5 credentials are: `user`/`user123` → `/dashboard`, `admin`/`admin123` → `/admin`, `hotel`/`hotel123` → `/hotel`, `bus`/`bus123` → `/transport`, `activity`/`activity123` → `/activity`.
- `Register` and the "Forgot password?" link are untouched.
- No new icons, no new spartan-ng components.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Replace `Login`'s quick-switch buttons with role-credential matching

**Files:**
- Modify: `src/app/features/auth/components/login/login.ts`
- Modify: `src/app/features/auth/components/login/login.html`
- Modify: `src/app/features/auth/components/login/login.spec.ts`

**Interfaces:**
- Consumes: Angular `Router` (injected); `RouterLink`; `HlmButtonImports`/`HlmInputImports`/`HlmLabelImports` (spartan-ng).
- Produces: `Login` (standalone component, no inputs, no longer has `enterAs()` or the `email`/`password` prefill fields) and `matchRole(username: string, password: string): string | null` (exported pure function), both importable from `@app/features/auth/components/login/login`.

- [ ] **Step 1: Replace the failing tests**

Replace the contents of `src/app/features/auth/components/login/login.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Login, matchRole } from '@app/features/auth/components/login/login';

describe('matchRole', () => {
  it('returns the correct route for each of the 5 valid credential pairs', () => {
    expect(matchRole('user', 'user123')).toBe('/dashboard');
    expect(matchRole('admin', 'admin123')).toBe('/admin');
    expect(matchRole('hotel', 'hotel123')).toBe('/hotel');
    expect(matchRole('bus', 'bus123')).toBe('/transport');
    expect(matchRole('activity', 'activity123')).toBe('/activity');
  });

  it('returns null for a wrong password on a valid username', () => {
    expect(matchRole('admin', 'wrongpassword')).toBeNull();
  });

  it('returns null for an unknown username', () => {
    expect(matchRole('nope', 'nope123')).toBeNull();
  });
});

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

  function submitWith(el: HTMLElement, username: string, password: string) {
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    inputs[0].value = username;
    inputs[1].value = password;
    const form = el.querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
  }

  it('renders no prefilled values and no quick-switch buttons', async () => {
    const { fixture } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    expect(inputs[0].value).toBe('');
    expect(inputs[1].value).toBe('');
    expect(el.textContent).not.toContain('Or enter as');
    expect(el.querySelectorAll('button[type="button"]')).toHaveLength(0);
  });

  it('navigates to the correct route for each of the 5 valid credential pairs', async () => {
    const { fixture, navigateSpy } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    const pairs: Array<[string, string, string]> = [
      ['user', 'user123', '/dashboard'],
      ['admin', 'admin123', '/admin'],
      ['hotel', 'hotel123', '/hotel'],
      ['bus', 'bus123', '/transport'],
      ['activity', 'activity123', '/activity'],
    ];
    for (const [username, password, route] of pairs) {
      submitWith(el, username, password);
      expect(navigateSpy).toHaveBeenCalledWith([route]);
    }
  });

  it('shows an inline error and does not navigate for an invalid pair', async () => {
    const { fixture, navigateSpy } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    submitWith(el, 'admin', 'wrongpassword');
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Invalid username or password');
  });

  it('clears the error after a subsequent valid submit', async () => {
    const { fixture, navigateSpy } = await setup();
    const el = fixture.nativeElement as HTMLElement;

    submitWith(el, 'admin', 'wrongpassword');
    fixture.detectChanges();
    expect(el.textContent).toContain('Invalid username or password');

    submitWith(el, 'admin', 'admin123');
    fixture.detectChanges();

    expect(el.textContent).not.toContain('Invalid username or password');
    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
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
Expected: FAIL — `matchRole` doesn't exist yet, and the current component still has the old prefilled fields and quick-switch buttons.

- [ ] **Step 3: Update `Login`**

Replace the contents of `src/app/features/auth/components/login/login.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';

interface RoleCredential {
  username: string;
  password: string;
  route: string;
}

const ROLE_CREDENTIALS: RoleCredential[] = [
  { username: 'user', password: 'user123', route: '/dashboard' },
  { username: 'admin', password: 'admin123', route: '/admin' },
  { username: 'hotel', password: 'hotel123', route: '/hotel' },
  { username: 'bus', password: 'bus123', route: '/transport' },
  { username: 'activity', password: 'activity123', route: '/activity' },
];

export function matchRole(username: string, password: string): string | null {
  const match = ROLE_CREDENTIALS.find(
    (c) => c.username === username && c.password === password,
  );
  return match ? match.route : null;
}

@Component({
  selector: 'app-login',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './login.html',
})
export class Login {
  private readonly router = inject(Router);

  protected readonly error = signal<string | null>(null);

  protected onSubmit(event: Event, username: string, password: string): void {
    event.preventDefault();
    const route = matchRole(username, password);
    if (route) {
      this.error.set(null);
      this.router.navigate([route]);
    } else {
      this.error.set('Invalid username or password');
    }
  }
}
```

Replace the contents of `src/app/features/auth/components/login/login.html`:

```html
<h1 class="text-2xl font-semibold tracking-tight">Welcome back</h1>
<p class="text-sm text-muted-foreground mt-1 mb-6">Sign in to manage your trips and travel plans.</p>

<form class="space-y-4" (submit)="onSubmit($event, usernameInput.value, passwordInput.value)">
  <div class="space-y-2">
    <label hlmLabel for="username">Username</label>
    <input hlmInput id="username" type="text" placeholder="e.g. admin" #usernameInput />
  </div>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <label hlmLabel for="password">Password</label>
      <a routerLink="/login" class="text-xs text-primary">Forgot password?</a>
    </div>
    <input hlmInput id="password" type="password" placeholder="••••••••" #passwordInput />
  </div>
  @if (error()) {
    <p class="text-xs text-destructive">{{ error() }}</p>
  }
  <button hlmBtn type="submit" class="w-full">Sign in</button>
</form>

<p class="text-sm text-muted-foreground mt-6 text-center">
  Don't have an account? <a routerLink="/register" class="text-primary font-medium">Sign up</a>
</p>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/auth/components/login/login.spec.ts' --watch=false`
Expected: PASS (8 tests)

---

### Task 2: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: Task 1's `Login`.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all pre-existing tests still pass, plus the updated `login.spec.ts` tests.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the check below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

Visit `http://localhost:4200/login` in a browser and confirm:
- The "Or enter as" section and its 4 buttons are gone.
- The two fields are labeled "Username" and "Password", both empty.
- Entering `admin` / `admin123` and clicking "Sign in" navigates to `/admin`.
- Entering `admin` / `wrongpassword` and clicking "Sign in" shows "Invalid username or password" and stays on `/login`.

This is a manual visual/interaction check — `curl` cannot submit a form or observe client-side navigation, so this step cannot be fully automated; the behavior itself is already covered by the unit tests in Task 1.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.

# Real Role-Based Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the frontend's hardcoded username/password login stub with real backend-integrated login: call `POST /api/auth/login`, store the JWT + role, route to the matching one of 5 dashboards, and guard every portal's routes against cross-role access.

**Architecture:** A new `core/auth` module (`auth.models.ts`, `auth.service.ts`, `auth.interceptor.ts`, `auth.guard.ts`) owns all auth state and logic. `Login` and `AppShell` consume `AuthService`; `app.routes.ts`'s 5 portal shells get `canActivate: [authGuard]`. One deviation from the design doc: instead of Angular's `environment.ts`/`environment.development.ts` file-replacement mechanism (which would require new `angular.json` `fileReplacements` wiring that doesn't exist today), the API base URL is a plain exported constant in `core/auth/api-config.ts` — same effect, no build-config risk, trivial to swap later. On the backend, `DemoDataInitializer` gains one demo user per provider role so all 5 dashboards are testable without a manual DB insert.

**Tech Stack:** Angular 21.2 (standalone, signals), `@angular/common/http` (functional interceptors, `HttpClient`), Vitest (`@angular/build:unit-test`), Spring Boot 3.5.16 / Java 17 on the backend.

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Import alias `@app/*` → `frontend/src/app/*`.
- Frontend test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite. Run from the `frontend/` directory.
- Frontend build command: `npx ng build` — must complete with no errors. Run from `frontend/`.
- Backend test command: `./mvnw test`. Backend run command: `./mvnw spring-boot:run`. Run from the `backend/` directory.
- Backend runs on `http://localhost:8080`; CORS (`app.cors.allowed-origins` in `application.properties`) already allows `http://localhost:4200`, the Angular dev server's default port — no CORS config changes needed.
- The 5 backend role strings and their frontend mapping (do not deviate from this table in any task):

  | Backend role              | Frontend `Role` | Home route   |
  |----------------------------|------------------|--------------|
  | `ROLE_ADMIN`                | `admin`          | `/admin`     |
  | `ROLE_TRAVELER`             | `traveler`       | `/dashboard` |
  | `ROLE_PROVIDER`             | `transport`      | `/transport` |
  | `ROLE_HOTEL_PROVIDER`       | `hotel`          | `/hotel`     |
  | `ROLE_ACTIVITY_PROVIDER`    | `activity`       | `/activity`  |

- Demo user password convention (matches existing `DemoDataInitializer` seed users): `password123`.

---

### Task 1: Seed one demo user per provider role (backend)

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/shared/config/DemoDataInitializer.java`

**Interfaces:**
- Consumes: existing `Role` enum (`ROLE_PROVIDER`, `ROLE_HOTEL_PROVIDER`, `ROLE_ACTIVITY_PROVIDER`), existing `getOrCreateUser(...)` pattern.
- Produces: 3 new demo users reachable via `POST /api/auth/login`, all with `providerId = 1L` (matching the existing `app.scheduler.default-provider-id=1` / seeded `Bus.providerId` convention).

There is no existing test file for `DemoDataInitializer`; this task is verified by starting the app and calling the login endpoint directly (Step 3).

- [ ] **Step 1: Add UUID constants and extend `getOrCreateUser` with a `providerId` parameter**

In `DemoDataInitializer.java`, add three new constants alongside the existing ones (after `CARA_ID`):

```java
    public static final UUID TRANSPORT_PROVIDER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID HOTEL_PROVIDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID ACTIVITY_PROVIDER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
```

Replace the `getOrCreateUser` method:

```java
    private User getOrCreateUser(UUID id, String name, String email, String phone, Role role, Long providerId) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setRole(role);
            user.setProviderId(providerId);
            return userRepository.save(user);
        });
    }
```

- [ ] **Step 2: Update `run()` to seed the 4 existing users with `null` providerId and add the 3 new provider users**

Replace the body of `run(String... args)`:

```java
    @Override
    @Transactional
    public void run(String... args) {
        User admin = getOrCreateUser(ADMIN_ID, "Admin User", "admin@travelease.test", "9000000000", Role.ROLE_ADMIN, null);
        User alice = getOrCreateUser(ALICE_ID, "Alice Traveler", "alice@travelease.test", "9000000001", Role.ROLE_TRAVELER, null);
        User bob = getOrCreateUser(BOB_ID, "Bob Traveler", "bob@travelease.test", "9000000002", Role.ROLE_TRAVELER, null);
        User cara = getOrCreateUser(CARA_ID, "Cara Traveler", "cara@travelease.test", "9000000003", Role.ROLE_TRAVELER, null);
        getOrCreateUser(TRANSPORT_PROVIDER_ID, "Priya Transport Provider", "provider@travelease.test", "9000000004", Role.ROLE_PROVIDER, 1L);
        getOrCreateUser(HOTEL_PROVIDER_ID, "Rahul Hotel Provider", "hotelprovider@travelease.test", "9000000005", Role.ROLE_HOTEL_PROVIDER, 1L);
        getOrCreateUser(ACTIVITY_PROVIDER_ID, "Meera Activity Provider", "activityprovider@travelease.test", "9000000006", Role.ROLE_ACTIVITY_PROVIDER, 1L);

        seedExpenseData(alice, bob, cara);
        seedBusBookingData(admin);
    }
```

- [ ] **Step 3: Verify by starting the backend and logging in as each new demo user**

Run: `./mvnw spring-boot:run` (from `backend/`), wait for `Started BackendApplication`, then in another terminal:

```bash
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"provider@travelease.test","password":"password123"}' | grep -o '"role":"[A-Z_]*"'
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"hotelprovider@travelease.test","password":"password123"}' | grep -o '"role":"[A-Z_]*"'
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"activityprovider@travelease.test","password":"password123"}' | grep -o '"role":"[A-Z_]*"'
```

Expected: `"role":"ROLE_PROVIDER"`, `"role":"ROLE_HOTEL_PROVIDER"`, `"role":"ROLE_ACTIVITY_PROVIDER"` respectively. Stop the backend afterward (`Ctrl+C` in its terminal, or `kill` the process) unless a later task's verification step needs it running — Task 9 needs it running again, so it's fine to leave running if convenient, but do not leave it running unattended at the very end of the plan.

---

### Task 2: `core/auth/auth.models.ts` — shared role types, and de-duplicate `app-shell.ts`

**Files:**
- Create: `frontend/src/app/core/auth/auth.models.ts`
- Create: `frontend/src/app/core/auth/auth.models.spec.ts`
- Modify: `frontend/src/app/shared/layout/app-shell/app-shell.ts`

**Interfaces:**
- Produces: `Role` type (`'traveler' | 'admin' | 'hotel' | 'transport' | 'activity'`), `BACKEND_ROLE_MAP: Record<string, Role>`, `ROLE_HOME: Record<Role, string>`, `StoredUser` interface (`{ id: string; name: string; email: string; role: Role }`) — all importable from `@app/core/auth/auth.models`. Every later task imports `Role`/`ROLE_HOME` from here, never redefines them.
- Consumes: nothing new.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/core/auth/auth.models.spec.ts`:

```ts
import { BACKEND_ROLE_MAP, ROLE_HOME } from '@app/core/auth/auth.models';

describe('BACKEND_ROLE_MAP', () => {
  it('maps exactly the 5 known backend roles to the 5 frontend roles', () => {
    expect(BACKEND_ROLE_MAP).toEqual({
      ROLE_ADMIN: 'admin',
      ROLE_TRAVELER: 'traveler',
      ROLE_PROVIDER: 'transport',
      ROLE_HOTEL_PROVIDER: 'hotel',
      ROLE_ACTIVITY_PROVIDER: 'activity',
    });
  });
});

describe('ROLE_HOME', () => {
  it('maps every frontend role to its dashboard home route', () => {
    expect(ROLE_HOME).toEqual({
      traveler: '/dashboard',
      admin: '/admin',
      hotel: '/hotel',
      transport: '/transport',
      activity: '/activity',
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/core/auth/auth.models.spec.ts' --watch=false`
Expected: FAIL — `@app/core/auth/auth.models` does not exist.

- [ ] **Step 3: Create `auth.models.ts`**

Create `frontend/src/app/core/auth/auth.models.ts`:

```ts
export type Role = 'traveler' | 'admin' | 'hotel' | 'transport' | 'activity';

export const BACKEND_ROLE_MAP: Record<string, Role> = {
  ROLE_ADMIN: 'admin',
  ROLE_TRAVELER: 'traveler',
  ROLE_PROVIDER: 'transport',
  ROLE_HOTEL_PROVIDER: 'hotel',
  ROLE_ACTIVITY_PROVIDER: 'activity',
};

export const ROLE_HOME: Record<Role, string> = {
  traveler: '/dashboard',
  admin: '/admin',
  hotel: '/hotel',
  transport: '/transport',
  activity: '/activity',
};

export interface StoredUser {
  id: string;
  name: string;
  email: string;
  role: Role;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/core/auth/auth.models.spec.ts' --watch=false`
Expected: PASS (2 tests)

- [ ] **Step 5: De-duplicate `app-shell.ts` — import `Role`/`ROLE_HOME` instead of redefining them**

In `frontend/src/app/shared/layout/app-shell/app-shell.ts`, add the import (near the other `@app/*` imports — there are none yet in this file, so add it after the `rxjs` import):

```ts
import { Role, ROLE_HOME } from '@app/core/auth/auth.models';
```

Delete this line (the local `Role` type is now imported instead):

```ts
export type Role = 'traveler' | 'admin' | 'hotel' | 'transport' | 'activity';
```

Delete this block (the local `ROLE_HOME` constant is now imported instead — `NAV_MAP` and `ROLE_LABEL` stay, they're UI-only and not needed elsewhere):

```ts
const ROLE_HOME: Record<Role, string> = {
  traveler: '/dashboard',
  admin: '/admin',
  hotel: '/hotel',
  transport: '/transport',
  activity: '/activity',
};
```

- [ ] **Step 6: Run the existing `app-shell` tests to confirm no regression**

Run: `npx ng test --include='src/app/shared/layout/app-shell/app-shell.spec.ts' --watch=false`
Expected: PASS (all 5 pre-existing tests, unchanged)

---

### Task 3: `core/auth/auth.service.ts` — real login, session persistence

**Files:**
- Create: `frontend/src/app/core/auth/api-config.ts`
- Create: `frontend/src/app/core/auth/auth.service.ts`
- Create: `frontend/src/app/core/auth/auth.service.spec.ts`

**Interfaces:**
- Consumes: `Role`, `BACKEND_ROLE_MAP`, `StoredUser` from `@app/core/auth/auth.models` (Task 2).
- Produces: `AuthService` (`@Injectable({ providedIn: 'root' })`) with:
  - `login(email: string, password: string): Promise<StoredUser>` — rejects with the raw `HttpErrorResponse` on failure.
  - `logout(): void`
  - `getToken(): string | null`
  - `isAuthenticated: Signal<boolean>`
  - `currentUser: Signal<StoredUser | null>`
  - `role: Signal<Role | null>`

  Later tasks (interceptor, guard, `Login`, `AppShell`) depend on these exact names.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/core/auth/auth.service.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { AuthService } from '@app/core/auth/auth.service';

describe('AuthService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  async function setup(platform: 'browser' | 'server' = 'browser') {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PLATFORM_ID, useValue: platform },
      ],
    }).compileComponents();
    return {
      service: TestBed.inject(AuthService),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('starts unauthenticated with no stored session', async () => {
    const { service } = await setup();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.role()).toBeNull();
  });

  it('logs in, maps the backend role, and persists the session', async () => {
    const { service, httpMock } = await setup();

    const loginPromise = service.login('admin@travelease.test', 'password123');

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@travelease.test', password: 'password123' });
    req.flush({
      success: true,
      data: {
        accessToken: 'jwt-token',
        user: {
          id: 'u1',
          name: 'Admin User',
          email: 'admin@travelease.test',
          phone: '9000000000',
          role: 'ROLE_ADMIN',
          providerId: null,
        },
      },
      message: 'Login successful',
      error: null,
    });

    const user = await loginPromise;
    expect(user).toEqual({ id: 'u1', name: 'Admin User', email: 'admin@travelease.test', role: 'admin' });
    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('admin');
    expect(localStorage.getItem('te_access_token')).toBe('jwt-token');
    expect(JSON.parse(localStorage.getItem('te_user')!)).toEqual(user);
  });

  it('rejects and does not persist on a failed login', async () => {
    const { service, httpMock } = await setup();

    const loginPromise = service.login('admin@travelease.test', 'wrongpassword');
    loginPromise.catch(() => {});
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    req.flush(
      {
        success: false,
        data: null,
        message: null,
        error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' },
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    await expect(loginPromise).rejects.toBeTruthy();
    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('te_access_token')).toBeNull();
  });

  it('logout clears the session', async () => {
    const { service, httpMock } = await setup();
    const loginPromise = service.login('admin@travelease.test', 'password123');
    httpMock.expectOne('http://localhost:8080/api/auth/login').flush({
      success: true,
      data: {
        accessToken: 'jwt-token',
        user: { id: 'u1', name: 'Admin', email: 'admin@travelease.test', phone: '900', role: 'ROLE_ADMIN', providerId: null },
      },
      message: 'ok',
      error: null,
    });
    await loginPromise;

    service.logout();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.role()).toBeNull();
    expect(localStorage.getItem('te_access_token')).toBeNull();
    expect(localStorage.getItem('te_user')).toBeNull();
  });

  it('restores a persisted session from localStorage on construction', async () => {
    localStorage.setItem('te_access_token', 'jwt-token');
    localStorage.setItem(
      'te_user',
      JSON.stringify({ id: 'u1', name: 'Admin', email: 'admin@travelease.test', role: 'admin' }),
    );

    const { service } = await setup();
    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('admin');
  });

  it('does not read or write localStorage when run on the server platform', async () => {
    localStorage.setItem('te_access_token', 'jwt-token');
    const { service } = await setup('server');
    expect(service.isAuthenticated()).toBe(false);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/core/auth/auth.service.spec.ts' --watch=false`
Expected: FAIL — `@app/core/auth/auth.service` does not exist.

- [ ] **Step 3: Create `api-config.ts`**

Create `frontend/src/app/core/auth/api-config.ts`:

```ts
export const API_BASE_URL = 'http://localhost:8080';
```

- [ ] **Step 4: Create `auth.service.ts`**

Create `frontend/src/app/core/auth/auth.service.ts`:

```ts
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '@app/core/auth/api-config';
import { BACKEND_ROLE_MAP, StoredUser } from '@app/core/auth/auth.models';

const TOKEN_KEY = 'te_access_token';
const USER_KEY = 'te_user';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: { code: string; message: string } | null;
}

interface LoginResponseDto {
  accessToken: string;
  user: {
    id: string;
    name: string;
    email: string;
    phone: string;
    role: string;
    providerId: number | null;
  };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly tokenSignal = signal<string | null>(this.readToken());
  private readonly userSignal = signal<StoredUser | null>(this.readUser());

  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly currentUser = computed(() => this.userSignal());
  readonly role = computed(() => this.userSignal()?.role ?? null);

  async login(email: string, password: string): Promise<StoredUser> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<LoginResponseDto>>(`${API_BASE_URL}/api/auth/login`, {
        email,
        password,
      }),
    );

    const role = BACKEND_ROLE_MAP[response.data.user.role];
    const user: StoredUser = {
      id: response.data.user.id,
      name: response.data.user.name,
      email: response.data.user.email,
      role,
    };

    this.persist(response.data.accessToken, user);
    return user;
  }

  logout(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    if (this.isBrowser) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  private persist(token: string, user: StoredUser): void {
    this.tokenSignal.set(token);
    this.userSignal.set(user);
    if (this.isBrowser) {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    }
  }

  private readToken(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(TOKEN_KEY);
  }

  private readUser(): StoredUser | null {
    if (!this.isBrowser) {
      return null;
    }
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/core/auth/auth.service.spec.ts' --watch=false`
Expected: PASS (6 tests)

---

### Task 4: `core/auth/auth.interceptor.ts` — attach the bearer token, wire into `app.config.ts`

**Files:**
- Create: `frontend/src/app/core/auth/auth.interceptor.ts`
- Create: `frontend/src/app/core/auth/auth.interceptor.spec.ts`
- Modify: `frontend/src/app/app.config.ts`

**Interfaces:**
- Consumes: `AuthService.getToken()` (Task 3).
- Produces: `authInterceptor: HttpInterceptorFn`, importable from `@app/core/auth/auth.interceptor`. Consumed by `app.config.ts`.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/core/auth/auth.interceptor.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '@app/core/auth/auth.service';
import { authInterceptor } from '@app/core/auth/auth.interceptor';

describe('authInterceptor', () => {
  async function setup(token: string | null) {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => token } },
      ],
    }).compileComponents();
    return {
      http: TestBed.inject(HttpClient),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('attaches the Authorization header when a token is present', async () => {
    const { http, httpMock } = await setup('jwt-token');
    http.get('/api/whatever').subscribe();
    const req = httpMock.expectOne('/api/whatever');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('omits the Authorization header when no token is present', async () => {
    const { http, httpMock } = await setup(null);
    http.get('/api/whatever').subscribe();
    const req = httpMock.expectOne('/api/whatever');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/core/auth/auth.interceptor.spec.ts' --watch=false`
Expected: FAIL — `@app/core/auth/auth.interceptor` does not exist.

- [ ] **Step 3: Create `auth.interceptor.ts`**

Create `frontend/src/app/core/auth/auth.interceptor.ts`:

```ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '@app/core/auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/core/auth/auth.interceptor.spec.ts' --watch=false`
Expected: PASS (2 tests)

- [ ] **Step 5: Wire the interceptor and `HttpClient` into `app.config.ts`**

In `frontend/src/app/app.config.ts`, add these imports:

```ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from '@app/core/auth/auth.interceptor';
```

Add `provideHttpClient(withInterceptors([authInterceptor]))` to the `providers` array (alongside `provideRouter(routes)`, `provideClientHydration(...)`, etc.).

- [ ] **Step 6: Run the full frontend test suite to confirm no regression**

Run: `npx ng test --watch=false`
Expected: PASS — all pre-existing tests plus the new ones from Tasks 2–4.

---

### Task 5: `core/auth/auth.guard.ts` — role-matched route protection

**Files:**
- Create: `frontend/src/app/core/auth/auth.guard.ts`
- Create: `frontend/src/app/core/auth/auth.guard.spec.ts`

**Interfaces:**
- Consumes: `AuthService.isAuthenticated`, `AuthService.role` (Task 3); `ROLE_HOME`, `Role` from `@app/core/auth/auth.models` (Task 2).
- Produces: `authGuard: CanActivateFn`, importable from `@app/core/auth/auth.guard`. Consumed by Task 6.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/core/auth/auth.guard.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '@app/core/auth/auth.service';
import { authGuard } from '@app/core/auth/auth.guard';

function routeWithRole(role: string): ActivatedRouteSnapshot {
  return { data: { role } } as unknown as ActivatedRouteSnapshot;
}

describe('authGuard', () => {
  async function setup(isAuthenticated: boolean, role: string | null) {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => isAuthenticated, role: () => role } },
      ],
    }).compileComponents();
  }

  it('redirects to /login when not authenticated', async () => {
    await setup(false, null);
    const result = TestBed.runInInjectionContext(() =>
      authGuard(routeWithRole('admin'), {} as RouterStateSnapshot),
    );
    expect(result).not.toBe(true);
    expect((result as ReturnType<Router['createUrlTree']>).toString()).toBe('/login');
  });

  it("redirects to the user's own home when the role does not match", async () => {
    await setup(true, 'traveler');
    const result = TestBed.runInInjectionContext(() =>
      authGuard(routeWithRole('admin'), {} as RouterStateSnapshot),
    );
    expect(result).not.toBe(true);
    expect((result as ReturnType<Router['createUrlTree']>).toString()).toBe('/dashboard');
  });

  it('allows activation when the role matches', async () => {
    await setup(true, 'admin');
    const result = TestBed.runInInjectionContext(() =>
      authGuard(routeWithRole('admin'), {} as RouterStateSnapshot),
    );
    expect(result).toBe(true);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/core/auth/auth.guard.spec.ts' --watch=false`
Expected: FAIL — `@app/core/auth/auth.guard` does not exist.

- [ ] **Step 3: Create `auth.guard.ts`**

Create `frontend/src/app/core/auth/auth.guard.ts`:

```ts
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '@app/core/auth/auth.service';
import { ROLE_HOME, Role } from '@app/core/auth/auth.models';

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const requiredRole = route.data['role'] as Role | undefined;
  const currentRole = authService.role();
  if (requiredRole && currentRole && requiredRole !== currentRole) {
    return router.createUrlTree([ROLE_HOME[currentRole]]);
  }

  return true;
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/core/auth/auth.guard.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 6: Guard the 5 portal route trees

**Files:**
- Modify: `frontend/src/app/features/admin/admin.routes.ts`
- Modify: `frontend/src/app/features/traveler/traveler.routes.ts`
- Modify: `frontend/src/app/features/hotel/hotel.routes.ts`
- Modify: `frontend/src/app/features/transport/transport.routes.ts`
- Modify: `frontend/src/app/features/activity/activity.routes.ts`
- Modify: `frontend/src/app/features/admin/admin.routes.spec.ts`
- Modify: `frontend/src/app/features/traveler/traveler.routes.spec.ts`
- Modify: `frontend/src/app/features/hotel/hotel.routes.spec.ts`
- Modify: `frontend/src/app/features/transport/transport.routes.spec.ts`
- Modify: `frontend/src/app/features/activity/activity.routes.spec.ts`

**Interfaces:**
- Consumes: `authGuard` from `@app/core/auth/auth.guard` (Task 5).
- Produces: nothing new — this is the same mechanical one-line addition applied identically to all 5 route files, so it's one task rather than five.

- [ ] **Step 1: Write the failing assertions (one per route spec)**

In each of the 5 `*.routes.spec.ts` files, add `authGuard` to the top import from `@app/core/auth/auth.guard`, and add a new assertion inside the existing "wraps ... in the AppShell" test (right after the `shellRoute.data?.['role']` assertion). Example for `admin.routes.spec.ts` (the other 4 follow the identical pattern with their own role name):

```ts
import { authGuard } from '@app/core/auth/auth.guard';
```

```ts
  it('wraps the admin pages in the AppShell with the admin role', async () => {
    expect(ADMIN_ROUTES).toHaveLength(1);
    const shellRoute = ADMIN_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('admin');
    expect(shellRoute.canActivate).toEqual([authGuard]);
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });
```

Apply the same two changes (import + `canActivate` assertion in the shell-wrapping test) to `traveler.routes.spec.ts`, `hotel.routes.spec.ts`, `transport.routes.spec.ts`, and `activity.routes.spec.ts`.

- [ ] **Step 2: Run the 5 route specs to verify they fail**

Run: `npx ng test --include='src/app/features/{admin,traveler,hotel,transport,activity}/*.routes.spec.ts' --watch=false`
Expected: FAIL — `shellRoute.canActivate` is `undefined` in all 5.

- [ ] **Step 3: Add `canActivate: [authGuard]` to each shell route**

In each of the 5 `*.routes.ts` files, add the import:

```ts
import { authGuard } from '@app/core/auth/auth.guard';
```

And add `canActivate: [authGuard]` to the shell route object (the one with `data: { role: ... }`). Example for `admin.routes.ts`:

```ts
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'admin' },
    canActivate: [authGuard],
    children: [
      // ...unchanged
    ],
  },
];
```

Apply the identical `canActivate: [authGuard]` line (right after each file's own `data: { role: '...' }` line) to `traveler.routes.ts`, `hotel.routes.ts`, `transport.routes.ts`, and `activity.routes.ts`.

- [ ] **Step 4: Run the 5 route specs to verify they pass**

Run: `npx ng test --include='src/app/features/{admin,traveler,hotel,transport,activity}/*.routes.spec.ts' --watch=false`
Expected: PASS (all pre-existing tests plus the new `canActivate` assertion in each)

---

### Task 7: Rewire `Login` to call `AuthService`

**Files:**
- Modify: `frontend/src/app/features/auth/components/login/login.ts`
- Modify: `frontend/src/app/features/auth/components/login/login.html`
- Modify: `frontend/src/app/features/auth/components/login/login.spec.ts`

**Interfaces:**
- Consumes: `AuthService.login()`, `AuthService.isAuthenticated()`, `AuthService.role()` (Task 3); `ROLE_HOME` from `@app/core/auth/auth.models` (Task 2).
- Produces: `Login` component with the same public shape as before (`error: Signal<string | null>`) plus a new `submitting: Signal<boolean>`; the exported `matchRole` function and `ROLE_CREDENTIALS` constant are deleted — nothing downstream consumed them (confirmed: `matchRole` was only imported by `login.spec.ts`, which this task also rewrites).

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/auth/components/login/login.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Login } from '@app/features/auth/components/login/login';
import { AuthService } from '@app/core/auth/auth.service';

describe('Login', () => {
  async function setup(overrides: Partial<AuthService> = {}, authenticated = false) {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => authenticated,
            role: () => (authenticated ? 'admin' : null),
            ...overrides,
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(Login);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigateSpy };
  }

  function submitWith(el: HTMLElement, email: string, password: string) {
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    inputs[0].value = email;
    inputs[1].value = password;
    const form = el.querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
  }

  it('renders empty email and password fields', async () => {
    const { fixture } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    expect(inputs[0].value).toBe('');
    expect(inputs[1].value).toBe('');
  });

  it('logs in and navigates to the mapped role home on success', async () => {
    const login = vi.fn().mockResolvedValue({
      id: '1',
      name: 'Admin',
      email: 'admin@travelease.test',
      role: 'admin',
    });
    const { fixture, navigateSpy } = await setup({ login });
    const el = fixture.nativeElement as HTMLElement;

    submitWith(el, 'admin@travelease.test', 'password123');
    await fixture.whenStable();

    expect(login).toHaveBeenCalledWith('admin@travelease.test', 'password123');
    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('shows the backend error message and does not navigate on a failed login', async () => {
    const httpError = new HttpErrorResponse({
      status: 401,
      error: {
        success: false,
        data: null,
        error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' },
      },
    });
    const login = vi.fn().mockRejectedValue(httpError);
    const { fixture, navigateSpy } = await setup({ login });
    const el = fixture.nativeElement as HTMLElement;

    submitWith(el, 'admin@travelease.test', 'wrongpassword');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Invalid email or password');
  });

  it('redirects immediately to the role home if already authenticated', async () => {
    const { navigateSpy } = await setup({}, true);
    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('points the footer link to /register', async () => {
    const { fixture } = await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/register"]');
    expect(link).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/auth/components/login/login.spec.ts' --watch=false`
Expected: FAIL — `Login` still uses `matchRole`, no `email`/password real submit handling.

- [ ] **Step 3: Replace `login.ts`**

Replace the contents of `frontend/src/app/features/auth/components/login/login.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { AuthService } from '@app/core/auth/auth.service';
import { ROLE_HOME } from '@app/core/auth/auth.models';

@Component({
  selector: 'app-login',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './login.html',
})
export class Login {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  constructor() {
    const role = this.authService.isAuthenticated() ? this.authService.role() : null;
    if (role) {
      this.router.navigate([ROLE_HOME[role]]);
    }
  }

  protected async onSubmit(event: Event, email: string, password: string): Promise<void> {
    event.preventDefault();
    this.error.set(null);
    this.submitting.set(true);
    try {
      const user = await this.authService.login(email, password);
      this.router.navigate([ROLE_HOME[user.role]]);
    } catch (err) {
      this.error.set(
        err instanceof HttpErrorResponse
          ? (err.error?.error?.message ?? 'Invalid email or password')
          : 'Something went wrong. Please try again.',
      );
    } finally {
      this.submitting.set(false);
    }
  }
}
```

- [ ] **Step 4: Replace `login.html`**

Replace the contents of `frontend/src/app/features/auth/components/login/login.html`:

```html
<h1 class="text-2xl font-semibold tracking-tight">Welcome back</h1>
<p class="text-sm text-muted-foreground mt-1 mb-6">Sign in to manage your trips and travel plans.</p>

<form class="space-y-4" (submit)="onSubmit($event, emailInput.value, passwordInput.value)">
  <div class="space-y-2">
    <label hlmLabel for="email">Email</label>
    <input hlmInput id="email" type="email" placeholder="e.g. admin@travelease.test" #emailInput />
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
  <button hlmBtn type="submit" class="w-full" [disabled]="submitting()">Sign in</button>
</form>

<p class="text-sm text-muted-foreground mt-6 text-center">
  Don't have an account? <a routerLink="/register" class="text-primary font-medium">Sign up</a>
</p>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/auth/components/login/login.spec.ts' --watch=false`
Expected: PASS (5 tests)

---

### Task 8: Wire "Sign out" in `AppShell`

**Files:**
- Modify: `frontend/src/app/shared/layout/app-shell/app-shell.ts`
- Modify: `frontend/src/app/shared/layout/app-shell/app-shell.html`
- Modify: `frontend/src/app/shared/layout/app-shell/app-shell.spec.ts`

**Interfaces:**
- Consumes: `AuthService.logout()` (Task 3).
- Produces: `AppShell.signOut(): void` (protected method), called from the template.

- [ ] **Step 1: Update the test file**

In `frontend/src/app/shared/layout/app-shell/app-shell.spec.ts`, add the import:

```ts
import { AuthService } from '@app/core/auth/auth.service';
```

Update `configureWithRole` to accept and provide an `AuthService` stub, and to spy on `Router.navigate`:

```ts
async function configureWithRole(role: string | undefined) {
  await TestBed.configureTestingModule({
    imports: [AppShell],
    providers: [
      provideRouter([]),
      provideIcons(ALL_ICONS),
      { provide: ActivatedRoute, useValue: { data: of(role === undefined ? {} : { role }) } },
      { provide: AuthService, useValue: { logout: vi.fn() } },
    ],
  }).compileComponents();
}
```

Add a new test at the end of the `describe('AppShell', ...)` block:

```ts
  it('calls AuthService.logout() and navigates to /login when Sign out is clicked', async () => {
    await configureWithRole('traveler');
    const fixture = TestBed.createComponent(AppShell);
    const router = TestBed.inject(Router);
    const authService = TestBed.inject(AuthService);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await fixture.whenStable();

    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'button[type="button"]',
    ) as HTMLButtonElement;
    button.click();

    expect(authService.logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
```

This requires importing `Router` in the spec file too — update the existing router import line:

```ts
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/shared/layout/app-shell/app-shell.spec.ts' --watch=false`
Expected: FAIL — `AppShell` doesn't inject `AuthService`, and there's no `button[type="button"]` (the current "Sign out" element is an `<a>`).

- [ ] **Step 3: Update `app-shell.ts`**

In `frontend/src/app/shared/layout/app-shell/app-shell.ts`, update the `@angular/router` import to include `Router`:

```ts
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
```

Add the `AuthService` import (alongside the `Role`/`ROLE_HOME` import added in Task 2):

```ts
import { AuthService } from '@app/core/auth/auth.service';
```

In the `AppShell` class, add the injections and the `signOut` method:

```ts
export class AppShell {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly role = toSignal(
    this.route.data.pipe(map((data) => (data['role'] as Role | undefined) ?? 'traveler')),
    { initialValue: 'traveler' as Role },
  );

  protected readonly nav = computed(() => NAV_MAP[this.role()]);
  protected readonly roleLabel = computed(() => ROLE_LABEL[this.role()]);
  protected readonly home = computed(() => ROLE_HOME[this.role()]);

  protected signOut(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
```

- [ ] **Step 4: Update `app-shell.html`**

In `frontend/src/app/shared/layout/app-shell/app-shell.html`, replace the "Sign out" `<a>` block:

```html
    <div class="p-3 border-t border-sidebar-border">
      <a
        routerLink="/login"
        class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-sidebar-foreground/75 hover:bg-sidebar-accent/60"
      >
        <ng-icon name="lucideLogOut" class="h-4 w-4" /> Sign out
      </a>
    </div>
```

with:

```html
    <div class="p-3 border-t border-sidebar-border">
      <button
        type="button"
        (click)="signOut()"
        class="w-full flex items-center gap-3 px-3 py-2 rounded-md text-sm text-sidebar-foreground/75 hover:bg-sidebar-accent/60"
      >
        <ng-icon name="lucideLogOut" class="h-4 w-4" /> Sign out
      </button>
    </div>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/shared/layout/app-shell/app-shell.spec.ts' --watch=false`
Expected: PASS (6 tests)

---

### Task 9: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–8.

- [ ] **Step 1: Full backend test + build**

Run (from `backend/`): `./mvnw test`
Expected: all tests pass, no regressions.

- [ ] **Step 2: Full frontend test suite**

Run (from `frontend/`): `npx ng test --watch=false`
Expected: all pre-existing tests pass, plus every new test added in Tasks 2–8.

- [ ] **Step 3: Full frontend production build**

Run (from `frontend/`): `npx ng build`
Expected: completes with no errors.

- [ ] **Step 4: End-to-end manual smoke check**

Start the backend (`./mvnw spring-boot:run` from `backend/`, wait for `Started BackendApplication`) and the frontend dev server (`npx ng serve --port 4200` from `frontend/`, wait for `Local: http://localhost:4200/`). If either is already running, reuse it rather than starting a second instance.

In a browser, visit `http://localhost:4200/login` and confirm, for each of the 5 seeded demo accounts:

| Email                              | Password      | Expected redirect |
|-------------------------------------|---------------|--------------------|
| `admin@travelease.test`             | `password123` | `/admin`           |
| `alice@travelease.test`             | `password123` | `/dashboard`       |
| `provider@travelease.test`          | `password123` | `/transport`       |
| `hotelprovider@travelease.test`     | `password123` | `/hotel`           |
| `activityprovider@travelease.test`  | `password123` | `/activity`        |

Then confirm:
- Entering a wrong password shows "Invalid email or password" and stays on `/login`.
- After logging in as `alice@travelease.test`, manually navigating the browser to `/admin` silently redirects back to `/dashboard`.
- Refreshing the page after login keeps the session (still redirected away from `/login` if visited again, still on the same dashboard if refreshed there).
- Clicking "Sign out" redirects to `/login`, and afterward manually navigating to `/dashboard` redirects back to `/login` (session is actually cleared).

This is a manual browser check — `curl` cannot exercise client-side routing/guards or observe redirects the way a browser does, so this step cannot be fully automated; the underlying logic is already covered by the unit tests in Tasks 3–8.

Stop any dev servers you started for this check (not ones that were already running) once done.

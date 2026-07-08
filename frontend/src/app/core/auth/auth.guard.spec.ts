import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
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

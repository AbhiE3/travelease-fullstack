import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBarChart3,
  lucideBell,
  lucideBus,
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideLayoutDashboard,
  lucideLogOut,
  lucideMail,
  lucidePlane,
  lucidePlus,
  lucideRoute,
  lucideSearch,
  lucideStar,
  lucideTrendingUp,
  lucideUser,
  lucideUserCheck,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { of } from 'rxjs';
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { AuthService } from '@app/core/auth/auth.service';

const ALL_ICONS = {
  lucideActivity,
  lucideBarChart3,
  lucideBell,
  lucideBus,
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideLayoutDashboard,
  lucideLogOut,
  lucideMail,
  lucidePlane,
  lucidePlus,
  lucideRoute,
  lucideSearch,
  lucideStar,
  lucideTrendingUp,
  lucideUser,
  lucideUserCheck,
  lucideUsers,
  lucideWallet,
};

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

describe('AppShell', () => {
  it('renders traveler nav items', async () => {
    await configureWithRole('traveler');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My Trips');
    expect(text).toContain('Invitations');
  });

  it('renders admin nav items', async () => {
    await configureWithRole('admin');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Route Analytics');
    expect(text).toContain('Partner Approvals');
  });

  it('shows the New Trip button for the traveler role', async () => {
    await configureWithRole('traveler');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('New Trip');
  });

  it('hides the New Trip button for the admin role', async () => {
    await configureWithRole('admin');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('New Trip');
  });

  it('defaults to the traveler role when route data has no role', async () => {
    await configureWithRole(undefined);
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My Trips');
  });

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
});

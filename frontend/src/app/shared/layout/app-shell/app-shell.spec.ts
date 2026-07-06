import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
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
});

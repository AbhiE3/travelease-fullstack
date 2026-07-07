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

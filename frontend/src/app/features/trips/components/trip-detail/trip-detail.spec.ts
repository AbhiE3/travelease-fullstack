import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
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
import { of } from 'rxjs';
import { trips } from '@app/core/mock-data';
import { TripDetail } from '@app/features/trips/components/trip-detail/trip-detail';

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

async function renderWithTripId(tripId: string | null) {
  await TestBed.configureTestingModule({
    imports: [TripDetail],
    providers: [
      provideRouter([]),
      provideIcons(ALL_ICONS),
      {
        provide: ActivatedRoute,
        useValue: { paramMap: of(convertToParamMap(tripId ? { tripId } : {})) },
      },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TripDetail);
  fixture.detectChanges();
  return fixture;
}

describe('TripDetail', () => {
  it('resolves the trip matching the route tripId', async () => {
    const fixture = await renderWithTripId('manali-winter');
    expect(fixture.componentInstance.trip().id).toBe('manali-winter');
  });

  it('falls back to the first trip when tripId matches nothing', async () => {
    const fixture = await renderWithTripId('does-not-exist');
    expect(fixture.componentInstance.trip()).toBe(trips[0]);
  });

  it('computes totalBudget and pct from the resolved trip', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const trip = trips.find((t) => t.id === 'goa-2026')!;
    const expectedTotal = trip.budgetPerPerson * trip.members;

    expect(fixture.componentInstance.totalBudget()).toBe(expectedTotal);
    expect(fixture.componentInstance.pct()).toBe(
      Math.round((trip.currentCost / expectedTotal) * 100),
    );
  });

  it('renders all 8 tab triggers', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const label of [
      'Overview',
      'Members',
      'Travel',
      'Accommodation',
      'Expenses',
      'Itinerary',
      'Alerts',
      'Reviews',
    ]) {
      expect(text).toContain(label);
    }
  });

  it('shows no coming-soon placeholder now that every tab has real content', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('This section is coming soon.');
  });
});

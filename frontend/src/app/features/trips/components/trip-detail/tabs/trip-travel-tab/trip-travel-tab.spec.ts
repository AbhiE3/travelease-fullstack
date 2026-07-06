import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideArrowRight,
  lucideBus,
  lucideSparkles,
  lucideStar,
} from '@ng-icons/lucide';
import { Trip, buses } from '@app/core/mock-data';
import { TripTravelTab } from '@app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab';

const BASE_TRIP: Trip = {
  id: 'test-trip',
  name: 'Test Trip',
  type: 'Friends',
  source: 'Bengaluru',
  destination: 'Goa',
  area: 'Baga Beach',
  startDate: '2026-07-12',
  endDate: '2026-07-16',
  budgetPerPerson: 10000,
  members: 4,
  currentCost: 10000,
  status: 'upcoming',
  image: 'https://example.com/image.jpg',
  progress: 50,
};

async function render(trip: Trip) {
  await TestBed.configureTestingModule({
    imports: [TripTravelTab],
    providers: [
      provideIcons({ lucideAlertTriangle, lucideArrowRight, lucideBus, lucideSparkles, lucideStar }),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TripTravelTab);
  fixture.componentRef.setInput('trip', trip);
  fixture.detectChanges();
  return fixture;
}

describe('TripTravelTab', () => {
  it('renders every bus from mock data', async () => {
    const fixture = await render(BASE_TRIP);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of buses) {
      expect(text).toContain(b.name);
    }
  });

  it('shows the Suitable for Group badge for every bus when the trip has few members', async () => {
    const lowMemberTrip: Trip = { ...BASE_TRIP, members: 2 };
    const fixture = await render(lowMemberTrip);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(buses.every((b) => b.seats >= 2)).toBe(true);
    expect(text.match(/Suitable for Group/g)?.length).toBe(buses.length);
  });

  it('hides the Suitable for Group badge when the trip has more members than any bus has seats', async () => {
    const highMemberTrip: Trip = { ...BASE_TRIP, members: 100 };
    const fixture = await render(highMemberTrip);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Suitable for Group');
  });

  it('renders exactly 30 seats in the allocation grid', async () => {
    const fixture = await render(BASE_TRIP);
    expect(fixture.componentInstance.seats).toHaveLength(30);
  });
});

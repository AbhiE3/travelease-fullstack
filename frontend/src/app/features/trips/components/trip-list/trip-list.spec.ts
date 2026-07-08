import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';

const SAMPLE_TRIPS: Trip[] = [
  {
    tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
    tripName: 'Goa Beach Escape',
    organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
    sourceLocation: 'Bengaluru',
    destinationId: 3,
    budgetAmount: 18000,
    categoryId: 1,
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    status: 'PLANNING',
    viewerRole: 'ORGANIZER',
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z',
  },
];

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [TripList],
    providers: [
      provideRouter([]),
      provideIcons({ lucidePlus, lucideCalendar, lucideWallet, lucideMapPin }),
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(TripList);
  fixture.detectChanges();
  return fixture;
}

describe('TripList', () => {
  it('shows a loading message before the trips arrive', async () => {
    const subject = new Subject<Trip[]>();
    const fixture = await setup({ listMyTrips: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading your trips');
  });

  it('renders trip cards with real fields once loaded', async () => {
    const fixture = await setup({ listMyTrips: () => of(SAMPLE_TRIPS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Goa Beach Escape');
    expect(el.textContent).toContain('Bengaluru');
    expect(el.textContent).toContain('Destination #3');
    expect(el.textContent).toContain('18,000');
    const link = el.querySelector(`a[href="/trips/${SAMPLE_TRIPS[0].tripId}"]`);
    expect(link).not.toBeNull();
  });

  it('shows an empty-state message when there are no trips', async () => {
    const fixture = await setup({ listMyTrips: () => of([]) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No trips yet');
  });

  it('shows an error message when the request fails', async () => {
    const fixture = await setup({
      listMyTrips: () => throwError(() => new Error('network error')),
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });
});

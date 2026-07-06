import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucidePlus, lucideUsers, lucideWallet } from '@ng-icons/lucide';
import { trips } from '@app/core/mock-data';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';

describe('TripList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripList],
      providers: [
        provideRouter([]),
        provideIcons({ lucidePlus, lucideCalendar, lucideUsers, lucideWallet, lucideMapPin }),
      ],
    }).compileComponents();
  });

  it('renders every trip from mock data, not a filtered subset', () => {
    const fixture = TestBed.createComponent(TripList);
    expect(fixture.componentInstance.trips).toEqual(trips);
    expect(fixture.componentInstance.trips.length).toBe(trips.length);
  });

  it('links each trip card to its detail route', () => {
    const fixture = TestBed.createComponent(TripList);
    fixture.detectChanges();

    const links = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a[href^="/trips/"]'),
    ) as HTMLAnchorElement[];
    const hrefs = links.map((a) => a.getAttribute('href'));
    for (const t of trips) {
      expect(hrefs).toContain(`/trips/${t.id}`);
    }
  });
});

import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucideStar } from '@ng-icons/lucide';
import { hotels } from '@app/core/mock-data';
import { TripAccommodationTab } from '@app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab';

describe('TripAccommodationTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripAccommodationTab],
      providers: [provideIcons({ lucideMapPin, lucideStar })],
    }).compileComponents();
  });

  it('renders every hotel from mock data', () => {
    const fixture = TestBed.createComponent(TripAccommodationTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of hotels) {
      expect(text).toContain(h.name);
    }
  });

  it('shows the Best Match badge only for the first hotel', () => {
    const fixture = TestBed.createComponent(TripAccommodationTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const count = (text.match(/Best Match/g) ?? []).length;
    expect(count).toBe(1);
  });
});

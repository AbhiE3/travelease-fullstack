import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideClock, lucideMapPin, lucidePlus, lucideSparkles } from '@ng-icons/lucide';
import { activities, itinerary } from '@app/core/mock-data';
import { TripItineraryTab } from '@app/features/trips/components/trip-detail/tabs/trip-itinerary-tab/trip-itinerary-tab';

describe('TripItineraryTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripItineraryTab],
      providers: [provideIcons({ lucideClock, lucideMapPin, lucidePlus, lucideSparkles })],
    }).compileComponents();
  });

  it('renders every itinerary day', () => {
    const fixture = TestBed.createComponent(TripItineraryTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const day of itinerary) {
      expect(text).toContain(day.title);
    }
  });

  it('renders every activity, not a sliced subset', () => {
    const fixture = TestBed.createComponent(TripItineraryTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of activities) {
      expect(text).toContain(a.name);
    }
    expect(fixture.componentInstance.activities).toHaveLength(activities.length);
  });
});

import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideStar } from '@ng-icons/lucide';
import { TripReviewsTab } from '@app/features/trips/components/trip-detail/tabs/trip-reviews-tab/trip-reviews-tab';

describe('TripReviewsTab', () => {
  it('renders all 3 hardcoded review card titles', async () => {
    await TestBed.configureTestingModule({
      imports: [TripReviewsTab],
      providers: [provideIcons({ lucideStar })],
    }).compileComponents();

    const fixture = TestBed.createComponent(TripReviewsTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Rate the Bus');
    expect(text).toContain('Rate the Hotel');
    expect(text).toContain('Rate the Trip Experience');
  });
});

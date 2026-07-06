import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideStar } from '@ng-icons/lucide';
import { HotelReviews } from '@app/features/hotel/components/hotel-reviews/hotel-reviews';

describe('HotelReviews', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HotelReviews],
      providers: [provideIcons({ lucideStar })],
    }).compileComponents();
  });

  it('renders all 4 hardcoded reviewer names', () => {
    const fixture = TestBed.createComponent(HotelReviews);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Sarathy R');
    expect(text).toContain('Anjali V');
    expect(text).toContain('Raj Patel');
    expect(text).toContain('Priya Sharma');
  });

  it("renders exactly as many stars as each review's own rating", () => {
    const fixture = TestBed.createComponent(HotelReviews);
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('[hlmCard]');
    const starCounts = Array.from(cards).map(
      (card) => card.querySelectorAll('ng-icon[name="lucideStar"]').length,
    );
    expect(starCounts).toEqual([5, 4, 5, 3]);
  });
});

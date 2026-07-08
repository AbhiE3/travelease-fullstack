import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideStar } from '@ng-icons/lucide';
import { HotelReviews } from '@app/features/hotel/components/hotel-reviews/hotel-reviews';
import { HotelProviderService } from '@app/features/hotel/services/hotel-provider.service';
import {
  TEST_PROVIDER_OVERVIEW,
  createHotelProviderStub,
} from '@app/features/hotel/testing/hotel-provider-test-data';

describe('HotelReviews', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HotelReviews],
      providers: [
        provideIcons({ lucideStar }),
        { provide: HotelProviderService, useValue: createHotelProviderStub() },
      ],
    }).compileComponents();
  });

  it('renders every provider reviewer name', () => {
    const fixture = TestBed.createComponent(HotelReviews);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const review of TEST_PROVIDER_OVERVIEW.reviews) {
      expect(text).toContain(review.userName);
    }
  });

  it("renders exactly as many stars as each review's own rating", () => {
    const fixture = TestBed.createComponent(HotelReviews);
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('[hlmCard]');
    const starCounts = Array.from(cards).map(
      (card) => card.querySelectorAll('ng-icon[name="lucideStar"]').length,
    );
    expect(starCounts).toEqual(TEST_PROVIDER_OVERVIEW.reviews.map((review) => Math.round(Number(review.rating))));
  });
});

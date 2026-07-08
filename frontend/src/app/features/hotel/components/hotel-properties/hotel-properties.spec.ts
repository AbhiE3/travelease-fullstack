import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucidePlus, lucideStar } from '@ng-icons/lucide';
import { HotelProperties } from '@app/features/hotel/components/hotel-properties/hotel-properties';
import { HotelProviderService } from '@app/features/hotel/services/hotel-provider.service';
import {
  TEST_PROVIDER_OVERVIEW,
  createHotelProviderStub,
} from '@app/features/hotel/testing/hotel-provider-test-data';

describe('HotelProperties', () => {
  it('renders every provider hotel name, price, and rating', async () => {
    await TestBed.configureTestingModule({
      imports: [HotelProperties],
      providers: [
        provideIcons({ lucideMapPin, lucidePlus, lucideStar }),
        { provide: HotelProviderService, useValue: createHotelProviderStub() },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(HotelProperties);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of TEST_PROVIDER_OVERVIEW.hotels) {
      expect(text).toContain(h.hotelName);
      expect(text).toContain(String(h.pricePerNight));
      expect(text).toContain(String(h.rating));
    }
  });
});

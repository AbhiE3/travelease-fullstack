import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucidePlus, lucideStar } from '@ng-icons/lucide';
import { hotels } from '@app/core/mock-data';
import { HotelProperties } from '@app/features/hotel/components/hotel-properties/hotel-properties';

describe('HotelProperties', () => {
  it('renders every hotel name, price, and rating', async () => {
    await TestBed.configureTestingModule({
      imports: [HotelProperties],
      providers: [provideIcons({ lucideMapPin, lucidePlus, lucideStar })],
    }).compileComponents();

    const fixture = TestBed.createComponent(HotelProperties);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of hotels) {
      expect(text).toContain(h.name);
      expect(text).toContain(String(h.price));
      expect(text).toContain(String(h.rating));
    }
  });
});

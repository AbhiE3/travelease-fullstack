import { TestBed } from '@angular/core/testing';
import { HotelBookings } from '@app/features/hotel/components/hotel-bookings/hotel-bookings';
import { HotelProviderService } from '@app/features/hotel/services/hotel-provider.service';
import {
  TEST_PROVIDER_OVERVIEW,
  createHotelProviderStub,
} from '@app/features/hotel/testing/hotel-provider-test-data';

describe('HotelBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HotelBookings],
      providers: [{ provide: HotelProviderService, useValue: createHotelProviderStub() }],
    }).compileComponents();
  });

  it('renders every provider booking guest and room', () => {
    const fixture = TestBed.createComponent(HotelBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of TEST_PROVIDER_OVERVIEW.bookings) {
      expect(text).toContain(b.bookedByUserName);
      expect(text).toContain(b.roomType);
    }
  });

  it('gives Confirmed and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(HotelBookings);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const confirmedBadge = badges.find((b) => b.textContent === 'Confirmed')!;
    const pendingBadge = badges.find((b) => b.textContent === 'Pending')!;
    expect(confirmedBadge.className).toContain('text-success');
    expect(pendingBadge.className).toContain('border-warning/20');
  });
});

import { TestBed } from '@angular/core/testing';
import { hotelBookings } from '@app/core/mock-data';
import { HotelBookings } from '@app/features/hotel/components/hotel-bookings/hotel-bookings';

describe('HotelBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HotelBookings] }).compileComponents();
  });

  it('renders every hotelBookings entry guest and room', () => {
    const fixture = TestBed.createComponent(HotelBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of hotelBookings) {
      expect(text).toContain(b.guest);
      expect(text).toContain(b.room);
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

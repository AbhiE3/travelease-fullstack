import { TestBed } from '@angular/core/testing';
import { TransportBookings } from '@app/features/transport/components/transport-bookings/transport-bookings';

describe('TransportBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TransportBookings] }).compileComponents();
  });

  it('renders every booking passenger and route', () => {
    const fixture = TestBed.createComponent(TransportBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of fixture.componentInstance.bookings) {
      expect(text).toContain(b.passenger);
      expect(text).toContain(b.route);
    }
  });

  it('gives Confirmed and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(TransportBookings);
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

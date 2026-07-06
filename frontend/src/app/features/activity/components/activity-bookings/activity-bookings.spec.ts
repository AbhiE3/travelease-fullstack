import { TestBed } from '@angular/core/testing';
import { ActivityBookings } from '@app/features/activity/components/activity-bookings/activity-bookings';

describe('ActivityBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ActivityBookings] }).compileComponents();
  });

  it('renders every booking customer and activity name', () => {
    const fixture = TestBed.createComponent(ActivityBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of fixture.componentInstance.bookings) {
      expect(text).toContain(b.customer);
      expect(text).toContain(b.activity);
    }
  });

  it('gives Confirmed and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(ActivityBookings);
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

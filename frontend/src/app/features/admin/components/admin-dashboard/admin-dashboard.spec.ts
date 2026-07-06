import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideBus,
  lucideHotel,
  lucidePlane,
  lucideTrendingUp,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import {
  AdminDashboard,
  bookingBarHeight,
} from '@app/features/admin/components/admin-dashboard/admin-dashboard';

describe('bookingBarHeight', () => {
  it('matches the sine-based formula from the React source', () => {
    for (let i = 0; i < 30; i++) {
      expect(bookingBarHeight(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.7) * 70) + (i % 4) * 5);
    }
  });
});

describe('AdminDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [
        provideIcons({
          lucideBus,
          lucideHotel,
          lucidePlane,
          lucideTrendingUp,
          lucideUsers,
          lucideWallet,
        }),
      ],
    }).compileComponents();
  });

  it('renders all 6 stat labels, values, and trends', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Total Trips');
    expect(text).toContain('248');
    expect(text).toContain('+12%');
    expect(text).toContain('Active Users');
    expect(text).toContain('1,842');
    expect(text).toContain('+8%');
    expect(text).toContain('Revenue (MTD)');
    expect(text).toContain('₹6.4L');
    expect(text).toContain('+18%');
    expect(text).toContain('Buses');
    expect(text).toContain('36');
    expect(text).toContain('—');
    expect(text).toContain('Hotels');
    expect(text).toContain('89');
    expect(text).toContain('+3');
    expect(text).toContain('Bus Occupancy');
    expect(text).toContain('82%');
    expect(text).toContain('+5%');
  });

  it('renders all 30 bars with heights matching the sine formula', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    expect(fixture.componentInstance.bars).toHaveLength(30);
    fixture.componentInstance.bars.forEach((h, i) => {
      expect(h).toBeCloseTo(bookingBarHeight(i));
    });
  });

  it('renders all 5 Popular Destinations names and percentages', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const d of [
      { name: 'Goa', pct: 92 },
      { name: 'Manali', pct: 74 },
      { name: 'Kerala', pct: 68 },
      { name: 'Pondicherry', pct: 55 },
      { name: 'Coorg', pct: 41 },
    ]) {
      expect(text).toContain(d.name);
      expect(text).toContain(`${d.pct}%`);
    }
  });
});

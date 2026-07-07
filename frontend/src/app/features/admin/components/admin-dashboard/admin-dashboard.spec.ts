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
  buildBookingsBarOption,
} from '@app/features/admin/components/admin-dashboard/admin-dashboard';

describe('bookingBarHeight', () => {
  it('matches the sine-based formula from the React source', () => {
    for (let i = 0; i < 30; i++) {
      expect(bookingBarHeight(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.7) * 70) + (i % 4) * 5);
    }
  });
});

describe('buildBookingsBarOption', () => {
  it('puts the bars array directly into the bar series data, unmodified', () => {
    const bars = [10, 50, 90];
    const option = buildBookingsBarOption(bars);
    const series = (option['series'] as any[])[0];
    expect(series.data).toEqual(bars);
  });

  it('labels the x-axis with 1-based day numbers', () => {
    const option = buildBookingsBarOption([10, 50, 90]);
    const xAxis = option['xAxis'] as { data: string[] };
    expect(xAxis.data).toEqual(['1', '2', '3']);
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

  it('builds 30 bars matching the sine formula, fed into the bookings chart options', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    const c = fixture.componentInstance;
    expect(c.bars).toHaveLength(30);
    c.bars.forEach((h, i) => {
      expect(h).toBeCloseTo(bookingBarHeight(i));
    });
    const series = (c.bookingsChartOptions['series'] as any[])[0];
    expect(series.data).toEqual(c.bars);
  });

  it('builds a destinations chart with all 5 names and percentages', () => {
    const fixture = TestBed.createComponent(AdminDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.destinationsChartOptions['yAxis'] as { data: string[] };
    const series = (c.destinationsChartOptions['series'] as any[])[0];

    for (const d of [
      { name: 'Goa', pct: 92 },
      { name: 'Manali', pct: 74 },
      { name: 'Kerala', pct: 68 },
      { name: 'Pondicherry', pct: 55 },
      { name: 'Coorg', pct: 41 },
    ]) {
      const idx = yAxis.data.indexOf(d.name);
      expect(idx).toBeGreaterThanOrEqual(0);
      expect(series.data[idx]).toBe(d.pct);
    }
  });
});

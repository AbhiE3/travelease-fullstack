import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideClock, lucideTrendingDown, lucideTrendingUp, lucideWallet } from '@ng-icons/lucide';
import { routeAnalytics } from '@app/core/mock-data';
import { AdminRouteAnalytics } from '@app/features/admin/components/admin-route-analytics/admin-route-analytics';

describe('AdminRouteAnalytics', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminRouteAnalytics],
      providers: [
        provideIcons({ lucideClock, lucideTrendingDown, lucideTrendingUp, lucideWallet }),
      ],
    }).compileComponents();
  });

  it('computes total routes, bookings, revenue, and average cancellation correctly', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;
    const totalBookings = routeAnalytics.reduce((s, r) => s + r.bookings, 0);
    const totalRevenue = routeAnalytics.reduce((s, r) => s + r.revenue, 0);
    const avgCancel = (
      routeAnalytics.reduce((s, r) => s + r.cancellation, 0) / routeAnalytics.length
    ).toFixed(1);

    expect(c.totalRoutes).toBe(routeAnalytics.length);
    expect(c.totalBookings).toBe(totalBookings);
    expect(c.totalRevenueLabel).toBe(`₹${(totalRevenue / 100000).toFixed(1)}L`);
    expect(c.avgCancellation).toBe(avgCancel);
  });

  it('selects the top 3 and bottom 3 routes by bookings, in the right order', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;
    const sorted = [...routeAnalytics].sort((a, b) => b.bookings - a.bookings);
    expect(c.top.map((r) => r.route)).toEqual(sorted.slice(0, 3).map((r) => r.route));
    expect(c.bottom.map((r) => r.route)).toEqual(
      [...sorted].slice(-3).reverse().map((r) => r.route),
    );
  });

  it('gives a >7% cancellation route the destructive tone and a <=7% route the success tone', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;
    const high = c.rows.find((r) => r.cancellation > 7)!;
    const low = c.rows.find((r) => r.cancellation <= 7)!;
    expect(high.cancellationClass).toContain('text-destructive');
    expect(low.cancellationClass).toContain('text-success');
  });

  it('builds top/bottom charts with bookings values matching the top/bottom route lists', () => {
    const fixture = TestBed.createComponent(AdminRouteAnalytics);
    const c = fixture.componentInstance;

    const topSeries = (c.topChartOptions['series'] as any[])[0];
    const topYAxis = c.topChartOptions['yAxis'] as { data: string[] };
    for (const r of c.top) {
      const idx = topYAxis.data.indexOf(r.route);
      expect(topSeries.data[idx]).toBe(r.bookings);
    }

    const bottomSeries = (c.bottomChartOptions['series'] as any[])[0];
    const bottomYAxis = c.bottomChartOptions['yAxis'] as { data: string[] };
    for (const r of c.bottom) {
      const idx = bottomYAxis.data.indexOf(r.route);
      expect(bottomSeries.data[idx]).toBe(r.bookings);
    }
  });
});

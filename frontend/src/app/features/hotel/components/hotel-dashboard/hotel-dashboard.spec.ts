import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideStar,
  lucideWallet,
} from '@ng-icons/lucide';
import {
  HotelDashboard,
  buildOccupancyCalendarOption,
  calendarOccupancy,
  currentMonthDates,
} from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';
import { HotelProviderService } from '@app/features/hotel/services/hotel-provider.service';
import {
  availableRoomCount,
  bookingsToday as expectedBookingsToday,
  formatCompactCurrency,
  groupRooms,
  monthlyRevenue,
} from '@app/features/hotel/services/hotel-provider-view-models';
import {
  TEST_PROVIDER_OVERVIEW,
  createHotelProviderStub,
} from '@app/features/hotel/testing/hotel-provider-test-data';

describe('calendarOccupancy', () => {
  it('matches the sine-based formula from the React source for a few indices', () => {
    for (const i of [0, 5, 13, 27]) {
      expect(calendarOccupancy(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.9) * 60) + (i % 5) * 4);
    }
  });
});

describe('currentMonthDates', () => {
  it('returns one ISO date string per day in a 28-day month', () => {
    const dates = currentMonthDates(new Date(2026, 1, 1));
    expect(dates).toHaveLength(28);
    expect(dates[0]).toBe('2026-02-01');
    expect(dates[27]).toBe('2026-02-28');
  });

  it('handles a 31-day month', () => {
    const dates = currentMonthDates(new Date(2026, 6, 1));
    expect(dates).toHaveLength(31);
    expect(dates[0]).toBe('2026-07-01');
    expect(dates[30]).toBe('2026-07-31');
  });
});

describe('buildOccupancyCalendarOption', () => {
  it('sets the calendar range to the first and last date, and one heatmap point per date', () => {
    const dates = currentMonthDates(new Date(2026, 6, 1));
    const option = buildOccupancyCalendarOption(dates, 'oklch(0.5 0.1 200)', 'oklch(0.9 0.01 200)');
    const calendar = option['calendar'] as any;
    const series = (option['series'] as any[])[0];

    expect(calendar.range).toEqual(['2026-07-01', '2026-07-31']);
    expect(series.data).toHaveLength(31);
    expect(series.data[0]).toEqual(['2026-07-01', calendarOccupancy(0)]);
  });
});

describe('HotelDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HotelDashboard],
      providers: [
        provideIcons({ lucideCalendarDays, lucideDoorOpen, lucideHotel, lucideStar, lucideWallet }),
        { provide: HotelProviderService, useValue: createHotelProviderStub() },
      ],
    }).compileComponents();
  });

  it('computes all 4 stat values from provider rooms and bookings', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;

    expect(c.totalRooms).toBe(TEST_PROVIDER_OVERVIEW.rooms.length);
    expect(c.availableRooms).toBe(availableRoomCount(TEST_PROVIDER_OVERVIEW.rooms));
    expect(c.bookingsToday).toBe(expectedBookingsToday(TEST_PROVIDER_OVERVIEW.bookings));
    expect(c.revenueMtd).toBe(formatCompactCurrency(monthlyRevenue(TEST_PROVIDER_OVERVIEW.bookings)));
  });

  it('builds a calendar chart spanning the current month', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const expectedDates = currentMonthDates();
    const calendar = c.calendarOptions['calendar'] as any;
    expect(calendar.range).toEqual([expectedDates[0], expectedDates[expectedDates.length - 1]]);
  });

  it('renders provider bookings in Recent Bookings', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Sarathy R');
    expect(text).toContain('Anjali V');
    expect(fixture.componentInstance.recentBookings).toHaveLength(2);
  });

  it('renders grouped room types with the correct available/total numbers', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of groupRooms(TEST_PROVIDER_OVERVIEW.rooms)) {
      expect(text).toContain(r.type);
      expect(text).toContain(`${r.available} / ${r.total}`);
    }
  });

  it('builds a room occupancy chart with the occupied percentage for every room type', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.roomInventoryOptions['yAxis'] as { data: string[] };
    const series = (c.roomInventoryOptions['series'] as any[])[0];

    for (const r of c.roomInventory) {
      const idx = yAxis.data.indexOf(r.type);
      expect(series.data[idx]).toBe(Math.round(r.pct));
    }
  });

  it('renders the provider rating average and review count', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('4.3');
    expect(text).toContain('4 reviews');
  });

  it('builds a rating distribution chart with all 5 star percentages', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.ratingOptions['yAxis'] as { data: string[] };
    const series = (c.ratingOptions['series'] as any[])[0];

    for (const row of c.ratingRows) {
      const idx = yAxis.data.indexOf(`${row.stars}\u2605`);
      expect(series.data[idx]).toBe(row.pct);
    }
  });
});

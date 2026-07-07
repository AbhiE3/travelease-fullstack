import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideStar,
  lucideWallet,
} from '@ng-icons/lucide';
import { hotelBookings, rooms } from '@app/core/mock-data';
import {
  HotelDashboard,
  buildOccupancyCalendarOption,
  calendarOccupancy,
  currentMonthDates,
} from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';

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
      ],
    }).compileComponents();
  });

  it('computes all 4 stat values from rooms and hotelBookings', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const totalRooms = rooms.reduce((s, r) => s + r.total, 0);
    const availableRooms = rooms.reduce((s, r) => s + r.available, 0);
    const revenue = hotelBookings.reduce((s, b) => s + b.total, 0);

    expect(c.totalRooms).toBe(totalRooms);
    expect(c.availableRooms).toBe(availableRooms);
    expect(c.bookingsToday).toBe(hotelBookings.length);
    expect(c.revenueMtd).toBe(`₹${(revenue / 1000).toFixed(0)}k`);
  });

  it('builds a calendar chart spanning the current month', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const expectedDates = currentMonthDates();
    const calendar = c.calendarOptions['calendar'] as any;
    expect(calendar.range).toEqual([expectedDates[0], expectedDates[expectedDates.length - 1]]);
  });

  it('renders every hotelBookings entry in Recent Bookings (slice(0,4) keeps all 4)', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of hotelBookings) {
      expect(text).toContain(b.guest);
    }
    expect(fixture.componentInstance.recentBookings).toHaveLength(4);
  });

  it('renders every room type with the correct available/total numbers', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of rooms) {
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

  it('renders the hardcoded rating average and review count', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('4.7');
    expect(text).toContain('182 reviews');
  });

  it('builds a rating distribution chart with all 5 star percentages', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    const c = fixture.componentInstance;
    const yAxis = c.ratingOptions['yAxis'] as { data: string[] };
    const series = (c.ratingOptions['series'] as any[])[0];

    for (const row of c.ratingRows) {
      const idx = yAxis.data.indexOf(`${row.stars}★`);
      expect(series.data[idx]).toBe(row.pct);
    }
  });
});

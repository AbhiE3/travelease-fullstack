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
  calendarOccupancy,
} from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';

describe('calendarOccupancy', () => {
  it('matches the sine-based formula from the React source for a few indices', () => {
    for (const i of [0, 5, 13, 27]) {
      expect(calendarOccupancy(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.9) * 60) + (i % 5) * 4);
    }
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

  it('renders all 28 calendar cells with the correct day numbers', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    expect(fixture.componentInstance.calendar).toHaveLength(28);
    expect(fixture.componentInstance.calendar.map((c) => c.day)).toEqual(
      Array.from({ length: 28 }, (_, i) => i + 1),
    );
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

  it('renders the hardcoded rating snapshot values', () => {
    const fixture = TestBed.createComponent(HotelDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('4.7');
    expect(text).toContain('182 reviews');
    for (const pct of [72, 18, 6, 2, 2]) {
      expect(text).toContain(`${pct}%`);
    }
  });
});

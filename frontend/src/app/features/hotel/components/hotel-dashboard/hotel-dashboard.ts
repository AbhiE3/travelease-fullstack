import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { hotelBookings, rooms } from '@app/core/mock-data';

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const RATING_PERCENTAGES = [72, 18, 6, 2, 2];

interface CalendarCell {
  day: number;
  occ: number;
  background: string;
  textToneClass: string;
  subTextToneClass: string;
}

interface RoomInventoryView {
  id: string;
  type: string;
  price: number;
  available: number;
  total: number;
  pct: number;
}

interface RatingRow {
  stars: number;
  pct: number;
}

export function calendarOccupancy(i: number): number {
  return 30 + Math.abs(Math.sin(i * 0.9) * 60) + (i % 5) * 4;
}

function buildCalendar(): CalendarCell[] {
  return Array.from({ length: 28 }, (_, i) => {
    const occ = calendarOccupancy(i);
    return {
      day: i + 1,
      occ,
      background: `color-mix(in oklab, var(--primary) ${occ * 0.6}%, var(--card))`,
      textToneClass:
        occ > 60 ? 'text-primary-foreground font-semibold' : 'text-foreground font-medium',
      subTextToneClass: occ > 60 ? 'text-primary-foreground/80' : 'text-muted-foreground',
    };
  });
}

@Component({
  selector: 'app-hotel-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './hotel-dashboard.html',
})
export class HotelDashboard {
  public readonly days = DAYS;

  public readonly totalRooms = rooms.reduce((s, r) => s + r.total, 0);
  public readonly availableRooms = rooms.reduce((s, r) => s + r.available, 0);
  public readonly bookingsToday = hotelBookings.length;
  public readonly revenueMtd = `₹${(hotelBookings.reduce((s, b) => s + b.total, 0) / 1000).toFixed(0)}k`;

  public readonly calendar: CalendarCell[] = buildCalendar();

  public readonly recentBookings = hotelBookings.slice(0, 4);

  public readonly roomInventory: RoomInventoryView[] = rooms.map((r) => ({
    id: r.id,
    type: r.type,
    price: r.price,
    available: r.available,
    total: r.total,
    pct: ((r.total - r.available) / r.total) * 100,
  }));

  public readonly ratingAverage = 4.7;
  public readonly ratingCount = 182;
  public readonly ratingRows: RatingRow[] = [5, 4, 3, 2, 1].map((s) => ({
    stars: s,
    pct: RATING_PERCENTAGES[5 - s],
  }));
}

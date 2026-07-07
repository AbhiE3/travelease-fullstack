import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import { hotelBookings, rooms } from '@app/core/mock-data';
import type { EChartsCoreOption } from 'echarts/core';

const RATING_PERCENTAGES = [72, 18, 6, 2, 2];

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

export function currentMonthDates(referenceDate = new Date()): string[] {
  const year = referenceDate.getFullYear();
  const month = referenceDate.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const pad = (n: number) => String(n).padStart(2, '0');
  return Array.from({ length: daysInMonth }, (_, i) => `${year}-${pad(month + 1)}-${pad(i + 1)}`);
}

export function buildOccupancyCalendarOption(
  dates: string[],
  color: string,
  mutedColor: string,
): EChartsCoreOption {
  return {
    animationDuration: 1800,
    tooltip: {
      formatter: (params: any) => `${params.value[0]}<br/>${params.value[1].toFixed(0)}% occupancy`,
    },
    visualMap: {
      show: false,
      min: 0,
      max: 100,
      inRange: { color: [mutedColor, color] },
    },
    calendar: {
      range: [dates[0], dates[dates.length - 1]],
      cellSize: ['auto', 28],
      itemStyle: { borderWidth: 2, borderColor: '#fff' },
      yearLabel: { show: false },
      monthLabel: { show: false },
      dayLabel: { firstDay: 1 },
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: dates.map((date, i) => [date, calendarOccupancy(i)]),
      },
    ],
  };
}

@Component({
  selector: 'app-hotel-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader, StatusBadge, EChart],
  templateUrl: './hotel-dashboard.html',
})
export class HotelDashboard {
  public readonly totalRooms = rooms.reduce((s, r) => s + r.total, 0);
  public readonly availableRooms = rooms.reduce((s, r) => s + r.available, 0);
  public readonly bookingsToday = hotelBookings.length;
  public readonly revenueMtd = `₹${(hotelBookings.reduce((s, b) => s + b.total, 0) / 1000).toFixed(0)}k`;

  private readonly calendarDates = currentMonthDates();
  public readonly calendarMonthLabel = new Date().toLocaleString('en-US', { month: 'long' });
  public readonly calendarOptions: EChartsCoreOption = buildOccupancyCalendarOption(
    this.calendarDates,
    CHART_COLORS.primary,
    CHART_COLORS.muted,
  );

  public readonly recentBookings = hotelBookings.slice(0, 4);

  public readonly roomInventory: RoomInventoryView[] = rooms.map((r) => ({
    id: r.id,
    type: r.type,
    price: r.price,
    available: r.available,
    total: r.total,
    pct: ((r.total - r.available) / r.total) * 100,
  }));

  public readonly roomInventoryOptions: EChartsCoreOption = buildRankingBarOption(
    this.roomInventory.map((r) => ({ label: r.type, value: Math.round(r.pct) })),
    CHART_COLORS.primary,
    '%',
  );

  public readonly ratingAverage = 4.7;
  public readonly ratingCount = 182;
  public readonly ratingRows: RatingRow[] = [5, 4, 3, 2, 1].map((s) => ({
    stars: s,
    pct: RATING_PERCENTAGES[5 - s],
  }));

  public readonly ratingOptions: EChartsCoreOption = buildRankingBarOption(
    this.ratingRows.map((r) => ({ label: `${r.stars}★`, value: r.pct })),
    CHART_COLORS.warning,
    '%',
  );
}

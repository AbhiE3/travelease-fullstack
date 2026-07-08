import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { EChart } from '@app/shared/ui/echart/echart';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { buildRankingBarOption } from '@app/shared/ui/echart/ranking-bar-chart';
import { WorkspaceSearchService } from '@app/shared/services/workspace-search.service';
import {
  EMPTY_PROVIDER_OVERVIEW,
  HotelProviderService,
  ProviderOverview,
} from '@app/features/hotel/services/hotel-provider.service';
import {
  HotelBookingView,
  RatingRow,
  RoomInventoryView,
  availableRoomCount,
  averageRating,
  bookingsToday,
  buildRatingRows,
  filterProviderOverview,
  formatCompactCurrency,
  groupRooms,
  mapBookingRows,
  monthlyRevenue,
  providerSubtitle,
} from '@app/features/hotel/services/hotel-provider-view-models';
import type { EChartsCoreOption } from 'echarts/core';
import { catchError, combineLatest, of } from 'rxjs';

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
  values = dates.map((_, i) => calendarOccupancy(i)),
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
        data: dates.map((date, i) => [date, values[i] ?? 0]),
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
  private readonly hotelProvider = inject(HotelProviderService);
  private readonly workspaceSearch = inject(WorkspaceSearchService);

  public totalRooms = 0;
  public availableRooms = 0;
  public bookingsToday = 0;
  public revenueMtd = formatCompactCurrency(0);
  public dashboardSubtitle = 'Live hotel performance and inventory.';

  private readonly calendarDates = currentMonthDates();
  public readonly calendarMonthLabel = new Date().toLocaleString('en-US', { month: 'long' });
  public calendarOptions: EChartsCoreOption = buildOccupancyCalendarOption(
    this.calendarDates,
    CHART_COLORS.primary,
    CHART_COLORS.muted,
  );

  public recentBookings: HotelBookingView[] = [];
  public roomInventory: RoomInventoryView[] = [];
  public roomInventoryOptions: EChartsCoreOption = this.buildRoomInventoryOptions();
  public ratingAverage = '0.0';
  public ratingCount = 0;
  public ratingRows: RatingRow[] = buildRatingRows([]);
  public ratingOptions: EChartsCoreOption = this.buildRatingOptions();

  constructor() {
    combineLatest([
      this.hotelProvider.getProviderOverview().pipe(catchError(() => of(EMPTY_PROVIDER_OVERVIEW))),
      this.workspaceSearch.hotelQuery$,
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(([overview, query]) => this.applyOverview(filterProviderOverview(overview, query)));
  }

  private applyOverview(overview: ProviderOverview): void {
    this.totalRooms = overview.rooms.length;
    this.availableRooms = availableRoomCount(overview.rooms);
    this.bookingsToday = bookingsToday(overview.bookings);
    this.revenueMtd = formatCompactCurrency(monthlyRevenue(overview.bookings));
    this.dashboardSubtitle = providerSubtitle(overview.hotels);

    this.recentBookings = mapBookingRows(overview.bookings).slice(0, 4);
    this.roomInventory = groupRooms(overview.rooms);
    this.roomInventoryOptions = this.buildRoomInventoryOptions();

    const occupancy = this.totalRooms > 0 ? ((this.totalRooms - this.availableRooms) / this.totalRooms) * 100 : 0;
    this.calendarOptions = buildOccupancyCalendarOption(
      this.calendarDates,
      CHART_COLORS.primary,
      CHART_COLORS.muted,
      this.calendarDates.map(() => Math.round(occupancy)),
    );

    this.ratingAverage = averageRating(overview).toFixed(1);
    this.ratingCount = overview.reviews.length;
    this.ratingRows = buildRatingRows(overview.reviews);
    this.ratingOptions = this.buildRatingOptions();
  }

  private buildRoomInventoryOptions(): EChartsCoreOption {
    return buildRankingBarOption(
      this.roomInventory.map((r) => ({ label: r.type, value: Math.round(r.pct) })),
      CHART_COLORS.primary,
      '%',
    );
  }

  private buildRatingOptions(): EChartsCoreOption {
    return buildRankingBarOption(
      this.ratingRows.map((r) => ({ label: `${r.stars}\u2605`, value: r.pct })),
      CHART_COLORS.warning,
      '%',
    );
  }
}

import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import type { EChartsCoreOption } from 'echarts/core';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';
import { WorkspaceSearchService } from '@app/shared/services/workspace-search.service';
import {
  EMPTY_PROVIDER_OVERVIEW,
  HotelProviderService,
} from '@app/features/hotel/services/hotel-provider.service';
import {
  ReportStat,
  buildReportStats,
  buildRevenueTrendData,
  filterProviderOverview,
} from '@app/features/hotel/services/hotel-provider-view-models';
import { catchError, combineLatest, of } from 'rxjs';

export function buildRevenueLineChartOption(data: number[]): EChartsCoreOption {
  const max = Math.max(200, Math.ceil(Math.max(...data, 0) * 1.15));

  return {
    animationDuration: 1800,
    grid: { left: -10, right: -10, top: 10, bottom: -10, containLabel: false },
    xAxis: { type: 'category', show: false, boundaryGap: false },
    yAxis: { type: 'value', show: false, max },
    tooltip: { trigger: 'axis' },
    series: [
      {
        type: 'line',
        data,
        smooth: false,
        symbol: 'none',
        lineStyle: { color: CHART_COLORS.primary, width: 2.5 },
        areaStyle: { color: CHART_COLORS.primary, opacity: 0.12 },
      },
    ],
  };
}

@Component({
  selector: 'app-hotel-reports',
  imports: [HlmCardImports, PageHeader, EChart],
  templateUrl: './hotel-reports.html',
})
export class HotelReports {
  private readonly hotelProvider = inject(HotelProviderService);
  private readonly workspaceSearch = inject(WorkspaceSearchService);

  public stats: ReportStat[] = buildReportStats(EMPTY_PROVIDER_OVERVIEW);
  public revenueChartOptions = buildRevenueLineChartOption(Array.from({ length: 11 }, () => 0));

  constructor() {
    combineLatest([
      this.hotelProvider.getProviderOverview().pipe(catchError(() => of(EMPTY_PROVIDER_OVERVIEW))),
      this.workspaceSearch.hotelQuery$,
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(([overview, query]) => {
        const filteredOverview = filterProviderOverview(overview, query);
        this.stats = buildReportStats(filteredOverview);
        this.revenueChartOptions = buildRevenueLineChartOption(buildRevenueTrendData(filteredOverview.bookings));
      });
  }
}

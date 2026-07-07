import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import type { EChartsCoreOption } from 'echarts/core';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';

interface ReportStat {
  label: string;
  value: string;
  icon: string;
}

interface TopDestinationRow {
  name: string;
  trips: number;
}

const STATS: ReportStat[] = [
  { label: 'Total Trips', value: '248', icon: 'lucidePlane' },
  { label: 'Active Users', value: '1,842', icon: 'lucideUsers' },
  { label: 'Revenue', value: '₹6.4L', icon: 'lucideWallet' },
  { label: 'Bus Occupancy', value: '82%', icon: 'lucideBus' },
  { label: 'Hotel Occupancy', value: '76%', icon: 'lucideHotel' },
  { label: 'Growth (MoM)', value: '+18%', icon: 'lucideTrendingUp' },
];

const TOP_DESTINATIONS: TopDestinationRow[] = [
  { name: 'Goa', trips: 92 },
  { name: 'Manali', trips: 74 },
  { name: 'Kerala', trips: 68 },
  { name: 'Pondicherry', trips: 55 },
  { name: 'Coorg', trips: 41 },
  { name: 'Jaipur', trips: 38 },
];

export function buildRevenueLineChartOption(data: number[]): EChartsCoreOption {
  return {
    animationDuration: 1800,
    grid: { left: -10, right: -10, top: 10, bottom: -10, containLabel: false },
    xAxis: { type: 'category', show: false, boundaryGap: false },
    yAxis: { type: 'value', show: false, max: 200 },
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
  selector: 'app-admin-reports',
  imports: [NgIcon, HlmCardImports, PageHeader, EChart],
  templateUrl: './admin-reports.html',
})
export class AdminReports {
  public readonly stats = STATS;
  public readonly destinations = TOP_DESTINATIONS;
  public readonly revenueChartOptions = buildRevenueLineChartOption([
    40, 60, 50, 90, 80, 120, 110, 140, 130, 160, 170
  ]);
}

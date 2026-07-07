import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { EChart } from '@app/shared/ui/echart/echart';
import type { EChartsCoreOption } from 'echarts/core';
import { CHART_COLORS } from '@app/shared/ui/echart/echart-theme';

interface ReportStat {
  label: string;
  value: string;
}

const STATS: ReportStat[] = [
  { label: 'Occupancy', value: '78%' },
  { label: 'Revenue MTD', value: '₹9.4L' },
  { label: 'ADR', value: '₹4,820' },
  { label: 'Avg Rating', value: '4.7' },
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
  selector: 'app-hotel-reports',
  imports: [HlmCardImports, PageHeader, EChart],
  templateUrl: './hotel-reports.html',
})
export class HotelReports {
  public readonly stats = STATS;
  public readonly revenueChartOptions = buildRevenueLineChartOption([
    50, 70, 60, 100, 90, 120, 110, 140, 130, 160, 170
  ]);
}

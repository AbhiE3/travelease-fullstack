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
  { label: 'Occupancy', value: '82%' },
  { label: 'Revenue MTD', value: '₹12.4L' },
  { label: 'Trips Completed', value: '186' },
  { label: 'Avg Rating', value: '4.6' },
];

export function weeklyBarHeight(i: number): number {
  return 30 + Math.abs(Math.sin(i * 0.7) * 70);
}

export function buildWeeklyBookingsBarOption(data: number[]): EChartsCoreOption {
  return {
    animationDuration: 1800,
    grid: { left: 8, right: 8, top: 8, bottom: 24, containLabel: false },
    xAxis: {
      type: 'category',
      data: data.map((_, i) => `Week ${i + 1}`),
      axisLabel: { color: CHART_COLORS.mutedForeground },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: { type: 'value', show: false, max: 100 },
    tooltip: { trigger: 'axis' },
    series: [
      {
        type: 'bar',
        data,
        itemStyle: { color: CHART_COLORS.primary, opacity: 0.8, borderRadius: [4, 4, 0, 0] },
        barCategoryGap: '30%',
      },
    ],
  };
}

@Component({
  selector: 'app-transport-reports',
  imports: [HlmCardImports, PageHeader, EChart],
  templateUrl: './transport-reports.html',
})
export class TransportReports {
  public readonly stats = STATS;
  public readonly bars = Array.from({ length: 12 }, (_, i) => weeklyBarHeight(i));
  public readonly weeklyBookingsOptions = buildWeeklyBookingsBarOption(this.bars);
}

import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

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

export const REVENUE_TREND_LINE_POINTS =
  '0,150 40,130 80,140 120,100 160,110 200,80 240,90 280,60 320,70 360,40 400,30';
export const REVENUE_TREND_AREA_POINTS = `${REVENUE_TREND_LINE_POINTS} 400,200 0,200`;

@Component({
  selector: 'app-hotel-reports',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './hotel-reports.html',
})
export class HotelReports {
  public readonly stats = STATS;
  public readonly linePoints = REVENUE_TREND_LINE_POINTS;
  public readonly areaPoints = REVENUE_TREND_AREA_POINTS;
}

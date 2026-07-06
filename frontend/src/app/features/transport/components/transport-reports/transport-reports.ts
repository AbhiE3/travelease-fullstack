import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

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

@Component({
  selector: 'app-transport-reports',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './transport-reports.html',
})
export class TransportReports {
  public readonly stats = STATS;
  public readonly bars = Array.from({ length: 12 }, (_, i) => weeklyBarHeight(i));
}

import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

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

export const REVENUE_TREND_LINE_POINTS =
  '0,160 40,140 80,150 120,110 160,120 200,80 240,90 280,60 320,70 360,40 400,30';
export const REVENUE_TREND_AREA_POINTS = `${REVENUE_TREND_LINE_POINTS} 400,200 0,200`;

@Component({
  selector: 'app-admin-reports',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './admin-reports.html',
})
export class AdminReports {
  public readonly stats = STATS;
  public readonly destinations = TOP_DESTINATIONS;
  public readonly linePoints = REVENUE_TREND_LINE_POINTS;
  public readonly areaPoints = REVENUE_TREND_AREA_POINTS;
}

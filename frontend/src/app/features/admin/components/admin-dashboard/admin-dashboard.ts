import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

interface StatCard {
  label: string;
  value: string;
  icon: string;
  trend: string;
}

interface DestinationRow {
  name: string;
  pct: number;
}

const STATS: StatCard[] = [
  { label: 'Total Trips', value: '248', icon: 'lucidePlane', trend: '+12%' },
  { label: 'Active Users', value: '1,842', icon: 'lucideUsers', trend: '+8%' },
  { label: 'Revenue (MTD)', value: '₹6.4L', icon: 'lucideWallet', trend: '+18%' },
  { label: 'Buses', value: '36', icon: 'lucideBus', trend: '—' },
  { label: 'Hotels', value: '89', icon: 'lucideHotel', trend: '+3' },
  { label: 'Bus Occupancy', value: '82%', icon: 'lucideTrendingUp', trend: '+5%' },
];

const POPULAR_DESTINATIONS: DestinationRow[] = [
  { name: 'Goa', pct: 92 },
  { name: 'Manali', pct: 74 },
  { name: 'Kerala', pct: 68 },
  { name: 'Pondicherry', pct: 55 },
  { name: 'Coorg', pct: 41 },
];

export function bookingBarHeight(i: number): number {
  return 30 + Math.abs(Math.sin(i * 0.7) * 70) + (i % 4) * 5;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard {
  public readonly stats = STATS;
  public readonly destinations = POPULAR_DESTINATIONS;
  public readonly bars = Array.from({ length: 30 }, (_, i) => bookingBarHeight(i));
}

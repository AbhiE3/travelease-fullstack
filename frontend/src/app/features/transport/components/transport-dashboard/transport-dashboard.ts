import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { partnerRoutes, vehicles } from '@app/core/mock-data';

interface RouteOccupancyView {
  id: string;
  route: string;
  departures: number;
  revenue: number;
  occupancy: number;
  toneClass: string;
}

export function occupancyTone(pct: number): string {
  if (pct > 80) return 'bg-success';
  if (pct > 60) return 'bg-primary';
  return 'bg-warning';
}

@Component({
  selector: 'app-transport-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './transport-dashboard.html',
})
export class TransportDashboard {
  public readonly totalBuses = vehicles.length;
  public readonly seatsBooked = '1,284';
  public readonly upcomingTrips = '47';
  public readonly revenueMtd = '₹12.4L';

  public readonly routeOccupancy: RouteOccupancyView[] = partnerRoutes.map((r) => ({
    id: r.id,
    route: r.route,
    departures: r.departures,
    revenue: r.revenue,
    occupancy: r.occupancy,
    toneClass: occupancyTone(r.occupancy),
  }));
}

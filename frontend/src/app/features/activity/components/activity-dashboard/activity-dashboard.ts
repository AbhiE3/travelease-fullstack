import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { providerActivities } from '@app/core/mock-data';

interface OccupancyView {
  id: string;
  name: string;
  booked: number;
  slots: number;
  pct: number;
  toneClass: string;
}

export function occupancyTone(pct: number): string {
  if (pct > 80) return 'bg-success';
  if (pct > 50) return 'bg-primary';
  return 'bg-warning';
}

@Component({
  selector: 'app-activity-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './activity-dashboard.html',
})
export class ActivityDashboard {
  private readonly totalSlots = providerActivities.reduce((s, a) => s + a.slots, 0);
  private readonly booked = providerActivities.reduce((s, a) => s + a.booked, 0);
  private readonly revenue = providerActivities.reduce((s, a) => s + a.booked * a.price, 0);

  public readonly activitiesListed = providerActivities.length;
  public readonly bookingsReceived = this.booked;
  public readonly availableSlots = this.totalSlots - this.booked;
  public readonly revenueMtd = `₹${(this.revenue / 1000).toFixed(0)}k`;

  public readonly occupancy: OccupancyView[] = providerActivities.map((a) => {
    const pct = (a.booked / a.slots) * 100;
    return {
      id: a.id,
      name: a.name,
      booked: a.booked,
      slots: a.slots,
      pct,
      toneClass: occupancyTone(pct),
    };
  });
}

import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { providerActivities } from '@app/core/mock-data';

interface RevenueView {
  id: string;
  name: string;
  revenue: number;
  pct: number;
}

function computeRevenueByActivity(): RevenueView[] {
  const revenues = providerActivities.map((a) => a.booked * a.price);
  const max = Math.max(...revenues);
  return providerActivities.map((a, i) => ({
    id: a.id,
    name: a.name,
    revenue: revenues[i],
    pct: (revenues[i] / max) * 100,
  }));
}

@Component({
  selector: 'app-activity-reports',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './activity-reports.html',
})
export class ActivityReports {
  public readonly revenueByActivity: RevenueView[] = computeRevenueByActivity();
}

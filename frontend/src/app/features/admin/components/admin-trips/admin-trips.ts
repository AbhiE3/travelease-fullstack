import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { trips } from '@app/core/mock-data';

interface TripRow {
  id: string;
  name: string;
  source: string;
  destination: string;
  image: string;
  type: string;
  startDate: string;
  endDate: string;
  members: number;
  status: string;
  budget: number;
}

@Component({
  selector: 'app-admin-trips',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './admin-trips.html',
})
export class AdminTrips {
  public readonly rows: TripRow[] = trips.map((t) => ({
    id: t.id,
    name: t.name,
    source: t.source,
    destination: t.destination,
    image: t.image,
    type: t.type,
    startDate: t.startDate,
    endDate: t.endDate,
    members: t.members,
    status: t.status,
    budget: t.budgetPerPerson * t.members,
  }));
}

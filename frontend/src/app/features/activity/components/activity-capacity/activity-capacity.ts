import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { providerActivities } from '@app/core/mock-data';

const SLOTS = ['08:00', '10:00', '12:00', '14:00', '16:00'];

interface CapacityCell {
  used: number;
  cap: number;
  pct: number;
  toneClass: string;
}

interface CapacityRow {
  id: string;
  name: string;
  total: number;
  cells: CapacityCell[];
}

export function capacityCell(booked: number, slots: number, colIndex: number): CapacityCell {
  const cap = Math.max(2, Math.floor(slots / 5));
  const used = Math.min(cap, Math.floor((booked / slots) * cap) + (colIndex % 2));
  const pct = (used / cap) * 100;
  return { used, cap, pct, toneClass: pct > 80 ? 'bg-success' : 'bg-primary' };
}

@Component({
  selector: 'app-activity-capacity',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './activity-capacity.html',
})
export class ActivityCapacity {
  public readonly slots = SLOTS;
  public readonly rows: CapacityRow[] = providerActivities.map((a) => ({
    id: a.id,
    name: a.name,
    total: a.slots,
    cells: SLOTS.map((_, i) => capacityCell(a.booked, a.slots, i)),
  }));
}

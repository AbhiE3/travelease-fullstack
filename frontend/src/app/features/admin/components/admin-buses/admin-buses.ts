import { Component, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { buses } from '@app/core/mock-data';

const STATUS_BY_INDEX = ['On Time', 'Delayed', 'On Time'];

export function busStatus(i: number): string {
  return STATUS_BY_INDEX[i] ?? 'On Time';
}

export function busStatusClass(status: string): string {
  return status === 'Delayed'
    ? 'bg-destructive/15 text-destructive border-destructive/20'
    : 'bg-success/15 text-success border-success/20';
}

interface BusRow {
  id: string;
  name: string;
  operator: string;
  seats: number;
  price: number;
  rating: number;
  status: string;
  statusClass: string;
}

@Component({
  selector: 'app-admin-buses',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-buses.html',
})
export class AdminBuses {
  public readonly rows: BusRow[] = buses.map((b, i) => {
    const status = busStatus(i);
    return {
      id: b.id,
      name: b.name,
      operator: b.operator,
      seats: b.seats,
      price: b.price,
      rating: b.rating,
      status,
      statusClass: busStatusClass(status),
    };
  });

  public readonly approvedIds = signal<ReadonlySet<string>>(new Set());

  public approve(id: string): void {
    this.approvedIds.update((ids) => new Set(ids).add(id));
  }
}

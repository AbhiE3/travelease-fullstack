import { Component, computed, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

export interface Partner {
  id: string;
  name: string;
  city: string;
  bookings: number;
  cancellation: number;
  rating: number;
  revenue: number;
  status: string;
}

export function cancellationClass(cancellation: number): string {
  return cancellation > 7
    ? 'bg-destructive/10 text-destructive border-destructive/20'
    : 'bg-success/10 text-success border-success/20';
}

export function partnerBadgeStatus(status: string): string {
  return status === 'Active' ? 'Accepted' : 'Pending';
}

interface PartnerRow extends Partner {
  rank: number;
  cancellationClass: string;
  badgeStatus: string;
}

@Component({
  selector: 'app-partner-ranking-table',
  imports: [NgIcon, HlmCardImports, HlmBadgeImports, StatusBadge],
  templateUrl: './partner-ranking-table.html',
})
export class PartnerRankingTable {
  public readonly data = input.required<readonly Partner[]>();
  public readonly label = input.required<string>();

  public readonly sorted = computed<PartnerRow[]>(() =>
    [...this.data()]
      .sort((a, b) => b.revenue - a.revenue)
      .map((p, i) => ({
        ...p,
        rank: i + 1,
        cancellationClass: cancellationClass(p.cancellation),
        badgeStatus: partnerBadgeStatus(p.status),
      })),
  );

  public readonly top = computed(() => this.sorted()[0]);
  public readonly needsAttention = computed(
    () => [...this.data()].sort((a, b) => b.cancellation - a.cancellation)[0],
  );
}

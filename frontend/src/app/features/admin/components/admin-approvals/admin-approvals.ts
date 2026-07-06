import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { pendingApprovals } from '@app/core/mock-data';

export function iconForApprovalType(type: string): string {
  if (type === 'Hotel') return 'lucideHotel';
  if (type === 'Transport') return 'lucideBus';
  return 'lucideActivity';
}

@Component({
  selector: 'app-admin-approvals',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, PageHeader],
  templateUrl: './admin-approvals.html',
})
export class AdminApprovals {
  public readonly approvals = pendingApprovals.map((p) => ({
    ...p,
    icon: iconForApprovalType(p.type),
  }));

  public readonly pendingCount = pendingApprovals.length;
  public readonly hotelCount = pendingApprovals.filter((p) => p.type === 'Hotel').length;
  public readonly transportCount = pendingApprovals.filter((p) => p.type === 'Transport').length;
  public readonly activityCount = pendingApprovals.filter((p) => p.type === 'Activity').length;
}

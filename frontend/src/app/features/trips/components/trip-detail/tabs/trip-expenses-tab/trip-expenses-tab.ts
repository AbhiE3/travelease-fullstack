import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { expenses, members } from '@app/core/mock-data';

interface Settlement {
  from: string;
  to: string;
  amount: number;
}

const PENDING_SETTLEMENTS: Settlement[] = [
  { from: 'Priya', to: 'You', amount: 2700 },
  { from: 'Neha', to: 'You', amount: 2700 },
  { from: 'You', to: 'Raj', amount: 1400 },
];

@Component({
  selector: 'app-trip-expenses-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
    StatusBadge,
  ],
  templateUrl: './trip-expenses-tab.html',
})
export class TripExpensesTab {
  public readonly expenses = expenses;
  protected readonly members = members;
  protected readonly defaultPaidBy = members[0].name;
  protected readonly pendingSettlements = PENDING_SETTLEMENTS;
}

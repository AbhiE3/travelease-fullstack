import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { expenses } from '@app/core/mock-data';

@Component({
  selector: 'app-expense-list',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader, StatusBadge],
  templateUrl: './expense-list.html',
})
export class ExpenseList {
  public readonly expenses = expenses;
}

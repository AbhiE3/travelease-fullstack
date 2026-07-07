import { Component, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-hotels',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-hotels.html',
})
export class AdminHotels {
  public readonly hotels = hotels;

  public readonly approvedIds = signal<ReadonlySet<string>>(new Set());

  public approve(id: string): void {
    this.approvedIds.update((ids) => new Set(ids).add(id));
  }
}

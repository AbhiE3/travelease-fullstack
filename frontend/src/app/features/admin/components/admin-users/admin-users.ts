import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { members } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-users',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmInputImports,
    HlmAvatarImports,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-users.html',
})
export class AdminUsers {
  public readonly rows = [...members, ...members].map((m, i) => ({ ...m, id: `${m.id}-${i}` }));
}

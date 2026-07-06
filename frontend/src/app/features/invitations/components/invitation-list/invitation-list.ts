import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { invitations } from '@app/core/mock-data';

@Component({
  selector: 'app-invitation-list',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './invitation-list.html',
})
export class InvitationList {
  public readonly invitations = invitations;
}

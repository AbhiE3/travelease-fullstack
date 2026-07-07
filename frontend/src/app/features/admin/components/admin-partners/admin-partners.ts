import { Component, signal } from '@angular/core';
import { HlmTabsImports } from '@spartan-ng/helm/tabs';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { PartnerRankingTable } from './partner-ranking-table/partner-ranking-table';
import { activityPartners, hotelPartners, transportPartners } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-partners',
  imports: [HlmTabsImports, PageHeader, PartnerRankingTable],
  templateUrl: './admin-partners.html',
})
export class AdminPartners {
  protected readonly activeTab = signal('hotels');

  public readonly hotelPartners = hotelPartners;
  public readonly transportPartners = transportPartners;
  public readonly activityPartners = activityPartners;
}

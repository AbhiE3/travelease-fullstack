import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { notifications } from '@app/core/mock-data';

const ICON_FOR: Record<string, string> = {
  invitation: 'lucideMail',
  expense: 'lucideWallet',
  budget: 'lucideAlertTriangle',
  delay: 'lucideBellRing',
  booking: 'lucideCheckCircle2',
};

export function iconForNotificationType(type: string): string {
  return ICON_FOR[type] ?? 'lucideBellRing';
}

interface NotificationView {
  id: string;
  title: string;
  desc: string;
  time: string;
  icon: string;
}

@Component({
  selector: 'app-notification-list',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './notification-list.html',
})
export class NotificationList {
  public readonly notificationViews: NotificationView[] = notifications.map((n) => ({
    ...n,
    icon: iconForNotificationType(n.type),
  }));
}

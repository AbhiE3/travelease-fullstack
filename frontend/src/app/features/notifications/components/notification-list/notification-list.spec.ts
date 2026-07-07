import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideBellRing,
  lucideCheckCircle2,
  lucideMail,
  lucideWallet,
} from '@ng-icons/lucide';
import { notifications } from '@app/core/mock-data';
import {
  NotificationList,
  iconForNotificationType,
} from '@app/features/notifications/components/notification-list/notification-list';

describe('iconForNotificationType', () => {
  it('maps a known type to its icon', () => {
    expect(iconForNotificationType('invitation')).toBe('lucideMail');
  });

  it('falls back to lucideBellRing for an unmapped type', () => {
    expect(iconForNotificationType('unknown')).toBe('lucideBellRing');
  });
});

describe('NotificationList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationList],
      providers: [
        provideIcons({ lucideAlertTriangle, lucideBellRing, lucideCheckCircle2, lucideMail, lucideWallet }),
      ],
    }).compileComponents();
  });

  it('renders every notification title', () => {
    const fixture = TestBed.createComponent(NotificationList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const n of notifications) {
      expect(text).toContain(n.title);
    }
  });
});

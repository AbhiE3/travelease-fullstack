import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBus,
  lucideCheck,
  lucideFileText,
  lucideHotel,
  lucideX,
} from '@ng-icons/lucide';
import { pendingApprovals } from '@app/core/mock-data';
import {
  AdminApprovals,
  iconForApprovalType,
} from '@app/features/admin/components/admin-approvals/admin-approvals';

describe('iconForApprovalType', () => {
  it('maps Hotel, Transport, and Activity to their icons', () => {
    expect(iconForApprovalType('Hotel')).toBe('lucideHotel');
    expect(iconForApprovalType('Transport')).toBe('lucideBus');
    expect(iconForApprovalType('Activity')).toBe('lucideActivity');
  });
});

describe('AdminApprovals', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminApprovals],
      providers: [
        provideIcons({ lucideActivity, lucideBus, lucideCheck, lucideFileText, lucideHotel, lucideX }),
      ],
    }).compileComponents();
  });

  it('computes all 4 stat counts from pendingApprovals', () => {
    const fixture = TestBed.createComponent(AdminApprovals);
    const c = fixture.componentInstance;
    expect(c.pendingCount).toBe(pendingApprovals.length);
    expect(c.hotelCount).toBe(pendingApprovals.filter((p) => p.type === 'Hotel').length);
    expect(c.transportCount).toBe(pendingApprovals.filter((p) => p.type === 'Transport').length);
    expect(c.activityCount).toBe(pendingApprovals.filter((p) => p.type === 'Activity').length);
  });

  it('renders every approval name and city', () => {
    const fixture = TestBed.createComponent(AdminApprovals);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const p of pendingApprovals) {
      expect(text).toContain(p.name);
      expect(text).toContain(p.city);
    }
  });
});

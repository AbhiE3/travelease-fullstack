import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideUserPlus } from '@ng-icons/lucide';
import { members } from '@app/core/mock-data';
import { TripMembersTab } from '@app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab';

describe('TripMembersTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripMembersTab],
      providers: [provideIcons({ lucideUserPlus })],
    }).compileComponents();
  });

  it('renders every member from mock data', () => {
    const fixture = TestBed.createComponent(TripMembersTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const m of members) {
      expect(text).toContain(m.name);
      expect(text).toContain(m.email);
    }
  });

  it('shows the Invite Member dialog trigger', () => {
    const fixture = TestBed.createComponent(TripMembersTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Invite Member');
  });
});

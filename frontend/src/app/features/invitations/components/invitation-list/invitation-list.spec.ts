import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucideUsers } from '@ng-icons/lucide';
import { invitations } from '@app/core/mock-data';
import { InvitationList } from '@app/features/invitations/components/invitation-list/invitation-list';

describe('InvitationList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvitationList],
      providers: [provideIcons({ lucideCalendar, lucideMapPin, lucideUsers })],
    }).compileComponents();
  });

  it('renders every invitation trip name and organizer', () => {
    const fixture = TestBed.createComponent(InvitationList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const inv of invitations) {
      expect(text).toContain(inv.trip);
      expect(text).toContain(inv.organizer);
    }
  });

  it('shows "Goa" on every card regardless of the invitation', () => {
    const fixture = TestBed.createComponent(InvitationList);
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('[hlmCard]');
    expect(cards.length).toBe(invitations.length);
    for (const card of Array.from(cards)) {
      const spans = card.querySelectorAll('span');
      const destinationSpan = spans[spans.length - 1];
      expect(destinationSpan.textContent).toContain('Goa');
    }
  });
});

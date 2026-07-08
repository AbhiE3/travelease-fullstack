import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { InvitationList } from '@app/features/invitations/components/invitation-list/invitation-list';
import { TripsService } from '@app/features/trips/services/trips.service';
import { PendingInvitation } from '@app/features/trips/services/trip.models';

const SAMPLE_INVITATIONS: PendingInvitation[] = [
  {
    tripMemberId: 'dddddddd-0000-0000-0000-000000000004',
    tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
    tripName: 'Goa Beach Escape',
    organizer: { userId: 'u1', name: 'Alice Traveler', email: 'alice@travelease.test' },
    sourceLocation: 'Bengaluru',
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    memberStatus: 'INVITED',
  },
];

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [InvitationList],
    providers: [
      provideIcons({ lucideCalendar, lucideMapPin }),
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(InvitationList);
  fixture.detectChanges();
  return fixture;
}

describe('InvitationList', () => {
  it('shows a loading message before invitations arrive', async () => {
    const subject = new Subject<PendingInvitation[]>();
    const fixture = await setup({ getPendingInvitations: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading');
  });

  it('shows an empty-state message when there are no invitations', async () => {
    const fixture = await setup({ getPendingInvitations: () => of([]) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No pending invitations');
  });

  it('shows an error message when the request fails', async () => {
    const fixture = await setup({ getPendingInvitations: () => throwError(() => new Error('boom')) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });

  it('renders every invitation with real fields', async () => {
    const fixture = await setup({ getPendingInvitations: () => of(SAMPLE_INVITATIONS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Goa Beach Escape');
    expect(el.textContent).toContain('Alice Traveler');
    expect(el.textContent).toContain('Bengaluru');
  });

  it('accepts an invitation and removes the card', async () => {
    const acceptInvitation = vi.fn().mockReturnValue(of(SAMPLE_INVITATIONS[0]));
    const fixture = await setup({ getPendingInvitations: () => of(SAMPLE_INVITATIONS), acceptInvitation });
    const el = fixture.nativeElement as HTMLElement;

    const acceptBtn = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Accept',
    )!;
    acceptBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(acceptInvitation).toHaveBeenCalledWith(
      SAMPLE_INVITATIONS[0].tripId,
      SAMPLE_INVITATIONS[0].tripMemberId,
    );
    expect(el.textContent).not.toContain('Goa Beach Escape');
  });

  it('declines an invitation and removes the card', async () => {
    const rejectInvitation = vi.fn().mockReturnValue(of(SAMPLE_INVITATIONS[0]));
    const fixture = await setup({ getPendingInvitations: () => of(SAMPLE_INVITATIONS), rejectInvitation });
    const el = fixture.nativeElement as HTMLElement;

    const declineBtn = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Decline',
    )!;
    declineBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(rejectInvitation).toHaveBeenCalledWith(
      SAMPLE_INVITATIONS[0].tripId,
      SAMPLE_INVITATIONS[0].tripMemberId,
    );
    expect(el.textContent).not.toContain('Goa Beach Escape');
  });
});

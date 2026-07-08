import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideUserPlus } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { TripMembersTab } from '@app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab';
import { TripsService } from '@app/features/trips/services/trips.service';
import { TripMember } from '@app/features/trips/services/trip.models';
import { UsersService } from '@app/core/users/users.service';

const TRIP_ID = 'aaaaaaaa-0000-0000-0000-000000000001';

const SAMPLE_MEMBERS: TripMember[] = [
  {
    tripMemberId: 'cccccccc-0000-0000-0000-000000000003',
    userId: 'u2',
    name: 'Bob Traveler',
    email: 'bob@travelease.test',
    memberStatus: 'ACCEPTED',
    joinedDate: '2026-07-01T00:00:00Z',
    budgetAmount: 5000,
    spentAmount: 0,
  },
];

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [TripMembersTab],
    providers: [
      provideIcons({ lucideUserPlus }),
      { provide: ActivatedRoute, useValue: { paramMap: of(new Map([['tripId', TRIP_ID]])) } },
      { provide: TripsService, useValue: tripsService },
      { provide: UsersService, useValue: { searchTravelers: () => of([]) } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(TripMembersTab);
  fixture.detectChanges();
  return fixture;
}

describe('TripMembersTab', () => {
  it('shows a loading message before members arrive', async () => {
    const subject = new Subject<TripMember[]>();
    const fixture = await setup({ getTripMembers: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading members');
  });

  it('renders every member with real fields', async () => {
    const fixture = await setup({ getTripMembers: () => of(SAMPLE_MEMBERS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Bob Traveler');
    expect(el.textContent).toContain('bob@travelease.test');
  });

  it('shows an error message when loading members fails', async () => {
    const fixture = await setup({ getTripMembers: () => throwError(() => new Error('boom')) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });

  it('calls getTripMembers with the tripId from the route', async () => {
    const getTripMembers = vi.fn().mockReturnValue(of(SAMPLE_MEMBERS));
    await setup({ getTripMembers });
    expect(getTripMembers).toHaveBeenCalledWith(TRIP_ID);
  });

  it('invites a member by email and appends it to the list', async () => {
    const newMember: TripMember = {
      tripMemberId: 'eeeeeeee-0000-0000-0000-000000000005',
      userId: 'u3',
      name: 'Cara Traveler',
      email: 'cara@travelease.test',
      memberStatus: 'INVITED',
      joinedDate: '2026-07-02T00:00:00Z',
      budgetAmount: 0,
      spentAmount: 0,
    };
    const inviteMember = vi.fn().mockReturnValue(of(newMember));
    const fixture = await setup({ getTripMembers: () => of(SAMPLE_MEMBERS), inviteMember });

    const fakeEvent = { preventDefault: () => {} } as Event;
    // Dialog content renders via a CDK Overlay attached to document.body, not
    // fixture.nativeElement, and no existing spec in this codebase drives that
    // interaction — calling the (protected) handler directly tests the same
    // service-call + list-update logic without a brittle, unproven overlay-query.
    (fixture.componentInstance as unknown as { onInvite: (e: Event, email: string) => void }).onInvite(
      fakeEvent,
      'cara@travelease.test',
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(inviteMember).toHaveBeenCalledWith(TRIP_ID, 'cara@travelease.test');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Cara Traveler');
  });

  it('removes a member from the list on success', async () => {
    const removeMember = vi.fn().mockReturnValue(of(undefined));
    const fixture = await setup({ getTripMembers: () => of(SAMPLE_MEMBERS), removeMember });
    const el = fixture.nativeElement as HTMLElement;

    const removeBtn = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Remove',
    )!;
    removeBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(removeMember).toHaveBeenCalledWith(TRIP_ID, SAMPLE_MEMBERS[0].tripMemberId);
    expect(el.textContent).not.toContain('Bob Traveler');
  });
});

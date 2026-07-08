import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TripsService } from '@app/features/trips/services/trips.service';
import {
  CreateTripPayload,
  PendingInvitation,
  Trip,
  TripMember,
} from '@app/features/trips/services/trip.models';

const SAMPLE_TRIP: Trip = {
  tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
  tripName: 'Goa Trip',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Mumbai',
  destinationId: 3,
  budgetAmount: 15000,
  categoryId: 1,
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  status: 'PLANNING',
  viewerRole: 'ORGANIZER',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
};

const SAMPLE_MEMBER: TripMember = {
  tripMemberId: 'cccccccc-0000-0000-0000-000000000003',
  userId: 'u2',
  name: 'Bob',
  email: 'bob@travelease.test',
  memberStatus: 'ACCEPTED',
  joinedDate: '2026-07-01T00:00:00Z',
  budgetAmount: 5000,
  spentAmount: 0,
};

const SAMPLE_INVITATION: PendingInvitation = {
  tripMemberId: 'dddddddd-0000-0000-0000-000000000004',
  tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
  tripName: 'Goa Trip',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Mumbai',
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  memberStatus: 'INVITED',
};

describe('TripsService', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    return {
      service: TestBed.inject(TripsService),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('fetches and unwraps the trip list', async () => {
    const { service, httpMock } = await setup();

    let result: Trip[] | undefined;
    service.listMyTrips().subscribe((trips) => (result = trips));

    const req = httpMock.expectOne('http://localhost:8080/api/trips');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [SAMPLE_TRIP], message: 'Trips retrieved', error: null });

    expect(result).toEqual([SAMPLE_TRIP]);
  });

  it('propagates an HTTP error', async () => {
    const { service, httpMock } = await setup();

    let errored = false;
    service.listMyTrips().subscribe({ error: () => (errored = true) });

    const req = httpMock.expectOne('http://localhost:8080/api/trips');
    req.flush(
      { success: false, data: null, message: null, error: { code: 'SERVER_ERROR', message: 'boom' } },
      { status: 500, statusText: 'Server Error' },
    );

    expect(errored).toBe(true);
  });

  it('creates a trip and unwraps the response', async () => {
    const { service, httpMock } = await setup();

    const payload: CreateTripPayload = {
      tripName: 'Goa Trip',
      sourceLocation: 'Mumbai',
      destinationId: 3,
      budgetAmount: 15000,
      categoryId: 4,
      startDate: '2026-08-01',
      endDate: '2026-08-05',
    };

    let result: Trip | undefined;
    service.createTrip(payload).subscribe((trip) => (result = trip));

    const req = httpMock.expectOne('http://localhost:8080/api/trips');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: SAMPLE_TRIP, message: 'Trip created successfully', error: null });

    expect(result).toEqual(SAMPLE_TRIP);
  });

  it('fetches and unwraps pending invitations', async () => {
    const { service, httpMock } = await setup();

    let result: PendingInvitation[] | undefined;
    service.getPendingInvitations().subscribe((invites) => (result = invites));

    const req = httpMock.expectOne('http://localhost:8080/api/trips/invitations');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [SAMPLE_INVITATION], message: 'ok', error: null });

    expect(result).toEqual([SAMPLE_INVITATION]);
  });

  it('fetches and unwraps trip members', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember[] | undefined;
    service.getTripMembers('aaaaaaaa-0000-0000-0000-000000000001').subscribe((members) => (result = members));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [SAMPLE_MEMBER], message: 'ok', error: null });

    expect(result).toEqual([SAMPLE_MEMBER]);
  });

  it('invites a member by email', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember | undefined;
    service
      .inviteMember('aaaaaaaa-0000-0000-0000-000000000001', 'bob@travelease.test')
      .subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members',
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'bob@travelease.test' });
    req.flush({ success: true, data: SAMPLE_MEMBER, message: 'ok', error: null });

    expect(result).toEqual(SAMPLE_MEMBER);
  });

  it('accepts an invitation', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember | undefined;
    service
      .acceptInvitation('aaaaaaaa-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000003')
      .subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members/cccccccc-0000-0000-0000-000000000003/accept',
    );
    expect(req.request.method).toBe('PATCH');
    req.flush({
      success: true,
      data: { ...SAMPLE_MEMBER, memberStatus: 'ACCEPTED' },
      message: 'ok',
      error: null,
    });

    expect(result?.memberStatus).toBe('ACCEPTED');
  });

  it('rejects an invitation', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember | undefined;
    service
      .rejectInvitation('aaaaaaaa-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000003')
      .subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members/cccccccc-0000-0000-0000-000000000003/reject',
    );
    expect(req.request.method).toBe('PATCH');
    req.flush({
      success: true,
      data: { ...SAMPLE_MEMBER, memberStatus: 'REJECTED' },
      message: 'ok',
      error: null,
    });

    expect(result?.memberStatus).toBe('REJECTED');
  });

  it('removes a member', async () => {
    const { service, httpMock } = await setup();

    let completed = false;
    service
      .removeMember('aaaaaaaa-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000003')
      .subscribe({ complete: () => (completed = true) });

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members/cccccccc-0000-0000-0000-000000000003',
    );
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, data: null, message: 'ok', error: null });

    expect(completed).toBe(true);
  });
});

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
import {
  CreateTripPayload,
  PendingInvitation,
  Trip,
  TripMember,
} from '@app/features/trips/services/trip.models';

@Injectable({ providedIn: 'root' })
export class TripsService {
  private readonly http = inject(HttpClient);

  listMyTrips(): Observable<Trip[]> {
    return this.http
      .get<ApiResponse<Trip[]>>(`${API_BASE_URL}/api/trips`)
      .pipe(map((response) => response.data));
  }

  createTrip(payload: CreateTripPayload): Observable<Trip> {
    return this.http
      .post<ApiResponse<Trip>>(`${API_BASE_URL}/api/trips`, payload)
      .pipe(map((response) => response.data));
  }

  getPendingInvitations(): Observable<PendingInvitation[]> {
    return this.http
      .get<ApiResponse<PendingInvitation[]>>(`${API_BASE_URL}/api/trips/invitations`)
      .pipe(map((response) => response.data));
  }

  getTripMembers(tripId: string): Observable<TripMember[]> {
    return this.http
      .get<ApiResponse<TripMember[]>>(`${API_BASE_URL}/api/trips/${tripId}/members`)
      .pipe(map((response) => response.data));
  }

  inviteMember(tripId: string, email: string): Observable<TripMember> {
    return this.http
      .post<ApiResponse<TripMember>>(`${API_BASE_URL}/api/trips/${tripId}/members`, { email })
      .pipe(map((response) => response.data));
  }

  acceptInvitation(tripId: string, tripMemberId: string): Observable<TripMember> {
    return this.http
      .patch<ApiResponse<TripMember>>(
        `${API_BASE_URL}/api/trips/${tripId}/members/${tripMemberId}/accept`,
        {},
      )
      .pipe(map((response) => response.data));
  }

  rejectInvitation(tripId: string, tripMemberId: string): Observable<TripMember> {
    return this.http
      .patch<ApiResponse<TripMember>>(
        `${API_BASE_URL}/api/trips/${tripId}/members/${tripMemberId}/reject`,
        {},
      )
      .pipe(map((response) => response.data));
  }

  removeMember(tripId: string, tripMemberId: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${API_BASE_URL}/api/trips/${tripId}/members/${tripMemberId}`)
      .pipe(map(() => undefined));
  }
}

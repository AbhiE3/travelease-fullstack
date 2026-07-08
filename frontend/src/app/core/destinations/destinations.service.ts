import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
import { Destination } from '@app/core/destinations/destination.models';

@Injectable({ providedIn: 'root' })
export class DestinationsService {
  private readonly http = inject(HttpClient);

  listDestinations(): Observable<Destination[]> {
    return this.http
      .get<ApiResponse<Destination[]>>(`${API_BASE_URL}/api/destinations`)
      .pipe(map((response) => response.data));
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
import { TravelerSearchResult } from '@app/core/users/user-search.model';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);

  searchTravelers(query: string): Observable<TravelerSearchResult[]> {
    return this.http
      .get<ApiResponse<TravelerSearchResult[]>>(`${API_BASE_URL}/api/users/search`, {
        params: new HttpParams().set('q', query),
      })
      .pipe(map((response) => response.data));
  }
}

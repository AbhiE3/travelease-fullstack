import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WorkspaceSearchService {
  private readonly hotelQuerySubject = new BehaviorSubject<string>('');

  readonly hotelQuery$ = this.hotelQuerySubject.asObservable();

  setHotelQuery(query: string): void {
    this.hotelQuerySubject.next(query.trimStart());
  }

  clearHotelQuery(): void {
    this.hotelQuerySubject.next('');
  }
}

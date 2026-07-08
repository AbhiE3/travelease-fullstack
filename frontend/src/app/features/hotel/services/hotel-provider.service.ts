import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { API_BASE_URL } from '@app/core/auth/api.config';
import { ApiResponse } from '@app/core/auth/auth.models';

export interface HotelResponse {
  hotelId: string;
  destinationId: number;
  hotelName: string;
  address: string;
  rating: number | string | null;
  pricePerNight: number | string;
  amenities: string | null;
  status: string;
  policies: string | null;
}

export interface RoomResponse {
  roomId: string;
  hotelId: string;
  roomType: string;
  capacity: number;
  bedType: string;
  pricePerNight: number | string;
  availabilityStatus: string;
}

export interface HotelBookingResponse {
  hotelBookingId: string;
  tripId: string | null;
  hotelId: string;
  hotelName: string;
  bookedByUserId: string | null;
  bookedByUserName?: string | null;
  checkInDate: string;
  checkOutDate: string;
  roomType: string;
  roomNumber: string | null;
  totalAmount: number | string;
  bookingStatus: string;
}

export interface HotelReviewResponse {
  reviewId: string;
  hotelId: string;
  userId: string;
  userName: string;
  rating: number | string;
  comment: string | null;
  createdAt?: string | null;
}

export interface HotelRequest {
  destinationId: number;
  hotelName: string;
  address: string;
  rating: number | null;
  pricePerNight: number;
  amenities: string | null;
  status: string;
}

export interface RoomRequest {
  roomType: string;
  capacity: number;
  bedType: string;
  pricePerNight: number;
  availabilityStatus: string;
}

export interface ProviderOverview {
  hotels: HotelResponse[];
  rooms: RoomResponse[];
  bookings: HotelBookingResponse[];
  reviews: HotelReviewResponse[];
}

export const EMPTY_PROVIDER_OVERVIEW: ProviderOverview = {
  hotels: [],
  rooms: [],
  bookings: [],
  reviews: [],
};

@Injectable({ providedIn: 'root' })
export class HotelProviderService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly providerDataRefresh = new BehaviorSubject<void>(void 0);

  getHotels(): Observable<HotelResponse[]> {
    return this.unwrap(this.http.get<ApiResponse<HotelResponse[]>>(`${this.apiBaseUrl}/provider/hotels`));
  }

  createHotel(request: HotelRequest): Observable<HotelResponse> {
    return this.unwrap(
      this.http.post<ApiResponse<HotelResponse>>(`${this.apiBaseUrl}/provider/hotels`, request),
    );
  }

  updateHotel(hotelId: string, request: HotelRequest): Observable<HotelResponse> {
    return this.unwrap(
      this.http.put<ApiResponse<HotelResponse>>(`${this.apiBaseUrl}/provider/hotels/${hotelId}`, request),
    );
  }

  getHotelDetails(hotelId: string): Observable<HotelResponse> {
    return this.unwrap(
      this.http.get<ApiResponse<{ hotel: HotelResponse }>>(`${this.apiBaseUrl}/provider/hotels/${hotelId}`),
    ).pipe(map((details) => details.hotel));
  }

  getInventory(): Observable<RoomResponse[]> {
    return this.unwrap(this.http.get<ApiResponse<RoomResponse[]>>(`${this.apiBaseUrl}/provider/inventory`));
  }

  createRoom(hotelId: string, request: RoomRequest): Observable<RoomResponse> {
    return this.unwrap(
      this.http.post<ApiResponse<RoomResponse>>(`${this.apiBaseUrl}/provider/hotels/${hotelId}/rooms`, request),
    );
  }

  getProviderBookings(): Observable<HotelBookingResponse[]> {
    return this.unwrap(
      this.http.get<ApiResponse<HotelBookingResponse[]>>(`${this.apiBaseUrl}/provider/hotel-bookings`),
    );
  }

  getHotelReviews(hotelId: string): Observable<HotelReviewResponse[]> {
    return this.unwrap(
      this.http.get<ApiResponse<HotelReviewResponse[]>>(`${this.apiBaseUrl}/hotels/${hotelId}/reviews`),
    );
  }

  checkIn(bookingId: string): Observable<HotelBookingResponse> {
    return this.unwrap(
      this.http.put<ApiResponse<HotelBookingResponse>>(
        `${this.apiBaseUrl}/provider/hotel-bookings/${bookingId}/check-in`,
        {},
      ),
    );
  }

  checkOut(bookingId: string): Observable<HotelBookingResponse> {
    return this.unwrap(
      this.http.put<ApiResponse<HotelBookingResponse>>(
        `${this.apiBaseUrl}/provider/hotel-bookings/${bookingId}/check-out`,
        {},
      ),
    );
  }

  getProviderOverview(): Observable<ProviderOverview> {
    return this.providerDataRefresh.pipe(switchMap(() => this.fetchProviderOverview()));
  }

  refreshProviderData(): void {
    this.providerDataRefresh.next();
  }

  private fetchProviderOverview(): Observable<ProviderOverview> {
    return this.getHotels().pipe(
      switchMap((hotels) =>
        forkJoin({
          hotels: of(hotels),
          rooms: this.getInventory().pipe(catchError(() => of([]))),
          bookings: this.getProviderBookings().pipe(catchError(() => of([]))),
          reviews: this.getReviewsForHotels(hotels).pipe(catchError(() => of([]))),
        }),
      ),
    );
  }

  private getReviewsForHotels(hotels: HotelResponse[]): Observable<HotelReviewResponse[]> {
    if (hotels.length === 0) {
      return of([]);
    }

    return forkJoin(
      hotels.map((hotel) => this.getHotelReviews(hotel.hotelId).pipe(catchError(() => of([])))),
    ).pipe(map((reviewGroups) => reviewGroups.flat()));
  }

  private unwrap<T>(request: Observable<ApiResponse<T>>): Observable<T> {
    return request.pipe(
      map((response) => {
        if (!response.success || response.data === null) {
          throw new Error(response.error?.message ?? 'Hotel provider request failed');
        }

        return response.data;
      }),
    );
  }
}

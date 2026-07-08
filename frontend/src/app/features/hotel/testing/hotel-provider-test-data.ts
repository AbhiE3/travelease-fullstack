import { of } from 'rxjs';

import {
  HotelProviderService,
  ProviderOverview,
} from '@app/features/hotel/services/hotel-provider.service';

function dateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const today = new Date();
const tomorrow = new Date(today);
tomorrow.setDate(today.getDate() + 1);
const nextWeek = new Date(today);
nextWeek.setDate(today.getDate() + 7);
const nextWeekCheckout = new Date(today);
nextWeekCheckout.setDate(today.getDate() + 9);

export const TEST_PROVIDER_OVERVIEW: ProviderOverview = {
  hotels: [
    {
      hotelId: 'hotel-1',
      destinationId: 1,
      hotelName: 'Sea Breeze Resort',
      address: 'Baga Beach, Goa',
      rating: 4.7,
      pricePerNight: 4800,
      amenities: 'Pool, Breakfast, Wifi',
      status: 'ACTIVE',
      policies: null,
    },
  ],
  rooms: [
    {
      roomId: 'room-1',
      hotelId: 'hotel-1',
      roomType: 'Deluxe Sea View',
      capacity: 2,
      bedType: 'King',
      pricePerNight: 4800,
      availabilityStatus: 'AVAILABLE',
    },
    {
      roomId: 'room-2',
      hotelId: 'hotel-1',
      roomType: 'Deluxe Sea View',
      capacity: 2,
      bedType: 'King',
      pricePerNight: 4800,
      availabilityStatus: 'BOOKED',
    },
    {
      roomId: 'room-3',
      hotelId: 'hotel-1',
      roomType: 'Family Suite',
      capacity: 4,
      bedType: 'Queen',
      pricePerNight: 8200,
      availabilityStatus: 'AVAILABLE',
    },
    {
      roomId: 'room-4',
      hotelId: 'hotel-1',
      roomType: 'Standard Double',
      capacity: 2,
      bedType: 'Double',
      pricePerNight: 3600,
      availabilityStatus: 'MAINTENANCE',
    },
  ],
  bookings: [
    {
      hotelBookingId: 'booking-1',
      tripId: 'trip-1',
      hotelId: 'hotel-1',
      hotelName: 'Sea Breeze Resort',
      bookedByUserId: 'user-1',
      bookedByUserName: 'Sarathy R',
      checkInDate: dateKey(today),
      checkOutDate: dateKey(tomorrow),
      roomType: 'Deluxe Sea View',
      roomNumber: '201',
      totalAmount: 19200,
      bookingStatus: 'CONFIRMED',
    },
    {
      hotelBookingId: 'booking-2',
      tripId: 'trip-2',
      hotelId: 'hotel-1',
      hotelName: 'Sea Breeze Resort',
      bookedByUserId: 'user-2',
      bookedByUserName: 'Anjali V',
      checkInDate: dateKey(nextWeek),
      checkOutDate: dateKey(nextWeekCheckout),
      roomType: 'Family Suite',
      roomNumber: null,
      totalAmount: 32800,
      bookingStatus: 'PENDING',
    },
  ],
  reviews: [
    {
      reviewId: 'review-1',
      hotelId: 'hotel-1',
      userId: 'user-1',
      userName: 'Sarathy R',
      rating: 5,
      comment: 'Stunning sea view, spotless rooms, attentive staff.',
      createdAt: `${dateKey(today)}T09:00:00`,
    },
    {
      reviewId: 'review-2',
      hotelId: 'hotel-1',
      userId: 'user-2',
      userName: 'Anjali V',
      rating: 4,
      comment: 'Great location near Baga.',
      createdAt: `${dateKey(today)}T10:00:00`,
    },
    {
      reviewId: 'review-3',
      hotelId: 'hotel-1',
      userId: 'user-3',
      userName: 'Raj Patel',
      rating: 5,
      comment: 'Perfect for a group of 6.',
      createdAt: `${dateKey(today)}T11:00:00`,
    },
    {
      reviewId: 'review-4',
      hotelId: 'hotel-1',
      userId: 'user-4',
      userName: 'Priya Sharma',
      rating: 3,
      comment: 'Decent stay but the wifi was patchy.',
      createdAt: `${dateKey(today)}T12:00:00`,
    },
  ],
};

export function createHotelProviderStub(
  overview = TEST_PROVIDER_OVERVIEW,
): Partial<HotelProviderService> {
  return {
    getHotels: () => of(overview.hotels),
    getProviderOverview: () => of(overview),
    getInventory: () => of(overview.rooms),
    getProviderBookings: () => of(overview.bookings),
    getHotelReviews: () => of(overview.reviews),
    createHotel: () => of(overview.hotels[0]),
    updateHotel: () => of(overview.hotels[0]),
    createRoom: () => of(overview.rooms[0]),
    refreshProviderData: () => undefined,
  };
}

import {
  HotelBookingResponse,
  HotelResponse,
  HotelReviewResponse,
  ProviderOverview,
  RoomResponse,
} from './hotel-provider.service';

const HOTEL_IMAGES = [
  'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=70',
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=70',
  'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=70',
];

export interface HotelCardView {
  id: string;
  destinationId: number;
  name: string;
  address: string;
  area: string;
  rating: number;
  ratingLabel: string;
  price: number;
  rooms: number;
  amenities: string | null;
  image: string;
  status: string;
  statusValue: string;
}

export interface RoomInventoryView {
  id: string;
  type: string;
  price: number;
  available: number;
  total: number;
  pct: number;
}

export interface HotelBookingView {
  id: string;
  guest: string;
  room: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  total: number;
  status: string;
}

export interface HotelReviewView {
  id: string;
  name: string;
  rating: number;
  text: string;
  date: string;
  stars: number[];
}

export interface RatingRow {
  stars: number;
  pct: number;
}

export interface ReportStat {
  label: string;
  value: string;
}

export function numberValue(value: number | string | null | undefined): number {
  const parsed = typeof value === 'number' ? value : Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function isRoomAvailable(room: RoomResponse): boolean {
  const status = room.availabilityStatus.trim().toUpperCase();
  return status === 'AVAILABLE' || status === 'ACTIVE';
}

export function isActiveBooking(booking: HotelBookingResponse): boolean {
  return booking.bookingStatus.trim().toUpperCase() !== 'CANCELLED';
}

export function displayStatus(status: string): string {
  return status
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => `${part[0]?.toUpperCase() ?? ''}${part.slice(1)}`)
    .join(' ');
}

export function roomOccupancy(total: number, available: number): number {
  return total > 0 ? ((total - available) / total) * 100 : 0;
}

export function groupRooms(rooms: RoomResponse[]): RoomInventoryView[] {
  const groups = new Map<string, RoomResponse[]>();

  for (const room of rooms) {
    const key = `${room.hotelId}-${room.roomType}`;
    groups.set(key, [...(groups.get(key) ?? []), room]);
  }

  return Array.from(groups.values()).map((group) => {
    const first = group[0];
    const total = group.length;
    const available = group.filter(isRoomAvailable).length;
    const price = average(group.map((room) => numberValue(room.pricePerNight)));

    return {
      id: `${first.hotelId}-${first.roomType}`,
      type: first.roomType,
      price,
      available,
      total,
      pct: roomOccupancy(total, available),
    };
  });
}

export function mapHotelCards(hotels: HotelResponse[], rooms: RoomResponse[]): HotelCardView[] {
  return hotels.map((hotel, index) => {
    const rating = numberValue(hotel.rating);

    return {
      id: hotel.hotelId,
      destinationId: hotel.destinationId,
      name: hotel.hotelName,
      address: hotel.address,
      area: firstAddressPart(hotel.address) || `Destination ${hotel.destinationId}`,
      rating,
      ratingLabel: rating > 0 ? trimFixed(rating) : 'No reviews',
      price: numberValue(hotel.pricePerNight),
      rooms: rooms.filter((room) => room.hotelId === hotel.hotelId).length,
      amenities: hotel.amenities,
      image: HOTEL_IMAGES[index % HOTEL_IMAGES.length],
      status: displayStatus(hotel.status || 'Live'),
      statusValue: (hotel.status || 'ACTIVE').toUpperCase(),
    };
  });
}

export function filterHotelCards(hotels: HotelCardView[], query: string): HotelCardView[] {
  const term = normalize(query);
  if (!term) {
    return hotels;
  }

  return hotels.filter((hotel) =>
    [
      hotel.name,
      hotel.area,
      hotel.address,
      hotel.amenities ?? '',
      hotel.status,
      hotel.statusValue,
      `Destination ${hotel.destinationId}`,
      String(hotel.destinationId),
    ]
      .map(normalize)
      .some((value) => value.includes(term)),
  );
}

export function filterProviderOverview(overview: ProviderOverview, query: string): ProviderOverview {
  const term = normalize(query);
  if (!term) {
    return overview;
  }

  const matchedHotelIds = new Set(
    filterHotelCards(mapHotelCards(overview.hotels, overview.rooms), term).map((h) => h.id),
  );

  return {
    hotels: overview.hotels.filter((hotel) => matchedHotelIds.has(hotel.hotelId)),
    rooms: overview.rooms.filter((room) => matchedHotelIds.has(room.hotelId)),
    bookings: overview.bookings.filter((booking) => matchedHotelIds.has(booking.hotelId)),
    reviews: overview.reviews.filter((review) => matchedHotelIds.has(review.hotelId)),
  };
}

export function mapBookingRows(bookings: HotelBookingResponse[]): HotelBookingView[] {
  return [...bookings]
    .sort((a, b) => b.checkInDate.localeCompare(a.checkInDate))
    .map((booking) => ({
      id: booking.hotelBookingId,
      guest:
        booking.bookedByUserName ||
        (booking.bookedByUserId ? `Guest ${booking.bookedByUserId.slice(0, 8)}` : 'Guest'),
      room: booking.roomNumber ? `${booking.roomType} ${booking.roomNumber}` : booking.roomType,
      checkIn: formatShortDate(booking.checkInDate),
      checkOut: formatShortDate(booking.checkOutDate),
      guests: 1,
      total: numberValue(booking.totalAmount),
      status: displayStatus(booking.bookingStatus),
    }));
}

export function mapReviewCards(reviews: HotelReviewResponse[]): HotelReviewView[] {
  return reviews.map((review) => {
    const rating = Math.round(numberValue(review.rating));

    return {
      id: review.reviewId,
      name: review.userName,
      rating,
      text: review.comment || 'No written comment was provided.',
      date: review.createdAt ? formatShortDate(review.createdAt) : 'Recent review',
      stars: Array.from({ length: Math.max(0, Math.min(5, rating)) }, (_, index) => index),
    };
  });
}

export function providerSubtitle(hotels: HotelResponse[]): string {
  const first = hotels[0];
  return first ? `${first.hotelName} \u00b7 ${first.address}` : 'Live hotel performance and inventory.';
}

export function availableRoomCount(rooms: RoomResponse[]): number {
  return rooms.filter(isRoomAvailable).length;
}

export function bookingsToday(bookings: HotelBookingResponse[], referenceDate = new Date()): number {
  const today = toDateKey(referenceDate);
  return bookings.filter((booking) => booking.checkInDate === today && isActiveBooking(booking)).length;
}

export function monthlyRevenue(bookings: HotelBookingResponse[], referenceDate = new Date()): number {
  const year = referenceDate.getFullYear();
  const month = referenceDate.getMonth();

  return bookings
    .filter((booking) => {
      const date = parseLocalDate(booking.checkInDate);
      return isActiveBooking(booking) && date.getFullYear() === year && date.getMonth() === month;
    })
    .reduce((sum, booking) => sum + numberValue(booking.totalAmount), 0);
}

export function averageRating(overview: ProviderOverview): number {
  if (overview.reviews.length > 0) {
    return average(overview.reviews.map((review) => numberValue(review.rating)));
  }

  return average(overview.hotels.map((hotel) => numberValue(hotel.rating)));
}

export function buildRatingRows(reviews: HotelReviewResponse[]): RatingRow[] {
  const counts = new Map<number, number>();

  for (const review of reviews) {
    const stars = Math.max(1, Math.min(5, Math.round(numberValue(review.rating))));
    counts.set(stars, (counts.get(stars) ?? 0) + 1);
  }

  return [5, 4, 3, 2, 1].map((stars) => ({
    stars,
    pct: reviews.length > 0 ? Math.round(((counts.get(stars) ?? 0) / reviews.length) * 100) : 0,
  }));
}

export function buildReportStats(overview: ProviderOverview): ReportStat[] {
  const totalRooms = overview.rooms.length;
  const availableRooms = availableRoomCount(overview.rooms);
  const occupancy = totalRooms > 0 ? roomOccupancy(totalRooms, availableRooms) : 0;
  const revenue = monthlyRevenue(overview.bookings);
  const adr = averageDailyRate(overview);
  const rating = averageRating(overview);

  return [
    { label: 'Occupancy', value: `${occupancy.toFixed(0)}%` },
    { label: 'Revenue MTD', value: formatCompactCurrency(revenue) },
    { label: 'ADR', value: `${rupee()}${Math.round(adr).toLocaleString()}` },
    { label: 'Avg Rating', value: rating.toFixed(1) },
  ];
}

export function buildRevenueTrendData(bookings: HotelBookingResponse[], referenceDate = new Date()): number[] {
  const buckets = Array.from({ length: 11 }, (_, index) => {
    const month = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - 10 + index, 1);
    return { key: `${month.getFullYear()}-${month.getMonth()}`, total: 0 };
  });

  for (const booking of bookings.filter(isActiveBooking)) {
    const date = parseLocalDate(booking.checkInDate);
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    const bucket = buckets.find((item) => item.key === key);
    if (bucket) {
      bucket.total += numberValue(booking.totalAmount);
    }
  }

  return buckets.map((bucket) => Math.round(bucket.total / 1000));
}

export function formatCompactCurrency(amount: number): string {
  if (amount >= 100000) {
    return `${rupee()}${trimFixed(amount / 100000)}L`;
  }

  if (amount >= 1000) {
    return `${rupee()}${(amount / 1000).toFixed(0)}k`;
  }

  return `${rupee()}${Math.round(amount).toLocaleString()}`;
}

function average(values: number[]): number {
  return values.length > 0 ? values.reduce((sum, value) => sum + value, 0) / values.length : 0;
}

function averageDailyRate(overview: ProviderOverview): number {
  const activeBookings = overview.bookings.filter(isActiveBooking);
  const totalNights = activeBookings.reduce(
    (sum, booking) => sum + nightsBetween(booking.checkInDate, booking.checkOutDate),
    0,
  );

  if (totalNights > 0) {
    const revenue = activeBookings.reduce((sum, booking) => sum + numberValue(booking.totalAmount), 0);
    return revenue / totalNights;
  }

  return average(overview.rooms.map((room) => numberValue(room.pricePerNight)));
}

function nightsBetween(checkIn: string, checkOut: string): number {
  const msPerDay = 24 * 60 * 60 * 1000;
  return Math.max(1, Math.round((parseLocalDate(checkOut).getTime() - parseLocalDate(checkIn).getTime()) / msPerDay));
}

function firstAddressPart(address: string): string {
  return address.split(',')[0]?.trim() ?? '';
}

function normalize(value: string): string {
  return value.trim().toLowerCase();
}

function formatShortDate(value: string): string {
  const datePart = value.split('T', 1)[0];
  return parseLocalDate(datePart).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });
}

function parseLocalDate(value: string): Date {
  return new Date(`${value.split('T', 1)[0]}T00:00:00`);
}

function toDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function rupee(): string {
  return '\u20b9';
}

function trimFixed(value: number): string {
  return value.toFixed(1).replace(/\.0$/, '');
}

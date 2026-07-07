import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { HotelDashboard } from '@app/features/hotel/components/hotel-dashboard/hotel-dashboard';
import { HotelProperties } from '@app/features/hotel/components/hotel-properties/hotel-properties';
import { ManageRooms } from '@app/features/hotel/components/manage-rooms/manage-rooms';
import { HotelBookings } from '@app/features/hotel/components/hotel-bookings/hotel-bookings';
import { HotelReviews } from '@app/features/hotel/components/hotel-reviews/hotel-reviews';
import { HotelReports } from '@app/features/hotel/components/hotel-reports/hotel-reports';
import { HOTEL_ROUTES } from './hotel.routes';

describe('HOTEL_ROUTES', () => {
  it('wraps the hotel pages in the AppShell with the hotel role', async () => {
    expect(HOTEL_ROUTES).toHaveLength(1);
    const shellRoute = HOTEL_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('hotel');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all hotel paths as children', () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'properties',
      'rooms',
      'bookings',
      'reviews',
      'reports',
    ]);
  });

  it('lazily loads the real component for each child route', async () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    const expected = [
      HotelDashboard,
      HotelProperties,
      ManageRooms,
      HotelBookings,
      HotelReviews,
      HotelReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});

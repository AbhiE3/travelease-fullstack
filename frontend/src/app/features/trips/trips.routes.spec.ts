import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';
import { TripDetail } from '@app/features/trips/components/trip-detail/trip-detail';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';
import { TRIPS_ROUTES } from './trips.routes';

describe('TRIPS_ROUTES', () => {
  it('defines the trips list, new-trip, and trip-detail paths', () => {
    expect(TRIPS_ROUTES.map((r) => r.path)).toEqual(['', 'new', ':tripId']);
  });

  it('lazily loads TripList for the index route', async () => {
    const loaded = await TRIPS_ROUTES[0].loadComponent!();
    expect(loaded).toBe(TripList);
  });

  it('lazily loads NewTrip for the new route', async () => {
    const loaded = await TRIPS_ROUTES[1].loadComponent!();
    expect(loaded).toBe(NewTrip);
  });

  it('lazily loads TripDetail for the trip-detail route', async () => {
    const loaded = await TRIPS_ROUTES[2].loadComponent!();
    expect(loaded).toBe(TripDetail);
  });
});

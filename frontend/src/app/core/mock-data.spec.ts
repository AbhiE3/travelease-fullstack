import {
  trips,
  members,
  buses,
  hotels,
  expenses,
  itinerary,
  alerts,
  notifications,
  invitations,
  activities,
  routeAnalytics,
  funnelStages,
  dropReasons,
  hotelPartners,
  transportPartners,
  activityPartners,
  pendingApprovals,
  hotelBookings,
  rooms,
  vehicles,
  partnerRoutes,
  providerActivities,
} from '@app/core/mock-data';

describe('mock-data', () => {
  it('exports the expected trips', () => {
    expect(trips).toHaveLength(3);
    expect(trips[0].id).toBe('goa-2026');
    expect(trips[0].status).toBe('upcoming');
  });

  it('exports every mock collection as a non-empty array', () => {
    const collections = [
      members,
      buses,
      hotels,
      expenses,
      itinerary,
      alerts,
      notifications,
      invitations,
      activities,
      routeAnalytics,
      funnelStages,
      dropReasons,
      hotelPartners,
      transportPartners,
      activityPartners,
      pendingApprovals,
      hotelBookings,
      rooms,
      vehicles,
      partnerRoutes,
      providerActivities,
    ];

    for (const collection of collections) {
      expect(Array.isArray(collection)).toBe(true);
      expect(collection.length).toBeGreaterThan(0);
    }
  });
});

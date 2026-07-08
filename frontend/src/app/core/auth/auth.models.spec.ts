import { BACKEND_ROLE_MAP, ROLE_HOME } from '@app/core/auth/auth.models';

describe('BACKEND_ROLE_MAP', () => {
  it('maps exactly the 5 known backend roles to the 5 frontend roles', () => {
    expect(BACKEND_ROLE_MAP).toEqual({
      ROLE_ADMIN: 'admin',
      ROLE_TRAVELER: 'traveler',
      ROLE_PROVIDER: 'transport',
      ROLE_HOTEL_PROVIDER: 'hotel',
      ROLE_ACTIVITY_PROVIDER: 'activity',
    });
  });
});

describe('ROLE_HOME', () => {
  it('maps every frontend role to its dashboard home route', () => {
    expect(ROLE_HOME).toEqual({
      traveler: '/dashboard',
      admin: '/admin',
      hotel: '/hotel',
      transport: '/transport',
      activity: '/activity',
    });
  });
});

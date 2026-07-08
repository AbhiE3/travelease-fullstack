export type Role = 'traveler' | 'admin' | 'hotel' | 'transport' | 'activity';

export const BACKEND_ROLE_MAP: Record<string, Role> = {
  ROLE_ADMIN: 'admin',
  ROLE_TRAVELER: 'traveler',
  ROLE_PROVIDER: 'transport',
  ROLE_HOTEL_PROVIDER: 'hotel',
  ROLE_ACTIVITY_PROVIDER: 'activity',
};

export const ROLE_HOME: Record<Role, string> = {
  traveler: '/dashboard',
  admin: '/admin',
  hotel: '/hotel',
  transport: '/transport',
  activity: '/activity',
};

export interface StoredUser {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export type BackendRole = 'ROLE_ADMIN' | 'ROLE_PROVIDER' | 'ROLE_TRAVELER';

export interface ApiError {
  code: string;
  message: string;
  details?: unknown[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: ApiError;
  timestamp: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: BackendRole;
  providerId: number | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  user: AuthUser;
}

export interface RefreshTokenResponse {
  accessToken: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string | null;
  user: AuthUser;
}

const ROLE_HOME: Record<BackendRole, string> = {
  ROLE_ADMIN: '/admin',
  ROLE_PROVIDER: '/hotel',
  ROLE_TRAVELER: '/dashboard',
};

const ROLE_URL_PREFIXES: Record<BackendRole, readonly string[]> = {
  ROLE_ADMIN: ['/admin'],
  ROLE_PROVIDER: ['/hotel', '/transport', '/activity'],
  ROLE_TRAVELER: ['/dashboard', '/trips', '/expenses', '/profile', '/notifications', '/invitations'],
};

export function homeRouteForRole(role: BackendRole): string {
  return ROLE_HOME[role];
}

export function homeRouteForUser(user: AuthUser | null | undefined): string {
  return user ? homeRouteForRole(user.role) : '/login';
}

export function isUrlAllowedForRole(role: BackendRole, url: string | null | undefined): boolean {
  if (!url || !url.startsWith('/') || url.startsWith('//')) {
    return false;
  }

  const path = url.split(/[?#]/, 1)[0];
  return ROLE_URL_PREFIXES[role].some((prefix) => path === prefix || path.startsWith(`${prefix}/`));
}

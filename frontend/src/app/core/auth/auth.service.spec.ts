import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { AuthService } from '@app/core/auth/auth.service';

describe('AuthService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  async function setup(platform: 'browser' | 'server' = 'browser') {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PLATFORM_ID, useValue: platform },
      ],
    }).compileComponents();
    return {
      service: TestBed.inject(AuthService),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('starts unauthenticated with no stored session', async () => {
    const { service } = await setup();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.role()).toBeNull();
  });

  it('logs in, maps the backend role, and persists the session', async () => {
    const { service, httpMock } = await setup();

    const loginPromise = service.login('admin@travelease.test', 'password123');

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@travelease.test', password: 'password123' });
    req.flush({
      success: true,
      data: {
        accessToken: 'jwt-token',
        user: {
          id: 'u1',
          name: 'Admin User',
          email: 'admin@travelease.test',
          phone: '9000000000',
          role: 'ROLE_ADMIN',
          providerId: null,
        },
      },
      message: 'Login successful',
      error: null,
    });

    const user = await loginPromise;
    expect(user).toEqual({ id: 'u1', name: 'Admin User', email: 'admin@travelease.test', role: 'admin' });
    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('admin');
    expect(localStorage.getItem('te_access_token')).toBe('jwt-token');
    expect(JSON.parse(localStorage.getItem('te_user')!)).toEqual(user);
  });

  it('rejects and does not persist on a failed login', async () => {
    const { service, httpMock } = await setup();

    const loginPromise = service.login('admin@travelease.test', 'wrongpassword');
    loginPromise.catch(() => {});
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    req.flush(
      {
        success: false,
        data: null,
        message: null,
        error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' },
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    await expect(loginPromise).rejects.toBeTruthy();
    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('te_access_token')).toBeNull();
  });

  it('logout clears the session', async () => {
    const { service, httpMock } = await setup();
    const loginPromise = service.login('admin@travelease.test', 'password123');
    httpMock.expectOne('http://localhost:8080/api/auth/login').flush({
      success: true,
      data: {
        accessToken: 'jwt-token',
        user: { id: 'u1', name: 'Admin', email: 'admin@travelease.test', phone: '900', role: 'ROLE_ADMIN', providerId: null },
      },
      message: 'ok',
      error: null,
    });
    await loginPromise;

    service.logout();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.role()).toBeNull();
    expect(localStorage.getItem('te_access_token')).toBeNull();
    expect(localStorage.getItem('te_user')).toBeNull();
  });

  it('restores a persisted session from localStorage on construction', async () => {
    localStorage.setItem('te_access_token', 'jwt-token');
    localStorage.setItem(
      'te_user',
      JSON.stringify({ id: 'u1', name: 'Admin', email: 'admin@travelease.test', role: 'admin' }),
    );

    const { service } = await setup();
    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('admin');
  });

  it('does not read or write localStorage when run on the server platform', async () => {
    localStorage.setItem('te_access_token', 'jwt-token');
    const { service } = await setup('server');
    expect(service.isAuthenticated()).toBe(false);
  });
});

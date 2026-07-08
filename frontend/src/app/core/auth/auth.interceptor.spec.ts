import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '@app/core/auth/auth.service';
import { authInterceptor } from '@app/core/auth/auth.interceptor';

describe('authInterceptor', () => {
  async function setup(token: string | null) {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => token } },
      ],
    }).compileComponents();
    return {
      http: TestBed.inject(HttpClient),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('attaches the Authorization header when a token is present', async () => {
    const { http, httpMock } = await setup('jwt-token');
    http.get('/api/whatever').subscribe();
    const req = httpMock.expectOne('/api/whatever');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('omits the Authorization header when no token is present', async () => {
    const { http, httpMock } = await setup(null);
    http.get('/api/whatever').subscribe();
    const req = httpMock.expectOne('/api/whatever');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});

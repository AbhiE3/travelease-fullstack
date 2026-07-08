import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
import { BACKEND_ROLE_MAP, StoredUser } from '@app/core/auth/auth.models';

const TOKEN_KEY = 'te_access_token';
const USER_KEY = 'te_user';

interface LoginResponseDto {
  accessToken: string;
  user: {
    id: string;
    name: string;
    email: string;
    phone: string;
    role: string;
    providerId: number | null;
  };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly tokenSignal = signal<string | null>(this.readToken());
  private readonly userSignal = signal<StoredUser | null>(this.readUser());

  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly currentUser = computed(() => this.userSignal());
  readonly role = computed(() => this.userSignal()?.role ?? null);

  async login(email: string, password: string): Promise<StoredUser> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<LoginResponseDto>>(`${API_BASE_URL}/api/auth/login`, {
        email,
        password,
      }),
    );

    const role = BACKEND_ROLE_MAP[response.data.user.role];
    const user: StoredUser = {
      id: response.data.user.id,
      name: response.data.user.name,
      email: response.data.user.email,
      role,
    };

    this.persist(response.data.accessToken, user);
    return user;
  }

  logout(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    if (this.isBrowser) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  private persist(token: string, user: StoredUser): void {
    this.tokenSignal.set(token);
    this.userSignal.set(user);
    if (this.isBrowser) {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    }
  }

  private readToken(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(TOKEN_KEY);
  }

  private readUser(): StoredUser | null {
    if (!this.isBrowser) {
      return null;
    }
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  }
}

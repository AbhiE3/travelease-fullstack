import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';

interface RoleCredential {
  username: string;
  password: string;
  route: string;
}

const ROLE_CREDENTIALS: RoleCredential[] = [
  { username: 'user', password: 'user123', route: '/dashboard' },
  { username: 'admin', password: 'admin123', route: '/admin' },
  { username: 'hotel', password: 'hotel123', route: '/hotel' },
  { username: 'bus', password: 'bus123', route: '/transport' },
  { username: 'activity', password: 'activity123', route: '/activity' },
];

export function matchRole(username: string, password: string): string | null {
  const match = ROLE_CREDENTIALS.find(
    (c) => c.username === username && c.password === password,
  );
  return match ? match.route : null;
}

@Component({
  selector: 'app-login',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './login.html',
})
export class Login {
  private readonly router = inject(Router);

  protected readonly error = signal<string | null>(null);

  protected onSubmit(event: Event, username: string, password: string): void {
    event.preventDefault();
    const route = matchRole(username, password);
    if (route) {
      this.error.set(null);
      this.router.navigate([route]);
    } else {
      this.error.set('Invalid username or password');
    }
  }
}

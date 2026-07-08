import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { Role, ROLE_HOME } from '@app/core/auth/auth.models';
import { AuthService } from '@app/core/auth/auth.service';

interface NavItem {
  to: string;
  label: string;
  icon: string;
}

const NAV_MAP: Record<Role, NavItem[]> = {
  traveler: [
    { to: '/dashboard', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/trips', label: 'My Trips', icon: 'lucidePlane' },
    { to: '/invitations', label: 'Invitations', icon: 'lucideMail' },
    { to: '/expenses', label: 'Expenses', icon: 'lucideWallet' },
    { to: '/notifications', label: 'Notifications', icon: 'lucideBell' },
    { to: '/profile', label: 'Profile', icon: 'lucideUser' },
  ],
  admin: [
    { to: '/admin', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/admin/route-analytics', label: 'Route Analytics', icon: 'lucideRoute' },
    { to: '/admin/partners', label: 'Partner Analytics', icon: 'lucideTrendingUp' },
    { to: '/admin/funnel', label: 'Booking Funnel', icon: 'lucideBarChart3' },
    { to: '/admin/approvals', label: 'Partner Approvals', icon: 'lucideUserCheck' },
    { to: '/admin/users', label: 'Users', icon: 'lucideUsers' },
    { to: '/admin/trips', label: 'Trips', icon: 'lucidePlane' },
    { to: '/admin/buses', label: 'Bus Management', icon: 'lucideBus' },
    { to: '/admin/hotels', label: 'Hotel Management', icon: 'lucideHotel' },
    { to: '/admin/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
  hotel: [
    { to: '/hotel', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/hotel/properties', label: 'Hotels', icon: 'lucideHotel' },
    { to: '/hotel/rooms', label: 'Rooms', icon: 'lucideDoorOpen' },
    { to: '/hotel/bookings', label: 'Bookings', icon: 'lucideCalendarDays' },
    { to: '/hotel/reviews', label: 'Reviews', icon: 'lucideStar' },
    { to: '/hotel/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
  transport: [
    { to: '/transport', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/transport/vehicles', label: 'Vehicles', icon: 'lucideBus' },
    { to: '/transport/routes', label: 'Routes', icon: 'lucideRoute' },
    { to: '/transport/bookings', label: 'Bookings', icon: 'lucideCalendarDays' },
    { to: '/transport/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
  activity: [
    { to: '/activity', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/activity/activities', label: 'Activities', icon: 'lucideActivity' },
    { to: '/activity/bookings', label: 'Bookings', icon: 'lucideCalendarDays' },
    { to: '/activity/capacity', label: 'Capacity', icon: 'lucideUsers' },
    { to: '/activity/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
};

const ROLE_LABEL: Record<Role, string> = {
  traveler: 'Traveler',
  admin: 'Admin',
  hotel: 'Hotel Partner',
  transport: 'Transport Partner',
  activity: 'Activity Provider',
};

@Component({
  selector: 'app-shell',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    NgIcon,
    HlmButtonImports,
    HlmInputImports,
    HlmAvatarImports,
  ],
  templateUrl: './app-shell.html',
})
export class AppShell {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly role = toSignal(
    this.route.data.pipe(map((data) => (data['role'] as Role | undefined) ?? 'traveler')),
    { initialValue: 'traveler' as Role },
  );

  protected readonly nav = computed(() => NAV_MAP[this.role()]);
  protected readonly roleLabel = computed(() => ROLE_LABEL[this.role()]);
  protected readonly home = computed(() => ROLE_HOME[this.role()]);

  protected signOut(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

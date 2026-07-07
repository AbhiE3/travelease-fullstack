import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideSearch } from '@ng-icons/lucide';
import { members } from '@app/core/mock-data';
import { AdminUsers } from '@app/features/admin/components/admin-users/admin-users';

describe('AdminUsers', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsers],
      providers: [provideIcons({ lucideSearch })],
    }).compileComponents();
  });

  it('renders exactly members.length * 2 rows with unique ids', () => {
    const fixture = TestBed.createComponent(AdminUsers);
    const rows = fixture.componentInstance.rows;
    expect(rows).toHaveLength(members.length * 2);
    const uniqueIds = new Set(rows.map((r) => r.id));
    expect(uniqueIds.size).toBe(rows.length);
  });

  it('renders each member name, email, and role (twice each)', () => {
    const fixture = TestBed.createComponent(AdminUsers);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const m of members) {
      const nameOccurrences = text.split(m.name).length - 1;
      expect(nameOccurrences).toBe(2);
      expect(text).toContain(m.email);
      expect(text).toContain(m.role);
    }
  });

  it('gives Accepted and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(AdminUsers);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const acceptedBadge = badges.find((b) => b.textContent === 'Accepted')!;
    const pendingBadge = badges.find((b) => b.textContent === 'Pending')!;
    expect(acceptedBadge.className).toContain('text-success');
    expect(pendingBadge.className).toContain('border-warning/20');
  });
});

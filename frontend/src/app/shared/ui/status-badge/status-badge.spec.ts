import { TestBed } from '@angular/core/testing';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

function render(status: string) {
  const fixture = TestBed.createComponent(StatusBadge);
  fixture.componentRef.setInput('status', status);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('StatusBadge', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StatusBadge] }).compileComponents();
  });

  it('applies the success color classes for Accepted', () => {
    const el = render('Accepted');
    expect(el.querySelector('span')?.className).toContain('text-success');
  });

  it('applies the warning color classes for Pending', () => {
    const el = render('Pending');
    expect(el.querySelector('span')?.className).toContain('border-warning/20');
  });

  it('applies the destructive color classes for Rejected', () => {
    const el = render('Rejected');
    expect(el.querySelector('span')?.className).toContain('text-destructive');
  });

  it('applies the primary color classes for upcoming', () => {
    const el = render('upcoming');
    expect(el.querySelector('span')?.className).toContain('text-primary');
  });

  it('applies the success/10 color classes for Confirmed, distinct from Accepted', () => {
    const el = render('Confirmed');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('bg-success/10');
    expect(className).toContain('text-success');
  });

  it('applies the success/10 color classes for Active', () => {
    const el = render('Active');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('bg-success/10');
    expect(className).toContain('text-success');
  });

  it('applies the warning color classes for Maintenance', () => {
    const el = render('Maintenance');
    expect(el.querySelector('span')?.className).toContain('border-warning/20');
  });

  it('falls back to no extra color classes for an unmatched status', () => {
    const el = render('SomeUnknownStatus');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('capitalize');
    expect(className).not.toContain('text-success');
    expect(className).not.toContain('text-destructive');
  });

  it('renders the status text', () => {
    const el = render('Accepted');
    expect(el.textContent?.trim()).toBe('Accepted');
  });
});

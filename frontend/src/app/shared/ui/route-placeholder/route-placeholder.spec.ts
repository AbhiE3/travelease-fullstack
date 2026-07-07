import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';

describe('RoutePlaceholder', () => {
  it('renders the title from route data', async () => {
    await TestBed.configureTestingModule({
      imports: [RoutePlaceholder],
      providers: [{ provide: ActivatedRoute, useValue: { data: of({ title: 'Dashboard' }) } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(RoutePlaceholder);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toBe('Dashboard');
  });

  it('falls back to "Untitled" when route data has no title', async () => {
    await TestBed.configureTestingModule({
      imports: [RoutePlaceholder],
      providers: [{ provide: ActivatedRoute, useValue: { data: of({}) } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(RoutePlaceholder);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toBe('Untitled');
  });
});

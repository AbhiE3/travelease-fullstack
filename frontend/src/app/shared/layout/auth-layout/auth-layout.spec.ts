import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthLayout } from '@app/shared/layout/auth-layout/auth-layout';

describe('AuthLayout', () => {
  it('creates and renders a router outlet for the auth pages', async () => {
    await TestBed.configureTestingModule({
      imports: [AuthLayout],
      providers: [provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(AuthLayout);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});

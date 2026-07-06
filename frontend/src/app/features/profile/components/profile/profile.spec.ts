import { TestBed } from '@angular/core/testing';
import { Profile } from '@app/features/profile/components/profile/profile';

describe('Profile', () => {
  it('renders the hardcoded name, email, and all 4 input defaults', async () => {
    await TestBed.configureTestingModule({ imports: [Profile] }).compileComponents();
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';

    expect(text).toContain('Sarathy R');
    expect(text).toContain('sarathy@example.com');

    const inputValues = Array.from(el.querySelectorAll('input')).map(
      (i) => (i as HTMLInputElement).value,
    );
    expect(inputValues).toEqual(['Sarathy R', 'sarathy@example.com', '+91 9876543210', 'Bengaluru']);
  });
});

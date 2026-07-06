import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin } from '@ng-icons/lucide';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';

describe('DestinationPill', () => {
  it('renders the from and to text with an arrow between them', async () => {
    await TestBed.configureTestingModule({
      imports: [DestinationPill],
      providers: [provideIcons({ lucideMapPin })],
    }).compileComponents();

    const fixture = TestBed.createComponent(DestinationPill);
    fixture.componentRef.setInput('from', 'Bengaluru');
    fixture.componentRef.setInput('to', 'Goa');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Bengaluru');
    expect(text).toContain('Goa');
    expect(text).toContain('→');
  });
});

import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideAward, lucideStar, lucideTrendingDown } from '@ng-icons/lucide';
import {
  PartnerRankingTable,
  cancellationClass,
  partnerBadgeStatus,
  type Partner,
} from '@app/features/admin/components/admin-partners/partner-ranking-table/partner-ranking-table';

const SAMPLE_DATA: Partner[] = [
  {
    id: 'p1',
    name: 'Alpha Hotel',
    city: 'Goa',
    bookings: 100,
    cancellation: 3,
    rating: 4.5,
    revenue: 500000,
    status: 'Active',
  },
  {
    id: 'p2',
    name: 'Beta Hotel',
    city: 'Manali',
    bookings: 50,
    cancellation: 12,
    rating: 3.9,
    revenue: 200000,
    status: 'Review',
  },
  {
    id: 'p3',
    name: 'Gamma Hotel',
    city: 'Goa',
    bookings: 80,
    cancellation: 5,
    rating: 4.2,
    revenue: 800000,
    status: 'Active',
  },
];

describe('cancellationClass', () => {
  it('gives >7% a destructive tone and <=7% a success tone', () => {
    expect(cancellationClass(8)).toContain('text-destructive');
    expect(cancellationClass(7)).toContain('text-success');
  });
});

describe('partnerBadgeStatus', () => {
  it('maps Active to Accepted and anything else to Pending', () => {
    expect(partnerBadgeStatus('Active')).toBe('Accepted');
    expect(partnerBadgeStatus('Review')).toBe('Pending');
  });
});

describe('PartnerRankingTable', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PartnerRankingTable],
      providers: [provideIcons({ lucideAward, lucideStar, lucideTrendingDown })],
    }).compileComponents();
  });

  it('shows the highest-revenue entry as Top and the highest-cancellation entry as Needs Attention', () => {
    const fixture = TestBed.createComponent(PartnerRankingTable);
    fixture.componentRef.setInput('data', SAMPLE_DATA);
    fixture.componentRef.setInput('label', 'Hotel');
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Top Hotel');
    expect(text).toContain('Gamma Hotel');
    expect(text).toContain('Needs Attention');
    expect(text).toContain('Beta Hotel');
  });

  it('renders every partner ranked by revenue descending', () => {
    const fixture = TestBed.createComponent(PartnerRankingTable);
    fixture.componentRef.setInput('data', SAMPLE_DATA);
    fixture.componentRef.setInput('label', 'Hotel');
    fixture.detectChanges();
    expect(fixture.componentInstance.sorted().map((p) => p.name)).toEqual([
      'Gamma Hotel',
      'Alpha Hotel',
      'Beta Hotel',
    ]);
  });
});

import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import {
  ManageRooms,
  roomOccupancy,
} from '@app/features/hotel/components/manage-rooms/manage-rooms';
import { HotelProviderService } from '@app/features/hotel/services/hotel-provider.service';
import { groupRooms } from '@app/features/hotel/services/hotel-provider-view-models';
import {
  TEST_PROVIDER_OVERVIEW,
  createHotelProviderStub,
} from '@app/features/hotel/testing/hotel-provider-test-data';

describe('roomOccupancy', () => {
  it('returns occupied room percentage safely', () => {
    expect(roomOccupancy(12, 4)).toBeCloseTo(((12 - 4) / 12) * 100);
    expect(roomOccupancy(0, 0)).toBe(0);
  });
});

describe('ManageRooms', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageRooms],
      providers: [
        provideIcons({ lucidePlus }),
        { provide: HotelProviderService, useValue: createHotelProviderStub() },
      ],
    }).compileComponents();
  });

  it('renders every provider room type and price', () => {
    const fixture = TestBed.createComponent(ManageRooms);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of groupRooms(TEST_PROVIDER_OVERVIEW.rooms)) {
      expect(text).toContain(r.type);
      expect(text).toContain(r.price.toLocaleString());
    }
  });

  it('computes the correct occupancy percentage per grouped room type', () => {
    const fixture = TestBed.createComponent(ManageRooms);
    const rows = fixture.componentInstance.rows;
    for (const r of groupRooms(TEST_PROVIDER_OVERVIEW.rooms)) {
      const row = rows.find((x) => x.id === r.id)!;
      expect(row.pct).toBeCloseTo(r.pct);
    }
  });
});

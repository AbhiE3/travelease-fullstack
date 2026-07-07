import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { rooms } from '@app/core/mock-data';
import {
  ManageRooms,
  roomOccupancy,
} from '@app/features/hotel/components/manage-rooms/manage-rooms';

describe('roomOccupancy', () => {
  it('matches the formula from the React source', () => {
    expect(roomOccupancy(12, 4)).toBeCloseTo(((12 - 4) / 12) * 100);
    expect(roomOccupancy(6, 1)).toBeCloseTo(((6 - 1) / 6) * 100);
  });
});

describe('ManageRooms', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageRooms],
      providers: [provideIcons({ lucidePlus })],
    }).compileComponents();
  });

  it('renders every room type, and price', () => {
    const fixture = TestBed.createComponent(ManageRooms);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of rooms) {
      expect(text).toContain(r.type);
      expect(text).toContain(r.price.toLocaleString());
    }
  });

  it('computes the correct occupancy percentage per room', () => {
    const fixture = TestBed.createComponent(ManageRooms);
    const rows = fixture.componentInstance.rows;
    for (const r of rooms) {
      const row = rows.find((x) => x.id === r.id)!;
      expect(row.occ).toBeCloseTo(((r.total - r.available) / r.total) * 100);
    }
  });
});

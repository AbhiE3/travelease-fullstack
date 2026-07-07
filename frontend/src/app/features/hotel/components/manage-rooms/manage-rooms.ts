import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { rooms } from '@app/core/mock-data';

interface RoomRow {
  id: string;
  type: string;
  total: number;
  available: number;
  price: number;
  occ: number;
}

export function roomOccupancy(total: number, available: number): number {
  return ((total - available) / total) * 100;
}

@Component({
  selector: 'app-manage-rooms',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './manage-rooms.html',
})
export class ManageRooms {
  public readonly rows: RoomRow[] = rooms.map((r) => ({
    id: r.id,
    type: r.type,
    total: r.total,
    available: r.available,
    price: r.price,
    occ: roomOccupancy(r.total, r.available),
  }));
}

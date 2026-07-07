import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideStar } from '@ng-icons/lucide';
import { activities } from '@app/core/mock-data';
import { ManageActivities } from '@app/features/activity/components/manage-activities/manage-activities';

describe('ManageActivities', () => {
  it('renders every activity name and price', async () => {
    await TestBed.configureTestingModule({
      imports: [ManageActivities],
      providers: [provideIcons({ lucidePlus, lucideStar })],
    }).compileComponents();

    const fixture = TestBed.createComponent(ManageActivities);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of activities) {
      expect(text).toContain(a.name);
      expect(text).toContain(a.price.toLocaleString());
    }
  });
});

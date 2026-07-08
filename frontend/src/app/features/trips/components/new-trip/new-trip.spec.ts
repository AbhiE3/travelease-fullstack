import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';
import { TripsService } from '@app/features/trips/services/trips.service';
import { CreateTripPayload, Trip } from '@app/features/trips/services/trip.models';

const CREATED_TRIP: Trip = {
  tripId: 'bbbbbbbb-0000-0000-0000-000000000002',
  tripName: 'Goa Beach Escape',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Bengaluru',
  destinationId: 3,
  budgetAmount: 18000,
  categoryId: 4,
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  status: 'PLANNING',
  viewerRole: 'ORGANIZER',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
};

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [NewTrip],
    providers: [
      provideRouter([]),
      provideIcons({ lucideArrowLeft }),
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(NewTrip);
  const router = TestBed.inject(Router);
  const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  fixture.detectChanges();
  return { fixture, navigateSpy };
}

function fillAndSubmit(el: HTMLElement) {
  (el.querySelector('#name') as HTMLInputElement).value = 'Goa Beach Escape';
  (el.querySelector('#budget') as HTMLInputElement).value = '18000';
  (el.querySelector('#source') as HTMLInputElement).value = 'Bengaluru';
  (el.querySelector('#destination') as HTMLInputElement).value = '3';
  (el.querySelector('#start-date') as HTMLInputElement).value = '2026-08-01';
  (el.querySelector('#end-date') as HTMLInputElement).value = '2026-08-05';
  const form = el.querySelector('form')!;
  form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
}

describe('NewTrip', () => {
  it('submits the form with the default trip type and navigates to the created trip', async () => {
    const createTrip = vi.fn().mockReturnValue(of(CREATED_TRIP));
    const { fixture, navigateSpy } = await setup({ createTrip });
    const el = fixture.nativeElement as HTMLElement;

    fillAndSubmit(el);
    await fixture.whenStable();

    const expectedPayload: CreateTripPayload = {
      tripName: 'Goa Beach Escape',
      sourceLocation: 'Bengaluru',
      destinationId: 3,
      budgetAmount: 18000,
      categoryId: 4,
      startDate: '2026-08-01',
      endDate: '2026-08-05',
    };
    expect(createTrip).toHaveBeenCalledWith(expectedPayload);
    expect(navigateSpy).toHaveBeenCalledWith(['/trips', CREATED_TRIP.tripId]);
  });

  it('shows validation error details and does not navigate on failure', async () => {
    const httpError = new HttpErrorResponse({
      status: 400,
      error: {
        success: false,
        data: null,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Validation failed',
          details: ['budgetAmount: must be greater than or equal to 0.01'],
        },
      },
    });
    const createTrip = vi.fn().mockReturnValue(throwError(() => httpError));
    const { fixture, navigateSpy } = await setup({ createTrip });
    const el = fixture.nativeElement as HTMLElement;

    fillAndSubmit(el);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Validation failed');
    expect(el.textContent).toContain('budgetAmount: must be greater than or equal to 0.01');
  });
});

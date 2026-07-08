import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DestinationsService } from '@app/core/destinations/destinations.service';
import { Destination } from '@app/core/destinations/destination.models';

const SAMPLE_DESTINATIONS: Destination[] = [
  {
    destinationId: 1,
    destinationName: 'Mumbai',
    state: 'Maharashtra',
    country: 'India',
    description: 'Financial capital of India',
  },
  {
    destinationId: 2,
    destinationName: 'Goa',
    state: 'Goa',
    country: 'India',
    description: 'Beach paradise',
  },
];

describe('DestinationsService', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    return {
      service: TestBed.inject(DestinationsService),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('fetches and unwraps the destination list', async () => {
    const { service, httpMock } = await setup();

    let result: Destination[] | undefined;
    service.listDestinations().subscribe((destinations) => (result = destinations));

    const req = httpMock.expectOne('http://localhost:8080/api/destinations');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: SAMPLE_DESTINATIONS, message: 'ok', error: null });

    expect(result).toEqual(SAMPLE_DESTINATIONS);
  });

  it('propagates an HTTP error', async () => {
    const { service, httpMock } = await setup();

    let errored = false;
    service.listDestinations().subscribe({ error: () => (errored = true) });

    const req = httpMock.expectOne('http://localhost:8080/api/destinations');
    req.flush(
      { success: false, data: null, message: null, error: { code: 'SERVER_ERROR', message: 'boom' } },
      { status: 500, statusText: 'Server Error' },
    );

    expect(errored).toBe(true);
  });
});

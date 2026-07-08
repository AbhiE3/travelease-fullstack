# Destinations Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a read-only destinations catalog (backend entity + seed data + `GET` endpoints) and wire it into the frontend's two existing gaps: `new-trip.html`'s raw destination-ID number input becomes a real dropdown, and `trip-list.html`'s `Destination #<id>` placeholder becomes a real name.

**Architecture:** Backend: a standalone `Destination` entity (Integer PK, matching the type of every existing `destinationId` column already in the app) in the `admin/` package, seeded via `DemoDataInitializer` with IDs aligned to existing hotel/activity seed data (1=Mumbai, 2=Goa). Frontend: a new `core/destinations/` module (reference data, not trip-specific) consumed by both `new-trip.ts` (dropdown) and `trip-list.ts` (name lookup).

**Tech Stack:** Java 17, Spring Boot 3.5.16, Spring Data JPA, JUnit 5 + Mockito + AssertJ (backend); Angular 21.2, Vitest, RxJS (frontend).

## Global Constraints

- Backend: follow `docs/coding_guidelines.md` package layout (`admin/controller`, `admin/dto`, `admin/entity`, `admin/repository`, `admin/service`). `Destination` does **not** extend `BaseEntity` — its PK is `Integer destinationId` (`@GeneratedValue(strategy = GenerationType.IDENTITY)`), matching every existing `destinationId: Integer` column already in `Trip`/`Activity`, not `BaseEntity`'s `Long`.
- **No `@WebMvcTest` precedent exists anywhere in this codebase** (confirmed by search) — the one existing controller-level test, `AuthFlowIntegrationTest`, is a full `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` integration test that exercises real JWT auth end-to-end. Task 3 below mirrors that exact pattern instead of introducing an unproven `@WebMvcTest` + security-mock setup with no local precedent to copy.
- No `@PreAuthorize` needed on the new endpoints — both are open to any authenticated user (`SecurityConfig`'s default `anyRequest().authenticated()` catch-all already covers `/api/destinations/**`; no `SecurityConfig` changes needed).
- Scope is **read-only**: `GET /api/destinations`, `GET /api/destinations/{destinationId}`. No create/update/delete endpoints or DTOs.
- Backend test command: `./mvnw test` (single class: `./mvnw test -Dtest=ClassName`). Run from `backend/`.
- Frontend test command: `npx ng test --include='<path>' --watch=false` (single file), `npx ng test --watch=false` (full suite). Run from `frontend/`.
- Frontend build command: `npx ng build`. Run from `frontend/`.
- **Do not run `git commit`.** Leave all changes in the working tree for review. No task below has a commit step.
- `hlm-select` (used for the new destination dropdown) is CDK-Overlay-based, same as the dialog encountered in the invitations work — no existing spec in this codebase drives a real select-open-click interaction. Task 5's test verifies submission using the auto-selected first destination (the same approach already used for the existing Trip Type select, which was never click-tested either), not a simulated dropdown interaction.

---

### Task 1: `Destination` entity, repository, service (backend)

**Files:**
- Create: `backend/src/main/java/com/travelease/backend/admin/entity/Destination.java`
- Create: `backend/src/main/java/com/travelease/backend/admin/dto/DestinationResponse.java`
- Create: `backend/src/main/java/com/travelease/backend/admin/repository/DestinationRepository.java`
- Create: `backend/src/main/java/com/travelease/backend/admin/service/DestinationService.java`
- Create: `backend/src/main/java/com/travelease/backend/admin/service/DestinationServiceImpl.java`
- Create: `backend/src/test/java/com/travelease/backend/admin/service/DestinationServiceImplTest.java`

**Interfaces:**
- Produces: `Destination` entity, `DestinationResponse(destinationId, destinationName, state, country, description)`, `DestinationRepository extends JpaRepository<Destination, Integer>`, `DestinationService.getAllDestinations(): List<DestinationResponse>` / `.getDestinationById(Integer): DestinationResponse` (throws `ResourceNotFoundException` if missing). Task 3's controller depends on `DestinationService`'s exact method names.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/travelease/backend/admin/service/DestinationServiceImplTest.java`:

```java
package com.travelease.backend.admin.service;

import com.travelease.backend.admin.dto.DestinationResponse;
import com.travelease.backend.admin.entity.Destination;
import com.travelease.backend.admin.repository.DestinationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceImplTest {

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private DestinationServiceImpl destinationService;

    private Destination sampleDestination() {
        Destination destination = new Destination();
        destination.setDestinationId(1);
        destination.setDestinationName("Mumbai");
        destination.setState("Maharashtra");
        destination.setCountry("India");
        destination.setDescription("Financial capital of India");
        return destination;
    }

    @Test
    void getAllDestinationsMapsEveryRow() {
        when(destinationRepository.findAll()).thenReturn(List.of(sampleDestination()));

        List<DestinationResponse> result = destinationService.getAllDestinations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).destinationId()).isEqualTo(1);
        assertThat(result.get(0).destinationName()).isEqualTo("Mumbai");
        assertThat(result.get(0).state()).isEqualTo("Maharashtra");
    }

    @Test
    void getDestinationByIdReturnsMappedResponseWhenFound() {
        when(destinationRepository.findById(1)).thenReturn(Optional.of(sampleDestination()));

        DestinationResponse result = destinationService.getDestinationById(1);

        assertThat(result.destinationName()).isEqualTo("Mumbai");
    }

    @Test
    void getDestinationByIdThrowsWhenNotFound() {
        when(destinationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> destinationService.getDestinationById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=DestinationServiceImplTest` (from `backend/`)
Expected: FAIL to compile — `Destination`, `DestinationResponse`, `DestinationRepository`, `DestinationServiceImpl` don't exist yet.

- [ ] **Step 3: Create the entity**

Create `backend/src/main/java/com/travelease/backend/admin/entity/Destination.java`:

```java
package com.travelease.backend.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "destinations")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer destinationId;

    @Column(nullable = false, length = 200)
    private String destinationName;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(columnDefinition = "TEXT")
    private String description;
}
```

- [ ] **Step 4: Create the response DTO**

Create `backend/src/main/java/com/travelease/backend/admin/dto/DestinationResponse.java`:

```java
package com.travelease.backend.admin.dto;

public record DestinationResponse(
        Integer destinationId,
        String destinationName,
        String state,
        String country,
        String description
) {
}
```

- [ ] **Step 5: Create the repository**

Create `backend/src/main/java/com/travelease/backend/admin/repository/DestinationRepository.java`:

```java
package com.travelease.backend.admin.repository;

import com.travelease.backend.admin.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Integer> {
}
```

- [ ] **Step 6: Create the service interface and implementation**

Create `backend/src/main/java/com/travelease/backend/admin/service/DestinationService.java`:

```java
package com.travelease.backend.admin.service;

import com.travelease.backend.admin.dto.DestinationResponse;

import java.util.List;

public interface DestinationService {
    List<DestinationResponse> getAllDestinations();
    DestinationResponse getDestinationById(Integer destinationId);
}
```

Create `backend/src/main/java/com/travelease/backend/admin/service/DestinationServiceImpl.java`:

```java
package com.travelease.backend.admin.service;

import com.travelease.backend.admin.dto.DestinationResponse;
import com.travelease.backend.admin.entity.Destination;
import com.travelease.backend.admin.repository.DestinationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;

    @Override
    public List<DestinationResponse> getAllDestinations() {
        return destinationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DestinationResponse getDestinationById(Integer destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination with id " + destinationId + " not found"));
        return toResponse(destination);
    }

    private DestinationResponse toResponse(Destination destination) {
        return new DestinationResponse(
                destination.getDestinationId(),
                destination.getDestinationName(),
                destination.getState(),
                destination.getCountry(),
                destination.getDescription()
        );
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./mvnw test -Dtest=DestinationServiceImplTest` (from `backend/`)
Expected: PASS (3 tests)

---

### Task 2: Seed destination data (backend)

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/shared/config/DemoDataInitializer.java`

**Interfaces:**
- Consumes: `DestinationRepository` (Task 1).
- Produces: 7 seeded `Destination` rows, IDs 1–7 in insertion order. Task 3's integration test depends on IDs 1 and 2 being Mumbai and Goa specifically.

This is seed data with no unit-testable behavior of its own (same as the provider-user seeding earlier this session) — verified by Task 3's integration test, which reads real seeded rows from the database.

- [ ] **Step 1: Add the repository dependency and imports**

In `backend/src/main/java/com/travelease/backend/shared/config/DemoDataInitializer.java`, add these imports (alongside the existing `com.travelease.backend.auth.*` imports):

```java
import com.travelease.backend.admin.entity.Destination;
import com.travelease.backend.admin.repository.DestinationRepository;
```

Add the field, alongside the other `private final ...Repository` fields:

```java
    private final DestinationRepository destinationRepository;
```

- [ ] **Step 2: Call `seedDestinations()` first in `run()`**

Change the start of `run(String... args)` from:

```java
    public void run(String... args) {
        User admin = getOrCreateUser(ADMIN_ID, "Admin User", "admin@travelease.test", "9000000000", Role.ROLE_ADMIN, null);
```

to:

```java
    public void run(String... args) {
        seedDestinations();

        User admin = getOrCreateUser(ADMIN_ID, "Admin User", "admin@travelease.test", "9000000000", Role.ROLE_ADMIN, null);
```

- [ ] **Step 3: Add the `seedDestinations` method**

Add this method (e.g. after `run`, before `seedExpenseData`):

```java
    private void seedDestinations() {
        if (destinationRepository.count() > 0) {
            return;
        }
        saveDestination("Mumbai", "Maharashtra", "India", "Financial capital of India, gateway to the Arabian Sea.");
        saveDestination("Goa", "Goa", "India", "Beach paradise on India's west coast.");
        saveDestination("Manali", "Himachal Pradesh", "India", "Himalayan hill station popular for adventure sports.");
        saveDestination("Jaipur", "Rajasthan", "India", "The Pink City, known for its forts and palaces.");
        saveDestination("Alleppey", "Kerala", "India", "Backwaters and houseboat cruises.");
        saveDestination("Chennai", "Tamil Nadu", "India", "Cultural capital of South India.");
        saveDestination("Coorg", "Karnataka", "India", "Coffee plantations in the Western Ghats.");
    }

    private void saveDestination(String name, String state, String country, String description) {
        Destination destination = new Destination();
        destination.setDestinationName(name);
        destination.setState(state);
        destination.setCountry(country);
        destination.setDescription(description);
        destinationRepository.save(destination);
    }
```

- [ ] **Step 4: Fix the "Demo Goa Trip" destinationId**

In `seedExpenseData`, change:

```java
        trip.setDestinationId(1);
```

to:

```java
        trip.setDestinationId(2);
```

(Destination ID 1 now means Mumbai, not Goa — this trip is named "Demo Goa Trip" and should point at Goa's real ID, 2.)

- [ ] **Step 5: Verify by starting the backend**

Run: `./mvnw spring-boot:run` (from `backend/`) and confirm it reaches `Started BackendApplication` with no exceptions during `DemoDataInitializer.run()`. There's no `DestinationController` yet to query the seeded rows through (Task 3 adds it) — Task 3's integration test verifies the actual seeded data (Mumbai as ID 1, Goa as ID 2) by reading it back through the new endpoint. Stop the backend afterward.

---

### Task 3: `DestinationController` + integration test (backend)

**Files:**
- Create: `backend/src/main/java/com/travelease/backend/admin/controller/DestinationController.java`
- Create: `backend/src/test/java/com/travelease/backend/admin/controller/DestinationFlowIntegrationTest.java`

**Interfaces:**
- Consumes: `DestinationService` (Task 1), seeded data (Task 2).
- Produces: `GET /api/destinations`, `GET /api/destinations/{destinationId}`, both returning the standard `ApiResponse<T>` envelope.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/travelease/backend/admin/controller/DestinationFlowIntegrationTest.java`:

```java
package com.travelease.backend.admin.controller;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DestinationFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String loginAsAlice() {
        LoginRequest loginRequest = new LoginRequest("alice@travelease.test", "password123");
        ResponseEntity<ApiResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, ApiResponse.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().data();
        return (String) loginData.get("accessToken");
    }

    private HttpEntity<Void> authedRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void listDestinationsReturnsSeededDestinationsWithMumbaiFirstAndGoaSecond() {
        String token = loginAsAlice();

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/destinations", HttpMethod.GET, authedRequest(token), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) response.getBody().data();
        assertThat(destinations.size()).isGreaterThanOrEqualTo(7);
        assertThat(destinations.get(0).get("destinationName")).isEqualTo("Mumbai");
        assertThat(destinations.get(1).get("destinationName")).isEqualTo("Goa");
    }

    @Test
    void getDestinationByIdReturnsMumbaiForId1() {
        String token = loginAsAlice();

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/destinations/1", HttpMethod.GET, authedRequest(token), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> destination = (Map<String, Object>) response.getBody().data();
        assertThat(destination.get("destinationName")).isEqualTo("Mumbai");
        assertThat(destination.get("state")).isEqualTo("Maharashtra");
    }

    @Test
    void getDestinationByIdReturns404ForUnknownId() {
        String token = loginAsAlice();

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/destinations/99999", HttpMethod.GET, authedRequest(token), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listDestinationsRejectsRequestsWithoutToken() {
        ResponseEntity<ApiResponse> response =
                restTemplate.getForEntity("/api/destinations", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=DestinationFlowIntegrationTest` (from `backend/`)
Expected: FAIL to compile — `DestinationController` doesn't exist, so `/api/destinations` isn't mapped (404/no route).

- [ ] **Step 3: Create the controller**

Create `backend/src/main/java/com/travelease/backend/admin/controller/DestinationController.java`:

```java
package com.travelease.backend.admin.controller;

import com.travelease.backend.admin.dto.DestinationResponse;
import com.travelease.backend.admin.service.DestinationService;
import com.travelease.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> getAllDestinations() {
        List<DestinationResponse> response = destinationService.getAllDestinations();
        return ResponseEntity.ok(ApiResponse.success(response, "Destinations retrieved"));
    }

    @GetMapping("/{destinationId}")
    public ResponseEntity<ApiResponse<DestinationResponse>> getDestinationById(
            @PathVariable Integer destinationId
    ) {
        DestinationResponse response = destinationService.getDestinationById(destinationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Destination retrieved"));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=DestinationFlowIntegrationTest` (from `backend/`)
Expected: PASS (4 tests)

- [ ] **Step 5: Run the full backend test suite**

Run: `./mvnw test` (from `backend/`)
Expected: all pre-existing tests pass, plus the 3 from Task 1 and 4 from this task (7 new total).

---

### Task 4: `core/destinations/` module (frontend)

**Files:**
- Create: `frontend/src/app/core/destinations/destination.models.ts`
- Create: `frontend/src/app/core/destinations/destinations.service.ts`
- Create: `frontend/src/app/core/destinations/destinations.service.spec.ts`

**Interfaces:**
- Produces: `Destination` interface, `DestinationsService.listDestinations(): Observable<Destination[]>`. Tasks 5–6 depend on both.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/core/destinations/destinations.service.spec.ts`:

```ts
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/core/destinations/destinations.service.spec.ts' --watch=false` (from `frontend/`)
Expected: FAIL — `@app/core/destinations/destinations.service` and `destination.models` don't exist.

- [ ] **Step 3: Create `destination.models.ts`**

Create `frontend/src/app/core/destinations/destination.models.ts`:

```ts
export interface Destination {
  destinationId: number;
  destinationName: string;
  state: string;
  country: string;
  description: string;
}
```

- [ ] **Step 4: Create `destinations.service.ts`**

Create `frontend/src/app/core/destinations/destinations.service.ts`:

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
import { Destination } from '@app/core/destinations/destination.models';

@Injectable({ providedIn: 'root' })
export class DestinationsService {
  private readonly http = inject(HttpClient);

  listDestinations(): Observable<Destination[]> {
    return this.http
      .get<ApiResponse<Destination[]>>(`${API_BASE_URL}/api/destinations`)
      .pipe(map((response) => response.data));
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/core/destinations/destinations.service.spec.ts' --watch=false` (from `frontend/`)
Expected: PASS (2 tests)

---

### Task 5: `new-trip` — destination dropdown

**Files:**
- Modify: `frontend/src/app/features/trips/components/new-trip/new-trip.ts`
- Modify: `frontend/src/app/features/trips/components/new-trip/new-trip.html`
- Modify: `frontend/src/app/features/trips/components/new-trip/new-trip.spec.ts`

**Interfaces:**
- Consumes: `DestinationsService.listDestinations()` (Task 4).
- Produces: `NewTrip` gains `destinations: Signal<Destination[]>`, `destinationsLoading: Signal<boolean>`, `destinationsError: Signal<boolean>`, `selectedDestinationId: Signal<string>` (all `protected`), and `onDestinationChange`/`destinationLabel` methods.

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/trips/components/new-trip/new-trip.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';
import { TripsService } from '@app/features/trips/services/trips.service';
import { CreateTripPayload, Trip } from '@app/features/trips/services/trip.models';
import { DestinationsService } from '@app/core/destinations/destinations.service';
import { Destination } from '@app/core/destinations/destination.models';

const SAMPLE_DESTINATIONS: Destination[] = [
  { destinationId: 1, destinationName: 'Mumbai', state: 'Maharashtra', country: 'India', description: '' },
  { destinationId: 2, destinationName: 'Goa', state: 'Goa', country: 'India', description: '' },
];

const CREATED_TRIP: Trip = {
  tripId: 'bbbbbbbb-0000-0000-0000-000000000002',
  tripName: 'Goa Beach Escape',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Bengaluru',
  destinationId: 1,
  budgetAmount: 18000,
  categoryId: 4,
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  status: 'PLANNING',
  viewerRole: 'ORGANIZER',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
};

async function setup(
  tripsService: Partial<TripsService>,
  destinationsService: Partial<DestinationsService> = { listDestinations: () => of(SAMPLE_DESTINATIONS) },
) {
  await TestBed.configureTestingModule({
    imports: [NewTrip],
    providers: [
      provideRouter([]),
      provideIcons({ lucideArrowLeft }),
      { provide: TripsService, useValue: tripsService },
      { provide: DestinationsService, useValue: destinationsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(NewTrip);
  const router = TestBed.inject(Router);
  const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return { fixture, navigateSpy };
}

function fillAndSubmit(el: HTMLElement) {
  (el.querySelector('#name') as HTMLInputElement).value = 'Goa Beach Escape';
  (el.querySelector('#budget') as HTMLInputElement).value = '18000';
  (el.querySelector('#source') as HTMLInputElement).value = 'Bengaluru';
  (el.querySelector('#start-date') as HTMLInputElement).value = '2026-08-01';
  (el.querySelector('#end-date') as HTMLInputElement).value = '2026-08-05';
  const form = el.querySelector('form')!;
  form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
}

describe('NewTrip', () => {
  it('submits with the default trip type and first-loaded destination, navigates to the created trip', async () => {
    const createTrip = vi.fn().mockReturnValue(of(CREATED_TRIP));
    const { fixture, navigateSpy } = await setup({ createTrip });
    const el = fixture.nativeElement as HTMLElement;

    fillAndSubmit(el);
    await fixture.whenStable();

    const expectedPayload: CreateTripPayload = {
      tripName: 'Goa Beach Escape',
      sourceLocation: 'Bengaluru',
      destinationId: 1,
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

  it('shows an error and disables submit when destinations fail to load', async () => {
    const createTrip = vi.fn();
    const { fixture } = await setup(
      { createTrip },
      { listDestinations: () => throwError(() => new Error('boom')) },
    );
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Could not load destinations');
    const submitBtn = el.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submitBtn.disabled).toBe(true);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/new-trip/new-trip.spec.ts' --watch=false` (from `frontend/`)
Expected: FAIL — `NewTrip` doesn't inject `DestinationsService` yet, no `destinationsError`/loading UI.

- [ ] **Step 3: Replace `new-trip.ts`**

Replace the contents of `frontend/src/app/features/trips/components/new-trip/new-trip.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { TripsService } from '@app/features/trips/services/trips.service';
import { CreateTripPayload } from '@app/features/trips/services/trip.models';
import { DestinationsService } from '@app/core/destinations/destinations.service';
import { Destination } from '@app/core/destinations/destination.models';

@Component({
  selector: 'app-new-trip',
  imports: [
    RouterLink,
    NgIcon,
    HlmButtonImports,
    HlmCardImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
    PageHeader,
  ],
  templateUrl: './new-trip.html',
})
export class NewTrip {
  private readonly router = inject(Router);
  private readonly tripsService = inject(TripsService);
  private readonly destinationsService = inject(DestinationsService);

  protected readonly tripTypes = ['Solo', 'Couple', 'Family', 'Friends', 'Corporate'];
  protected readonly tripType = signal('Friends');
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly destinations = signal<Destination[]>([]);
  protected readonly destinationsLoading = signal(true);
  protected readonly destinationsError = signal(false);
  protected readonly selectedDestinationId = signal('');

  constructor() {
    this.destinationsService.listDestinations().subscribe({
      next: (destinations) => {
        this.destinations.set(destinations);
        this.destinationsLoading.set(false);
        if (destinations.length > 0) {
          this.selectedDestinationId.set(String(destinations[0].destinationId));
        }
      },
      error: () => {
        this.destinationsError.set(true);
        this.destinationsLoading.set(false);
      },
    });
  }

  protected onTripTypeChange(value: string | null | undefined): void {
    if (value) {
      this.tripType.set(value);
    }
  }

  protected onDestinationChange(value: string | null | undefined): void {
    if (value) {
      this.selectedDestinationId.set(value);
    }
  }

  protected destinationLabel(destination: Destination): string {
    return `${destination.destinationName}, ${destination.state}`;
  }

  protected onSubmit(
    event: Event,
    name: string,
    budget: string,
    source: string,
    startDate: string,
    endDate: string,
  ): void {
    event.preventDefault();
    this.error.set(null);

    if (!this.selectedDestinationId()) {
      this.error.set('Please select a destination.');
      return;
    }

    const categoryId = this.tripTypes.indexOf(this.tripType()) + 1;
    const payload: CreateTripPayload = {
      tripName: name,
      sourceLocation: source,
      destinationId: Number(this.selectedDestinationId()),
      budgetAmount: Number(budget),
      categoryId,
      startDate,
      endDate,
    };

    this.submitting.set(true);
    this.tripsService.createTrip(payload).subscribe({
      next: (trip) => {
        this.submitting.set(false);
        this.router.navigate(['/trips', trip.tripId]);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.extractErrorMessage(err));
      },
    });
  }

  private extractErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const apiError = err.error?.error;
      if (apiError) {
        const details: string[] = apiError.details ?? [];
        return details.length > 0 ? `${apiError.message}\n${details.join('\n')}` : apiError.message;
      }
    }
    return 'Something went wrong creating your trip. Please try again.';
  }
}
```

- [ ] **Step 4: Replace `new-trip.html`**

Replace the contents of `frontend/src/app/features/trips/components/new-trip/new-trip.html`:

```html
<a hlmBtn variant="ghost" size="sm" class="mb-3" routerLink="/trips">
  <ng-icon name="lucideArrowLeft" class="h-4 w-4 mr-1" />Back to trips
</a>
<app-page-header
  title="Create a new trip"
  subtitle="Set the essentials. You can invite members and book everything next."
/>

<div hlmCard class="max-w-3xl">
  <div hlmCardContent class="pt-6">
    <form
      class="grid grid-cols-1 md:grid-cols-2 gap-5"
      (submit)="onSubmit($event, nameInput.value, budgetInput.value, sourceInput.value, startDateInput.value, endDateInput.value)"
    >
      <div class="md:col-span-2 space-y-2">
        <label hlmLabel for="name">Trip Name</label>
        <input hlmInput id="name" placeholder="Goa Beach Escape" #nameInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Trip Type</label>
        <hlm-select [value]="tripType()" (valueChange)="onTripTypeChange($event)">
          <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
          <ng-template hlmSelectPortal>
            <hlm-select-content>
              @for (t of tripTypes; track t) {
                <hlm-select-item [value]="t">{{ t }}</hlm-select-item>
              }
            </hlm-select-content>
          </ng-template>
        </hlm-select>
      </div>
      <div class="space-y-2">
        <label hlmLabel for="budget">Budget (₹)</label>
        <input hlmInput id="budget" type="number" min="1" step="1" placeholder="18000" #budgetInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="source">Source Location</label>
        <input hlmInput id="source" placeholder="Bengaluru" #sourceInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Destination</label>
        @if (destinationsLoading()) {
          <p class="text-xs text-muted-foreground py-2">Loading destinations…</p>
        } @else if (destinationsError()) {
          <p class="text-xs text-destructive py-2">Could not load destinations.</p>
        } @else {
          <hlm-select [value]="selectedDestinationId()" (valueChange)="onDestinationChange($event)">
            <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
            <ng-template hlmSelectPortal>
              <hlm-select-content>
                @for (d of destinations(); track d.destinationId) {
                  <hlm-select-item [value]="d.destinationId.toString()">{{ destinationLabel(d) }}</hlm-select-item>
                }
              </hlm-select-content>
            </ng-template>
          </hlm-select>
        }
      </div>
      <div class="space-y-2">
        <label hlmLabel for="start-date">Start Date</label>
        <input hlmInput id="start-date" type="date" #startDateInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="end-date">End Date</label>
        <input hlmInput id="end-date" type="date" #endDateInput />
      </div>
      @if (error()) {
        <p class="md:col-span-2 text-xs text-destructive whitespace-pre-line">{{ error() }}</p>
      }
      <div class="md:col-span-2 flex gap-3 pt-2">
        <button hlmBtn type="submit" [disabled]="submitting() || destinationsLoading() || destinationsError()">
          Create Trip
        </button>
        <a hlmBtn variant="outline" routerLink="/trips">Cancel</a>
      </div>
    </form>
  </div>
</div>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/new-trip/new-trip.spec.ts' --watch=false` (from `frontend/`)
Expected: PASS (3 tests)

---

### Task 6: `trip-list` — real destination names

**Files:**
- Modify: `frontend/src/app/features/trips/components/trip-list/trip-list.ts`
- Modify: `frontend/src/app/features/trips/components/trip-list/trip-list.html`
- Modify: `frontend/src/app/features/trips/components/trip-list/trip-list.spec.ts`

**Interfaces:**
- Consumes: `DestinationsService.listDestinations()` (Task 4).
- Produces: `TripList` gains `destinationLabel(destinationId: number): string` (`protected`), backed by a `destinationNames: Signal<Map<number, string>>`.

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/trips/components/trip-list/trip-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';
import { DestinationsService } from '@app/core/destinations/destinations.service';
import { Destination } from '@app/core/destinations/destination.models';

const SAMPLE_TRIPS: Trip[] = [
  {
    tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
    tripName: 'Goa Beach Escape',
    organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
    sourceLocation: 'Bengaluru',
    destinationId: 3,
    budgetAmount: 18000,
    categoryId: 1,
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    status: 'PLANNING',
    viewerRole: 'ORGANIZER',
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z',
  },
];

const SAMPLE_DESTINATIONS: Destination[] = [
  { destinationId: 3, destinationName: 'Manali', state: 'Himachal Pradesh', country: 'India', description: '' },
];

async function setup(
  tripsService: Partial<TripsService>,
  destinationsService: Partial<DestinationsService> = { listDestinations: () => of(SAMPLE_DESTINATIONS) },
) {
  await TestBed.configureTestingModule({
    imports: [TripList],
    providers: [
      provideRouter([]),
      provideIcons({ lucidePlus, lucideCalendar, lucideWallet, lucideMapPin }),
      { provide: TripsService, useValue: tripsService },
      { provide: DestinationsService, useValue: destinationsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(TripList);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

describe('TripList', () => {
  it('shows a loading message before the trips arrive', async () => {
    const subject = new Subject<Trip[]>();
    const fixture = await setup({ listMyTrips: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading your trips');
  });

  it('renders trip cards with the real destination name once destinations load', async () => {
    const fixture = await setup({ listMyTrips: () => of(SAMPLE_TRIPS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Goa Beach Escape');
    expect(el.textContent).toContain('Bengaluru');
    expect(el.textContent).toContain('Manali');
    expect(el.textContent).toContain('18,000');
    const link = el.querySelector(`a[href="/trips/${SAMPLE_TRIPS[0].tripId}"]`);
    expect(link).not.toBeNull();
  });

  it('falls back to "Destination #<id>" when destinations fail to load', async () => {
    const fixture = await setup(
      { listMyTrips: () => of(SAMPLE_TRIPS) },
      { listDestinations: () => throwError(() => new Error('boom')) },
    );
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Destination #3');
  });

  it('shows an empty-state message when there are no trips', async () => {
    const fixture = await setup({ listMyTrips: () => of([]) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No trips yet');
  });

  it('shows an error message when the request fails', async () => {
    const fixture = await setup({ listMyTrips: () => throwError(() => new Error('network error')) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-list/trip-list.spec.ts' --watch=false` (from `frontend/`)
Expected: FAIL — 2 new tests fail (`TripList` doesn't inject `DestinationsService`, still shows `Destination #<id>` unconditionally).

- [ ] **Step 3: Update `trip-list.ts`**

Replace the contents of `frontend/src/app/features/trips/components/trip-list/trip-list.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';
import { DestinationsService } from '@app/core/destinations/destinations.service';

@Component({
  selector: 'app-trip-list',
  imports: [
    RouterLink,
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    PageHeader,
    StatusBadge,
    DestinationPill,
  ],
  templateUrl: './trip-list.html',
})
export class TripList {
  private readonly tripsService = inject(TripsService);
  private readonly destinationsService = inject(DestinationsService);

  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly destinationNames = signal<Map<number, string>>(new Map());

  constructor() {
    this.tripsService.listMyTrips().subscribe({
      next: (trips) => {
        this.trips.set(trips);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading your trips. Please try again.');
        this.loading.set(false);
      },
    });

    this.destinationsService.listDestinations().subscribe({
      next: (destinations) => {
        this.destinationNames.set(
          new Map(destinations.map((d) => [d.destinationId, d.destinationName])),
        );
      },
      error: () => {
        // Destination names are an enhancement, not required to view trips —
        // cards fall back to the "Destination #<id>" placeholder below.
      },
    });
  }

  protected destinationLabel(destinationId: number): string {
    return this.destinationNames().get(destinationId) ?? `Destination #${destinationId}`;
  }
}
```

- [ ] **Step 4: Update `trip-list.html`**

In `frontend/src/app/features/trips/components/trip-list/trip-list.html`, change:

```html
            <app-destination-pill [from]="t.sourceLocation" [to]="'Destination #' + t.destinationId" />
```

to:

```html
            <app-destination-pill [from]="t.sourceLocation" [to]="destinationLabel(t.destinationId)" />
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-list/trip-list.spec.ts' --watch=false` (from `frontend/`)
Expected: PASS (5 tests)

---

### Task 7: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–6.

- [ ] **Step 1: Full backend test suite**

Run (from `backend/`): `./mvnw test`
Expected: all tests pass, no regressions.

- [ ] **Step 2: Full frontend test suite**

Run (from `frontend/`): `npx ng test --watch=false`
Expected: all pre-existing tests pass, plus every new/modified test from Tasks 4–6.

- [ ] **Step 3: Full frontend production build**

Run (from `frontend/`): `npx ng build`
Expected: completes with no errors.

- [ ] **Step 4: Manual end-to-end check**

Start the backend (`./mvnw spring-boot:run` from `backend/`, wait for `Started BackendApplication`) and confirm the seeded catalog:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"alice@travelease.test","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
curl -s http://localhost:8080/api/destinations -H "Authorization: Bearer $TOKEN"
```

Expected: 7 destinations, with Mumbai as ID 1 and Goa as ID 2.

Then start the frontend dev server (`npx ng serve --port 4200` from `frontend/`, reuse if already running), log in as a traveler, and confirm:
- `/trips/new` shows a real destination dropdown (not a number input), defaulting to Mumbai.
- Creating a trip with a selected destination, then viewing `/trips`, shows the real destination name on that trip's card (not `Destination #<id>`).
- The pre-existing "Demo Goa Trip" (Alice's seeded trip) now shows "Goa" as its destination, not "Mumbai".

Stop any dev servers you started for this check (not ones already running) once done.

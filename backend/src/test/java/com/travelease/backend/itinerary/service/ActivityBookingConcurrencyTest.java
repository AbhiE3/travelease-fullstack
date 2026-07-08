package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.exception.SeatUnavailableException;
import com.travelease.backend.itinerary.dto.ActivityProviderRequest;
import com.travelease.backend.itinerary.dto.ActivitySlotRequest;
import com.travelease.backend.itinerary.dto.ActivitySlotResponse;
import com.travelease.backend.itinerary.dto.CreateActivityBookingRequest;
import com.travelease.backend.itinerary.entity.ActivityBookingStatus;
import com.travelease.backend.itinerary.repository.ActivityBookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the pessimistic-lock capacity check in
 * ActivityBookingServiceImpl.createBooking actually serializes concurrent
 * bookings against real transactions/H2 - a Mockito-mocked repository cannot
 * demonstrate this, since @Lock(PESSIMISTIC_WRITE) only has meaning against a
 * real Connection/transaction manager.
 */
@SpringBootTest
class ActivityBookingConcurrencyTest {

    @Autowired
    private ActivityProviderService activityProviderService;
    @Autowired
    private ActivityBookingService activityBookingService;
    @Autowired
    private ActivityBookingRepository activityBookingRepository;
    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private User createTraveler(String email) {
        User user = new User();
        user.setName("Concurrency Test Traveler");
        user.setEmail(email);
        user.setPhone("9999999999");
        user.setPasswordHash("not-used-in-this-test");
        user.setRole(Role.ROLE_TRAVELER);
        return userRepository.save(user);
    }

    @Test
    void concurrentOverlappingBookingsCannotExceedSlotCapacity() throws InterruptedException {
        // Seeded Activity Provider tenant 201 (activityprovider1@travelease.com).
        authenticateAs("activityprovider1@travelease.com", "ROLE_ACTIVITY_PROVIDER");
        var activity = activityProviderService.createActivity(
                new ActivityProviderRequest(1, "Concurrency Test Activity", 1.0, "09:00", "10:00", "desc", null));
        ActivitySlotResponse slot = activityProviderService.createSlot(activity.activityId(),
                new ActivitySlotRequest(LocalDate.now().plusDays(10), LocalTime.of(9, 0), LocalTime.of(10, 0),
                        new BigDecimal("200.00"), 5));
        SecurityContextHolder.clearContext();

        User travelerA = createTraveler("concurrency-traveler-a@travelease.com");
        User travelerB = createTraveler("concurrency-traveler-b@travelease.com");

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityRejectedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable bookThreeParticipants = () -> {
            try {
                startGate.await();
                try {
                    activityBookingService.createBooking(new CreateActivityBookingRequest(slot.activitySlotId(), 3));
                    successCount.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    capacityRejectedCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                SecurityContextHolder.clearContext();
                doneLatch.countDown();
            }
        };

        executor.submit(() -> {
            authenticateAs(travelerA.getEmail(), "ROLE_TRAVELER");
            bookThreeParticipants.run();
        });
        executor.submit(() -> {
            authenticateAs(travelerB.getEmail(), "ROLE_TRAVELER");
            bookThreeParticipants.run();
        });

        startGate.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Capacity is 5 and each request asks for 3 - both succeeding would
        // overshoot to 6, so exactly one must be serialized out by the
        // pessimistic lock's authoritative re-check.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(capacityRejectedCount.get()).isEqualTo(1);

        int totalConfirmedParticipants = activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(
                slot.activitySlotId(), List.of(ActivityBookingStatus.CONFIRMED, ActivityBookingStatus.ATTENDED, ActivityBookingStatus.NO_SHOW));
        assertThat(totalConfirmedParticipants).isEqualTo(3);
    }
}

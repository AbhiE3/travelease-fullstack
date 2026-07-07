package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.response.BookingResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.mapper.BookingMapper;
import com.travelease.backend.busbooking.repository.BookingRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplOwnershipTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void adminCanReadAnotherTravelersBooking() {
        UUID ownerId = UUID.randomUUID();
        Booking booking = Booking.builder().id(1L).userId(ownerId).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_ADMIN"));
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().build());

        assertThatCode(() -> bookingService.getBookingById(1L)).doesNotThrowAnyException();
    }

    @Test
    void nonOwningTravelerIsRejected() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Booking booking = Booking.builder().id(1L).userId(ownerId).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(securityUtil.getCurrentUserId()).thenReturn(otherId);

        assertThatThrownBy(() -> bookingService.getBookingById(1L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}

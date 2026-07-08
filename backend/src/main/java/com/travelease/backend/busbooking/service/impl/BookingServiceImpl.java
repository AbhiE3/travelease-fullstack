package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.*;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.*;
import com.travelease.backend.busbooking.entity.enums.*;
import com.travelease.backend.busbooking.exception.BookingException;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.BookingMapper;
import com.travelease.backend.busbooking.repository.*;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.BookingService;
import com.travelease.backend.busbooking.service.CouponService;
import com.travelease.backend.busbooking.service.RefundService;
import com.travelease.backend.busbooking.service.SeatAllocationService;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BusScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final BookingTimelineRepository timelineRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final BookingMapper bookingMapper;
    private final SecurityUtil securityUtil;
    private final SeatAllocationService seatAllocationService;
    private final CouponService couponService;
    private final RefundService refundService;
    private final TripRepository tripRepository;
    private final TripAuthorizationService tripAuthorizationService;

    private static final Set<BookingStatus> TRIP_ATTACHABLE_STATUSES =
            Set.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // CREATE BOOKING (backward-compatible: creates + auto-confirms)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        java.util.UUID userId = securityUtil.getCurrentUserId();
        BusSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", request.getScheduleId()));

        validateScheduleForBooking(schedule);
        seatAllocationService.validateSeatsForBooking(schedule.getId(), request.getSeatIds(), userId);

        if (schedule.getAvailableSeats() < request.getSeatIds().size()) {
            throw new BookingException("Not enough available seats. Available: " + schedule.getAvailableSeats());
        }

        List<Long> availableSeatIds = seatRepository.findAvailableSeatsForSchedule(schedule.getId())
                .stream().map(Seat::getId).toList();
        for (Long seatId : request.getSeatIds()) {
            if (!availableSeatIds.contains(seatId)) {
                throw new BookingException("Seat with ID " + seatId + " is not available for this schedule");
            }
        }

        List<Seat> seatsToBook = request.getSeatIds().stream()
                .map(id -> seatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seat", "id", id)))
                .toList();

        if (request.getPassengerDetails().size() != seatsToBook.size()) {
            throw new BookingException("Passenger details count must match seat count");
        }

        Map<Long, PassengerDetailDto> passengerMap = request.getPassengerDetails().stream()
                .collect(Collectors.toMap(PassengerDetailDto::getSeatId, p -> p));

        Map<Long, String> passengerGenderMap = passengerMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getPassengerGender()));
        seatAllocationService.validateLadiesSeats(new ArrayList<>(seatsToBook), passengerGenderMap);

        // Create booking as PENDING
        Booking booking = Booking.builder()
                .userId(userId)
                .schedule(schedule)
                .bookingReference(generateBookingReference())
                .status(BookingStatus.PENDING)
                .totalFare(schedule.getFare() * seatsToBook.size())
                .paymentStatus(PaymentStatus.PENDING)
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .couponCode(request.getCouponCode())
                .couponDiscount(0.0)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .bookingSeats(new ArrayList<>())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        addTimelineEntry(savedBooking, BookingEvent.BOOKING_CREATED, "Booking created with reference " + savedBooking.getBookingReference());

        // Create booking seats
        List<BookingSeat> bookingSeats = new ArrayList<>();
        boolean primarySet = false;
        for (Seat seat : seatsToBook) {
            PassengerDetailDto passenger = passengerMap.getOrDefault(seat.getId(),
                    request.getPassengerDetails().get(seatsToBook.indexOf(seat)));

            boolean isPrimary = !primarySet && (Boolean.TRUE.equals(passenger.getIsPrimary()) || bookingSeats.isEmpty());
            if (isPrimary) primarySet = true;

            bookingSeats.add(BookingSeat.builder()
                    .booking(savedBooking)
                    .seat(seat)
                    .passengerName(passenger.getPassengerName())
                    .passengerAge(passenger.getPassengerAge())
                    .passengerGender(passenger.getPassengerGender())
                    .passengerEmail(passenger.getPassengerEmail())
                    .passengerPhone(passenger.getPassengerPhone())
                    .isPrimary(isPrimary)
                    .build());
        }
        savedBooking.setBookingSeats(bookingSeats);
        bookingRepository.save(savedBooking);

        // Auto-confirm (simulate payment)
        return confirmBookingInternal(savedBooking, request.getCouponCode());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // CONFIRM BOOKING
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);
        return confirmBookingInternal(booking, booking.getCouponCode());
    }

    private BookingResponse confirmBookingInternal(Booking booking, String couponCode) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return bookingMapper.toResponse(booking);
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot confirm booking in status: " + booking.getStatus());
        }

        // Simulate payment
        addTimelineEntry(booking, BookingEvent.PAYMENT_INITIATED, "Payment initiated for amount " + booking.getTotalFare());

        // Apply coupon if provided
        double couponDiscount = 0.0;
        if (couponCode != null && !couponCode.isBlank()) {
            try {
                CouponResponse validated = couponService.validateCoupon(couponCode, booking.getTotalFare(),
                        booking.getSchedule().getRoute().getId(),
                        booking.getSchedule().getBus().getBusType() != null ? booking.getSchedule().getBus().getBusType().name() : null);
                if (validated.getDiscountType() == DiscountType.PERCENTAGE) {
                    couponDiscount = booking.getTotalFare() * validated.getDiscountValue() / 100.0;
                } else {
                    couponDiscount = validated.getDiscountValue();
                }
                if (validated.getMaxDiscount() != null && couponDiscount > validated.getMaxDiscount()) {
                    couponDiscount = validated.getMaxDiscount();
                }
                couponService.incrementCouponUsage(couponCode);
                addTimelineEntry(booking, BookingEvent.PAYMENT_COMPLETED, "Coupon '" + couponCode + "' applied, discount: " + couponDiscount);
            } catch (Exception e) {
                log.warn("Coupon '{}' could not be applied: {}", couponCode, e.getMessage());
                addTimelineEntry(booking, BookingEvent.PAYMENT_COMPLETED, "Coupon '" + couponCode + "' rejected: " + e.getMessage());
            }
        }

        double finalFare = booking.getTotalFare() - couponDiscount;
        booking.setTotalFare(Math.max(finalFare, 0.0));
        booking.setCouponDiscount(couponDiscount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setExpiresAt(null);

        // Generate ticket
        String ticketNumber = generateTicketNumber();
        booking.setTicketNumber(ticketNumber);
        booking.setQrCodeString(generateQrCodeString(booking));

        bookingRepository.save(booking);

        addTimelineEntry(booking, BookingEvent.PAYMENT_COMPLETED, "Payment completed (simulated)");
        addTimelineEntry(booking, BookingEvent.BOOKING_CONFIRMED, "Booking confirmed");
        addTimelineEntry(booking, BookingEvent.TICKET_GENERATED, "Ticket " + ticketNumber + " generated");

        // Decrement available seats
        BusSchedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() - booking.getBookingSeats().size());
        scheduleRepository.save(schedule);

        // Release locks
        List<Long> seatIds = booking.getBookingSeats().stream().map(bs -> bs.getSeat().getId()).toList();
        seatAllocationService.releaseLocksForBooking(schedule.getId(), seatIds, booking.getUserId());

        return bookingMapper.toResponse(booking);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // CANCEL BOOKING
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot cancel booking in status: " + booking.getStatus());
        }

        int seatsCount = booking.getBookingSeats().size();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        booking.setTicketStatus("CANCELLED");
        bookingRepository.save(booking);

        BusSchedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() + seatsCount);
        scheduleRepository.save(schedule);

        addTimelineEntry(booking, BookingEvent.BOOKING_CANCELLED, "Booking cancelled by user");

        return bookingMapper.toResponse(booking);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // MODIFY BOOKING
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public BookingResponse modifyBooking(BookingModificationRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));
        ensureOwnership(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.FAILED) {
            throw new BookingException("Cannot modify booking in status: " + booking.getStatus());
        }

        // Update contact info
        if (request.getContactEmail() != null) booking.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) booking.setContactPhone(request.getContactPhone());

        // Update passenger details
        if (request.getUpdatedPassengerDetails() != null && !request.getUpdatedPassengerDetails().isEmpty()) {
            Map<Long, PassengerDetailDto> updates = request.getUpdatedPassengerDetails().stream()
                    .filter(p -> p.getSeatId() != null)
                    .collect(Collectors.toMap(PassengerDetailDto::getSeatId, p -> p));

            for (BookingSeat bs : booking.getBookingSeats()) {
                PassengerDetailDto update = updates.get(bs.getSeat().getId());
                if (update != null) {
                    if (update.getPassengerName() != null) bs.setPassengerName(update.getPassengerName());
                    if (update.getPassengerAge() != null) bs.setPassengerAge(update.getPassengerAge());
                    if (update.getPassengerGender() != null) bs.setPassengerGender(update.getPassengerGender());
                    if (update.getPassengerEmail() != null) bs.setPassengerEmail(update.getPassengerEmail());
                    if (update.getPassengerPhone() != null) bs.setPassengerPhone(update.getPassengerPhone());
                    if (Boolean.TRUE.equals(update.getIsPrimary())) {
                        booking.getBookingSeats().forEach(s -> s.setIsPrimary(false));
                        bs.setIsPrimary(true);
                    }
                }
            }
        }

        bookingRepository.save(booking);
        addTimelineEntry(booking, BookingEvent.BOOKING_MODIFIED, "Booking details modified");

        return bookingMapper.toResponse(booking);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // COMPLETE BOOKING
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Completes every still-CONFIRMED booking on a schedule. Called from
     * TripServiceImpl when a Trip transitions to COMPLETED - joins that same
     * @Transactional call so trip completion and booking completion commit or
     * roll back together. PENDING/RESERVED/CANCELLED/FAILED/EXPIRED bookings are
     * left untouched, and already-COMPLETED bookings are skipped (idempotent).
     */
    @Override
    @Transactional
    public void completeBookingsForSchedule(Long scheduleId) {
        List<Booking> bookings = bookingRepository.findByScheduleId(scheduleId);
        for (Booking booking : bookings) {
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setCompletedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            addTimelineEntry(booking, BookingEvent.BOOKING_COMPLETED, "Booking completed automatically after trip completion");
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // EXPIRE BOOKINGS (scheduler)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public void expireBookings() {
        List<Booking> expired = bookingRepository.findExpiredBookings(
                List.of(BookingStatus.PENDING, BookingStatus.RESERVED), LocalDateTime.now());
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);
            addTimelineEntry(booking, BookingEvent.BOOKING_EXPIRED, "Booking expired due to timeout");
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} bookings", expired.size());
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // READ OPERATIONS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        ensureOwnership(booking);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String ref) {
        Booking booking = bookingRepository.findByBookingReference(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", ref));
        ensureOwnership(booking);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingTimelineResponse> getBookingTimeline(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);
        return timelineRepository.findByBookingIdOrderByOccurredAtAsc(bookingId)
                .stream().map(bookingMapper::toTimelineResponse).toList();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // SEARCH / FILTER / PAGINATION / SORTING
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public PaginatedSearchResponse<BookingHistoryResponse> getBookings(String scope, BookingStatus status, String reference, LocalDate from, LocalDate to, Pageable pageable) {
        java.util.UUID userId = securityUtil.getCurrentUserId();
        boolean isAdmin = securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN");
        LocalDate today = LocalDate.now();

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdmin) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (reference != null && !reference.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("bookingReference")), "%" + reference.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("schedule").get("travelDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("schedule").get("travelDate"), to));
            }
            if (scope != null) {
                String normalized = scope.trim().toUpperCase();
                if ("UPCOMING".equals(normalized)) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("schedule").get("travelDate"), today));
                } else if ("PAST".equals(normalized)) {
                    predicates.add(cb.lessThan(root.get("schedule").get("travelDate"), today));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        List<BookingHistoryResponse> content = page.getContent().stream()
                .map(bookingMapper::toHistoryResponse).toList();

        return PaginatedSearchResponse.<BookingHistoryResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // TICKET MANAGEMENT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);
        if (booking.getTicketNumber() == null) {
            throw new BookingException("No ticket generated for this booking");
        }
        return bookingMapper.toTicketResponse(booking);
    }

    @Transactional
    public TicketResponse reprintTicket(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);
        if (booking.getTicketNumber() == null) {
            throw new BookingException("No ticket generated for this booking");
        }
        addTimelineEntry(booking, BookingEvent.TICKET_REPRINTED, "Ticket reprinted");
        return bookingMapper.toTicketResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse verifyTicket(String ticketNumber) {
        // Fix C-1: Use direct DB lookup instead of loading all bookings into memory
        Booking booking = bookingRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "ticketNumber", ticketNumber));
        return bookingMapper.toTicketResponse(booking);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PHASE 7 â€“ ENHANCED CANCELLATION
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public CancellationResponse cancelBookingUnified(Long bookingId, CancellationRequest request) {
        CancellationRequest effectiveRequest = request != null ? request : new CancellationRequest(bookingId, CancellationReason.OTHER, null);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot cancel booking in status: " + booking.getStatus());
        }

        // Calculate refund using cancellation policy
        double originalFare = booking.getTotalFare();
        double cancellationCharge = calculateCancellationCharge(booking);
        double refundAmount = Math.max(originalFare - cancellationCharge, 0.0);

        // Update booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(effectiveRequest.getReason());
        booking.setCancellationReasonText(effectiveRequest.getReasonText());
        booking.setTotalRefundAmount(refundAmount);
        booking.setTicketStatus("CANCELLED");
        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        bookingRepository.save(booking);

        // Release seats
        int seatsCount = booking.getBookingSeats().size();
        BusSchedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() + seatsCount);
        scheduleRepository.save(schedule);

        // Initiate refund
        RefundResponse refund = null;
        if (refundAmount > 0) {
            refund = refundService.initiateRefund(booking.getId(), refundAmount, effectiveRequest.getReason().name());
            // Auto-process refund (simulate approval)
            refund = refundService.processRefund(refund.getId());
            refund = refundService.approveRefund(refund.getId());
            refund = refundService.completeRefund(refund.getId());
        }

        addTimelineEntry(booking, BookingEvent.BOOKING_CANCELLED,
                "Booking cancelled. Reason: " + effectiveRequest.getReason() + ", Refund: " + refundAmount);

        return CancellationResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .reason(effectiveRequest.getReason().name())
                .reasonText(effectiveRequest.getReasonText())
                .partialCancellation(false)
                .cancelledSeatIds(booking.getBookingSeats().stream().map(bs -> bs.getSeat().getId()).toList())
                .totalCancelledSeats(seatsCount)
                .originalFare(originalFare)
                .cancellationCharge(cancellationCharge)
                .refundAmount(refundAmount)
                .netPayableAfterCancellation(0.0)
                .refund(refund)
                .ticketStatus("CANCELLED")
                .build();
    }

    @Override
    @Transactional
    public CancellationResponse partialCancelBooking(PartialCancellationRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));
        ensureOwnership(booking);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Only confirmed bookings can be partially cancelled");
        }

        // Validate seats belong to this booking
        List<BookingSeat> seatsToCancel = booking.getBookingSeats().stream()
                .filter(bs -> request.getSeatIds().contains(bs.getSeat().getId()))
                .filter(bs -> !Boolean.TRUE.equals(bs.getIsCancelled()))
                .toList();

        if (seatsToCancel.size() != request.getSeatIds().size()) {
            throw new BookingException("Some seats are not available for cancellation or don't belong to this booking");
        }

        if (seatsToCancel.size() == booking.getBookingSeats().size()) {
            throw new BookingException("Cannot cancel all seats. Use full cancellation instead");
        }

        // Calculate cancellation charge per seat
        double originalFare = booking.getTotalFare();
        double farePerSeat = originalFare / booking.getBookingSeats().size();
        double cancellationChargePerSeat = calculateCancellationCharge(booking) / booking.getBookingSeats().size();

        double totalCancellationCharge = 0.0;
        double totalRefundAmount = 0.0;

        // Cancel selected seats
        for (BookingSeat bs : seatsToCancel) {
            bs.setIsCancelled(true);
            bs.setCancellationCharge(cancellationChargePerSeat);
            double seatRefund = Math.max(farePerSeat - cancellationChargePerSeat, 0.0);
            bs.setRefundAmount(seatRefund);
            totalCancellationCharge += cancellationChargePerSeat;
            totalRefundAmount += seatRefund;
        }

        // Update booking
        double newTotalFare = originalFare - totalRefundAmount - totalCancellationCharge;
        booking.setTotalFare(Math.max(newTotalFare, 0.0));
        booking.setCancellationReason(request.getReason());
        booking.setCancellationReasonText(request.getReasonText());
        booking.setTotalRefundAmount((booking.getTotalRefundAmount() != null ? booking.getTotalRefundAmount() : 0.0) + totalRefundAmount);

        String cancelledSeatIdsStr = seatsToCancel.stream()
                .map(bs -> bs.getSeat().getId().toString())
                .collect(Collectors.joining(","));
        booking.setCancelledSeatIds(cancelledSeatIdsStr);

        bookingRepository.save(booking);

        // Release seats back to inventory
        BusSchedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() + seatsToCancel.size());
        scheduleRepository.save(schedule);

        // Initiate refund
        RefundResponse refund = null;
        if (totalRefundAmount > 0) {
            refund = refundService.initiateRefund(booking.getId(), totalRefundAmount, request.getReason().name());
            refund = refundService.processRefund(refund.getId());
            refund = refundService.approveRefund(refund.getId());
            refund = refundService.completeRefund(refund.getId());
        }

        addTimelineEntry(booking, BookingEvent.BOOKING_MODIFIED,
                "Partial cancellation: " + seatsToCancel.size() + " seats cancelled. Refund: " + totalRefundAmount);

        return CancellationResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .reason(request.getReason().name())
                .reasonText(request.getReasonText())
                .partialCancellation(true)
                .cancelledSeatIds(request.getSeatIds())
                .totalCancelledSeats(seatsToCancel.size())
                .originalFare(originalFare)
                .cancellationCharge(totalCancellationCharge)
                .refundAmount(totalRefundAmount)
                .netPayableAfterCancellation(booking.getTotalFare())
                .refund(refund)
                .ticketStatus(booking.getTicketStatus())
                .build();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PHASE 7 â€“ TICKET ENHANCEMENT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public TicketResponse revalidateTicket(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);

        if (booking.getTicketNumber() == null) {
            throw new BookingException("No ticket generated for this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Cannot revalidate a cancelled ticket");
        }

        // Revalidate: regenerate QR code string
        booking.setQrCodeString(generateQrCodeString(booking));
        booking.setTicketStatus("ACTIVE");
        bookingRepository.save(booking);

        addTimelineEntry(booking, BookingEvent.TICKET_REPRINTED, "Ticket revalidated");

        return bookingMapper.toTicketResponse(booking);
    }

    @Transactional
    public TicketResponse regenerateTicket(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        ensureOwnership(booking);

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BookingException("Can only regenerate tickets for confirmed or completed bookings");
        }

        // Generate new ticket number
        String newTicketNumber = generateTicketNumber();
        booking.setTicketNumber(newTicketNumber);
        booking.setQrCodeString(generateQrCodeString(booking));
        booking.setTicketStatus("ACTIVE");
        bookingRepository.save(booking);

        addTimelineEntry(booking, BookingEvent.TICKET_GENERATED,
                "Ticket regenerated: " + newTicketNumber);

        return bookingMapper.toTicketResponse(booking);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PRIVATE HELPERS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void validateScheduleForBooking(BusSchedule schedule) {
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BookingException("Cannot book a cancelled schedule");
        }
        if (schedule.getBus().getStatus() != BusStatus.ACTIVE) {
            throw new BookingException("Bus is not active");
        }
        if (schedule.getRoute().getStatus() != RouteStatus.ACTIVE) {
            throw new BookingException("Route is not active");
        }
    }

    private void ensureOwnership(Booking booking) {
        if (securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN")) {
            return;
        }
        if (!booking.getUserId().equals(securityUtil.getCurrentUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to access this booking");
        }
    }

    private void addTimelineEntry(Booking booking, BookingEvent event, String description) {
        BookingTimeline timeline = BookingTimeline.builder()
                .booking(booking)
                .event(event)
                .description(description)
                .build();
        timelineRepository.save(timeline);
    }

    private String generateBookingReference() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(9000) + 1000;
        return "BK" + timestamp + random;
    }

    private String generateTicketNumber() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(90000) + 10000;
        return "TK" + timestamp + random;
    }

    private String generateQrCodeString(Booking booking) {
        return String.format("BUSBOOK|%s|%s|%d|%s|%.2f",
                booking.getBookingReference(),
                booking.getTicketNumber(),
                booking.getSchedule().getId(),
                booking.getSchedule().getTravelDate(),
                booking.getTotalFare());
    }

    private Specification<Booking> buildSearchSpecification(java.util.UUID userId, BookingSearchRequest criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (criteria.getBookingReference() != null && !criteria.getBookingReference().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("bookingReference")),
                        "%" + criteria.getBookingReference().toLowerCase() + "%"));
            }
            if (criteria.getSource() != null && !criteria.getSource().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("schedule").get("route").get("source")),
                        "%" + criteria.getSource().toLowerCase() + "%"));
            }
            if (criteria.getDestination() != null && !criteria.getDestination().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("schedule").get("route").get("destination")),
                        "%" + criteria.getDestination().toLowerCase() + "%"));
            }
            if (criteria.getTravelDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("schedule").get("travelDate"), criteria.getTravelDateFrom()));
            }
            if (criteria.getTravelDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("schedule").get("travelDate"), criteria.getTravelDateTo()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildBookingSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return Sort.by(Sort.Direction.DESC, "bookedAt");
        return switch (sortBy.toUpperCase()) {
            case "DATE_ASC" -> Sort.by(Sort.Direction.ASC, "bookedAt");
            case "DATE_DESC" -> Sort.by(Sort.Direction.DESC, "bookedAt");
            case "FARE_ASC" -> Sort.by(Sort.Direction.ASC, "totalFare");
            case "FARE_DESC" -> Sort.by(Sort.Direction.DESC, "totalFare");
            case "REFERENCE_ASC" -> Sort.by(Sort.Direction.ASC, "bookingReference");
            default -> Sort.by(Sort.Direction.DESC, "bookedAt");
        };
    }

    private double calculateCancellationCharge(Booking booking) {
        Long routeId = booking.getSchedule().getRoute().getId();
        BusType busType = booking.getSchedule().getBus().getBusType();

        List<CancellationPolicy> policies = cancellationPolicyRepository.findApplicablePolicies(routeId, busType);

        if (policies.isEmpty()) {
            // Default: 10% cancellation charge
            return booking.getTotalFare() * 0.10;
        }

        CancellationPolicy policy = policies.get(0); // Most specific policy
        return booking.getTotalFare() * policy.getCancellationChargePercent() / 100.0;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // TRAVELER TRIP INTEGRATION - attach/detach/list only
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public TripBusBookingResponse attachBookingToTrip(UUID tripId, AttachBusBookingRequest request) {
        Trip trip = getTrip(tripId);
        requireTripAccess(trip);
        tripAuthorizationService.requireMutableTrip(trip);

        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.bookingId()));
        // Trip membership gives Trip access; booking ownership gives booking
        // control. An organizer must not attach a fellow accepted member's
        // booking merely because the trip is shared - both checks are required.
        ensureOwnership(booking);

        if (booking.getTravelerTripId() != null && booking.getTravelerTripId().equals(tripId)) {
            // Already attached to this exact trip - idempotent, not an error.
            return toTripBusBookingResponse(booking);
        }
        if (booking.getTravelerTripId() != null) {
            throw new BookingException("Booking is already attached to a different trip");
        }

        ensureAttachableStatus(booking);

        booking.setTravelerTripId(tripId);
        bookingRepository.save(booking);
        return toTripBusBookingResponse(booking);
    }

    @Override
    @Transactional
    public void removeBookingFromTrip(UUID tripId, Long bookingId) {
        Trip trip = getTrip(tripId);
        requireTripAccess(trip);
        tripAuthorizationService.requireMutableTrip(trip);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        // Deliberately stricter than the Hotel Booking precedent: only the
        // booking owner (or admin) may detach it. The trip organizer does NOT
        // gain detach rights over a fellow accepted member's booking merely by
        // being organizer - organizer status is a Trip-level authority, not a
        // Booking-level one.
        ensureOwnership(booking);

        if (!tripId.equals(booking.getTravelerTripId())) {
            throw new BookingException("Booking is not attached to trip " + tripId);
        }

        booking.setTravelerTripId(null);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public TripBusBookingSummaryResponse getTripBusBookings(UUID tripId) {
        Trip trip = getTrip(tripId);
        requireTripAccess(trip);

        List<TripBusBookingResponse> bookings = bookingRepository.findByTravelerTripId(tripId).stream()
                .map(this::toTripBusBookingResponse)
                .toList();
        double totalFare = bookings.stream().mapToDouble(TripBusBookingResponse::totalFare).sum();
        return new TripBusBookingSummaryResponse(tripId, bookings.size(), totalFare, bookings);
    }

    private Trip getTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
    }

    private void requireTripAccess(Trip trip) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        boolean isAdmin = securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN");
        tripAuthorizationService.requireMember(trip, currentUserId, isAdmin);
    }

    private void ensureAttachableStatus(Booking booking) {
        if (!TRIP_ATTACHABLE_STATUSES.contains(booking.getStatus())) {
            throw new BookingException(
                    "Booking status " + booking.getStatus() + " cannot be attached to a trip; "
                            + "only CONFIRMED or COMPLETED bookings are eligible");
        }
    }

    private TripBusBookingResponse toTripBusBookingResponse(Booking booking) {
        return new TripBusBookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getTotalFare(),
                booking.getSchedule().getId(),
                booking.getSchedule().getTravelDate(),
                booking.getSchedule().getRoute().getSource(),
                booking.getSchedule().getRoute().getDestination(),
                booking.getUserId(),
                booking.getTravelerTripId()
        );
    }
}

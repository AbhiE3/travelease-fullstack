package com.travelease.backend.busbooking.entity;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.entity.enums.CancellationReason;
import com.travelease.backend.busbooking.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    /**
     * Optional association to the Traveler planning Trip (trip.entity.Trip, a
     * different domain from this module's own operational Trip/BusSchedule
     * lifecycle) that this booking has been attached to for shared trip planning.
     * Named travelerTripId (not tripId) to avoid ambiguity with this module's
     * operational Trip concept. Bare UUID, no FK/relationship - mirrors
     * HotelBooking.tripId's cross-module association shape rather than importing
     * the Traveler Trip entity into this domain.
     */
    @Column(name = "traveler_trip_id")
    private java.util.UUID travelerTripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private BusSchedule schedule;

    @Column(name = "booking_reference", unique = true, nullable = false)
    private String bookingReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_fare", nullable = false)
    private Double totalFare;

    // â”€â”€ Ticket â”€â”€
    @Column(name = "ticket_number", unique = true)
    private String ticketNumber;

    @Column(name = "qr_code_string")
    private String qrCodeString;

    // â”€â”€ Payment (simulated) â”€â”€
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // â”€â”€ Contact â”€â”€
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    // â”€â”€ Coupon â”€â”€
    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "coupon_discount")
    @Builder.Default
    private Double couponDiscount = 0.0;

    // â”€â”€ Timestamps â”€â”€
    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // â”€â”€ Phase 7 â€“ Cancellation â”€â”€
    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason")
    private CancellationReason cancellationReason;

    @Column(name = "cancellation_reason_text")
    private String cancellationReasonText;

    @Column(name = "cancelled_seat_ids")
    private String cancelledSeatIds; // comma-separated seat IDs for partial cancellation

    @Column(name = "total_refund_amount")
    private Double totalRefundAmount;

    // â”€â”€ Phase 7 â€“ Ticket Status â”€â”€
    @Column(name = "ticket_status")
    private String ticketStatus; // ACTIVE, CANCELLED, EXPIRED, JOURNEY_COMPLETED

    // â”€â”€ Relationships â”€â”€
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingSeat> bookingSeats = new ArrayList<>();
}

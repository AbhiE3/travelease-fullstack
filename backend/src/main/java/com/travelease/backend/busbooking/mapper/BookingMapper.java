package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.BookingHistoryResponse;
import com.travelease.backend.busbooking.dto.response.BookingResponse;
import com.travelease.backend.busbooking.dto.response.BookingSeatResponse;
import com.travelease.backend.busbooking.dto.response.BookingTimelineResponse;
import com.travelease.backend.busbooking.dto.response.TicketResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.BookingTimeline;
import com.travelease.backend.busbooking.entity.BusSchedule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    private final ScheduleMapper scheduleMapper;

    public BookingMapper(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    public BookingResponse toResponse(Booking booking) {
        List<BookingSeatResponse> seatResponses = booking.getBookingSeats()
                .stream()
                .map(bs -> BookingSeatResponse.builder()
                        .id(bs.getId())
                        .seatNumber(bs.getSeat().getSeatNumber())
                        .seatType(bs.getSeat().getSeatType())
                        .deck(bs.getSeat().getDeck())
                        .passengerName(bs.getPassengerName())
                        .passengerAge(bs.getPassengerAge())
                        .passengerGender(bs.getPassengerGender())
                        .passengerEmail(bs.getPassengerEmail())
                        .passengerPhone(bs.getPassengerPhone())
                        .isPrimary(bs.getIsPrimary())
                        .build())
                .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .totalFare(booking.getTotalFare())
                .ticketNumber(booking.getTicketNumber())
                .qrCodeString(booking.getQrCodeString())
                .paymentStatus(booking.getPaymentStatus())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .couponCode(booking.getCouponCode())
                .couponDiscount(booking.getCouponDiscount())
                .bookedAt(booking.getBookedAt())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .completedAt(booking.getCompletedAt())
                .expiresAt(booking.getExpiresAt())
                .scheduleId(booking.getSchedule().getId())
                .routeId(booking.getSchedule().getRoute().getId())
                .providerId(booking.getSchedule().getBus().getProviderId())
                .busId(booking.getSchedule().getBus().getId())
                .userId(booking.getUserId())
                .schedule(scheduleMapper.toResponse(booking.getSchedule()))
                .seats(seatResponses)
                .build();
    }

    public BookingResponse toResponseWithCancellation(Booking booking) {
        BookingResponse response = toResponse(booking);
        // Add cancellation-specific fields if needed
        return response;
    }

    public BookingHistoryResponse toHistoryResponse(Booking booking) {
        BusSchedule schedule = booking.getSchedule();
        return BookingHistoryResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .source(schedule.getRoute().getSource())
                .destination(schedule.getRoute().getDestination())
                .travelDate(schedule.getTravelDate())
                .departureTime(schedule.getDepartureTime())
                .totalFare(booking.getTotalFare())
                .status(booking.getStatus())
                .busName(schedule.getBus().getBusName())
                .seatsBooked(booking.getBookingSeats().size())
                .bookedAt(booking.getBookedAt())
                .ticketNumber(booking.getTicketNumber())
                .paymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null)
                .build();
    }

    public BookingTimelineResponse toTimelineResponse(BookingTimeline timeline) {
        return BookingTimelineResponse.builder()
                .id(timeline.getId())
                .event(timeline.getEvent())
                .description(timeline.getDescription())
                .occurredAt(timeline.getOccurredAt())
                .metadata(timeline.getMetadata())
                .build();
    }

    public TicketResponse toTicketResponse(Booking booking) {
        String primaryPassenger = booking.getBookingSeats().stream()
                .filter(bs -> Boolean.TRUE.equals(bs.getIsPrimary()))
                .findFirst()
                .map(BookingSeat -> BookingSeat.getPassengerName())
                .orElse(booking.getBookingSeats().isEmpty() ? null : booking.getBookingSeats().get(0).getPassengerName());

        return TicketResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .ticketNumber(booking.getTicketNumber())
                .qrCodeString(booking.getQrCodeString())
                .status(booking.getStatus())
                .scheduleId(booking.getSchedule().getId())
                .routeId(booking.getSchedule().getRoute().getId())
                .providerId(booking.getSchedule().getBus().getProviderId())
                .busId(booking.getSchedule().getBus().getId())
                .busName(booking.getSchedule().getBus().getBusName())
                .busNumber(booking.getSchedule().getBus().getBusNumber())
                .source(booking.getSchedule().getRoute().getSource())
                .destination(booking.getSchedule().getRoute().getDestination())
                .travelDate(booking.getSchedule().getTravelDate())
                .primaryPassengerName(primaryPassenger)
                .totalPassengers(booking.getBookingSeats().size())
                .totalFare(booking.getTotalFare())
                .bookedAt(booking.getBookedAt())
                .confirmedAt(booking.getConfirmedAt())
                .build();
    }
}

package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.RefundStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.RefundResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.BookingTimeline;
import com.travelease.backend.busbooking.entity.Refund;
import com.travelease.backend.busbooking.entity.enums.BookingEvent;
import com.travelease.backend.busbooking.entity.enums.PaymentStatus;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import com.travelease.backend.busbooking.exception.BookingException;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.RefundMapper;
import com.travelease.backend.busbooking.repository.BookingRepository;
import com.travelease.backend.busbooking.repository.BookingTimelineRepository;
import com.travelease.backend.busbooking.repository.RefundRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.RefundService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final BookingTimelineRepository timelineRepository;
    private final RefundMapper refundMapper;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public RefundResponse initiateRefund(Long bookingId, Double amount, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (amount <= 0) {
            throw new BookingException("Refund amount must be positive");
        }

        Refund refund = Refund.builder()
                .booking(booking)
                .refundReference(generateRefundReference())
                .originalAmount(amount)
                .cancellationCharge(0.0)
                .gstAdjustment(0.0)
                .couponAdjustment(0.0)
                .netRefundable(amount)
                .status(RefundStatus.INITIATED)
                .reason(reason)
                .build();

        Refund saved = refundRepository.save(refund);
        addTimelineEntry(booking, BookingEvent.REFUND_INITIATED,
                "Refund initiated: " + saved.getRefundReference() + ", amount: " + amount);

        log.info("Refund initiated for booking {}: {}", bookingId, saved.getRefundReference());
        return refundMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RefundResponse processRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));

        if (refund.getStatus() != RefundStatus.INITIATED) {
            throw new BookingException("Cannot process refund in status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.PROCESSING);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        addTimelineEntry(refund.getBooking(), BookingEvent.REFUND_PROCESSING,
                "Refund " + refund.getRefundReference() + " is being processed");

        log.info("Refund {} is being processed", refund.getRefundReference());
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse approveRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));

        if (refund.getStatus() != RefundStatus.PROCESSING) {
            throw new BookingException("Cannot approve refund in status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.APPROVED);
        refund.setApprovedAt(LocalDateTime.now());
        refundRepository.save(refund);

        addTimelineEntry(refund.getBooking(), BookingEvent.REFUND_APPROVED,
                "Refund " + refund.getRefundReference() + " approved");

        log.info("Refund {} approved", refund.getRefundReference());
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse completeRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));

        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new BookingException("Cannot complete refund in status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refundRepository.save(refund);

        Booking booking = refund.getBooking();
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        booking.setTotalRefundAmount(refund.getNetRefundable());
        bookingRepository.save(booking);

        addTimelineEntry(booking, BookingEvent.REFUND_COMPLETED,
                "Refund " + refund.getRefundReference() + " completed, amount: " + refund.getNetRefundable());

        log.info("Refund {} completed", refund.getRefundReference());
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse rejectRefund(Long refundId, String rejectionReason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));

        if (refund.getStatus() == RefundStatus.COMPLETED || refund.getStatus() == RefundStatus.REJECTED) {
            throw new BookingException("Cannot reject refund in status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectionReason(rejectionReason);
        refund.setRejectedAt(LocalDateTime.now());
        refundRepository.save(refund);

        addTimelineEntry(refund.getBooking(), BookingEvent.REFUND_REJECTED,
                "Refund " + refund.getRefundReference() + " rejected: " + rejectionReason);

        log.info("Refund {} rejected", refund.getRefundReference());
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse failRefund(Long refundId, String failureReason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));

        if (refund.getStatus() == RefundStatus.COMPLETED || refund.getStatus() == RefundStatus.FAILED) {
            throw new BookingException("Cannot fail refund in status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.FAILED);
        refund.setRejectionReason(failureReason);
        refund.setFailedAt(LocalDateTime.now());
        refundRepository.save(refund);

        addTimelineEntry(refund.getBooking(), BookingEvent.REFUND_FAILED,
                "Refund " + refund.getRefundReference() + " failed: " + failureReason);

        log.warn("Refund {} failed: {}", refund.getRefundReference(), failureReason);
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse transitionRefund(Long id, RefundStatusTransitionRequest request) {
        return switch (request.getStatus()) {
            case INITIATED -> throw new BookingException("Refund cannot be transitioned back to INITIATED");
            case PROCESSING -> processRefund(id);
            case APPROVED -> approveRefund(id);
            case COMPLETED -> completeRefund(id);
            case REJECTED -> rejectRefund(id, request.getReason());
            case FAILED -> failRefund(id, request.getReason());
        };
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundById(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefunds(String reference, Long bookingId, RefundStatus status, Pageable pageable) {
        Specification<Refund> spec = buildRefundSpecification(reference, bookingId, status);
        java.util.UUID currentUserId = securityUtil.getCurrentUserId();
        boolean isAdmin = securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN");
        if (!isAdmin) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("booking").get("userId"), currentUserId));
        }

        return refundRepository.findAll(spec, pageable).stream()
                .map(refundMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundByReference(String refundReference) {
        Refund refund = refundRepository.findByRefundReference(refundReference)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "reference", refundReference));
        return refundMapper.toResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByBookingId(Long bookingId) {
        return refundRepository.findByBookingIdOrderByInitiatedAtDesc(bookingId)
                .stream().map(refundMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getUserRefunds() {
        java.util.UUID userId = securityUtil.getCurrentUserId();
        return refundRepository.findByBookingUserIdOrderByInitiatedAtDesc(userId)
                .stream().map(refundMapper::toResponse).toList();
    }

    private Specification<Refund> buildRefundSpecification(String reference, Long bookingId, RefundStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (reference != null && !reference.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("refundReference")), "%" + reference.toLowerCase() + "%"));
            }
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), bookingId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addTimelineEntry(Booking booking, BookingEvent event, String description) {
        BookingTimeline timeline = BookingTimeline.builder()
                .booking(booking)
                .event(event)
                .description(description)
                .build();
        timelineRepository.save(timeline);
    }

    private String generateRefundReference() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(9000) + 1000;
        return "RF" + timestamp + random;
    }
}

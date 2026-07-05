package com.travelease.backend.trip.entity;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "trip_member_id", nullable = false, updatable = false))
@Table(name = "trip_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_members_trip_user", columnNames = {"trip_id", "user_id"})
})
public class TripMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false, length = 20)
    private TripMemberStatus memberStatus = TripMemberStatus.INVITED;

    @Column(name = "joined_date")
    private LocalDateTime joinedDate;

    @Column(name = "budget_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(name = "spent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;
}

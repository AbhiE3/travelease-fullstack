package com.travelease.backend.settlement.entity;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.entity.BaseEntity;
import com.travelease.backend.trip.entity.Trip;
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

@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "settlement_id", nullable = false, updatable = false))
@Table(name = "settlements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlements_trip_payer_receiver", columnNames = {"trip_id", "payer_user_id", "receiver_user_id"})
})
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;
}

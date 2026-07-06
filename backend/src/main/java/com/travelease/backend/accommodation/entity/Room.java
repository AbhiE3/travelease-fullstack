package com.travelease.backend.accommodation.entity;

import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "room_id", nullable = false, updatable = false))
@Table(name = "rooms")
public class Room extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 80)
    private String roomType;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, length = 80)
    private String bedType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(nullable = false, length = 40)
    private String availabilityStatus = "AVAILABLE";
}

package com.travelease.backend.accommodation.entity;

import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "hotel_id", nullable = false, updatable = false))
@Table(name = "hotels")
public class Hotel extends BaseEntity {

    /**
     * The Hotel Provider tenant that owns this hotel. Assigned server-side from
     * the authenticated ROLE_HOTEL_PROVIDER account (or explicitly by ROLE_ADMIN)
     * at creation time; never accepted as client-authoritative on update.
     */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "destination_id", nullable = false)
    private Integer destinationId;

    @Column(name = "hotel_name", nullable = false, length = 200)
    private String hotelName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String policies;
}

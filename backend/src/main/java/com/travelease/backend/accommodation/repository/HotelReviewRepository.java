package com.travelease.backend.accommodation.repository;

import com.travelease.backend.accommodation.entity.HotelReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelReviewRepository extends JpaRepository<HotelReview, UUID> {

    List<HotelReview> findByHotelId(UUID hotelId);
}

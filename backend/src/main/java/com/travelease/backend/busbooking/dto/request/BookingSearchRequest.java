package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSearchRequest {

    private String bookingReference;
    private String source;
    private String destination;
    private LocalDate travelDateFrom;
    private LocalDate travelDateTo;
    private BookingStatus status;
    private String sortBy;       // DATE_ASC, DATE_DESC, FARE_ASC, FARE_DESC, REFERENCE_ASC
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
}

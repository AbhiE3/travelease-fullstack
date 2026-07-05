package com.travelease.backend.busbooking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDetailDto {

    private Long seatId;
    private String passengerName;
    private Integer passengerAge;
    private String passengerGender;
    private String passengerEmail;
    private String passengerPhone;
    private Boolean isPrimary;
}

package com.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private Long offeringId;
    private Long teacherId;
    private String startTime;
    private String endTime;
    private String timezone;
}
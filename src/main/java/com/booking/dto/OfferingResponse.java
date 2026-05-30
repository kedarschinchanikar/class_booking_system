package com.booking.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OfferingResponse {

    private Long id;
    private String courseName;
    private String offeringName;
    private String teacherName;
    private String teacherTimezone;
    private int maxCapacity;
    private int currentEnrollment;
    private int availableSlots;
    private List<SessionResponse> sessions;
}
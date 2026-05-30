package com.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddSessionRequest {

    @NotNull(message = "Offering ID is required")
    private Long offeringId;

    @NotNull(message = "Start time is required (ISO-8601 format, e.g. 2025-06-07T17:00:00)")
    private String startTime;

    @NotNull(message = "End time is required (ISO-8601 format, e.g. 2025-06-07T18:00:00)")
    private String endTime;

    /** Teacher's timezone (e.g. America/New_York). If null, times are treated as UTC. */
    private String timezone;
}
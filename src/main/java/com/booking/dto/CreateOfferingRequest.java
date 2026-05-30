package com.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOfferingRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotBlank(message = "Offering name is required")
    private String name;

    @Min(value = 1, message = "Max capacity must be at least 1")
    private int maxCapacity = 30;
}
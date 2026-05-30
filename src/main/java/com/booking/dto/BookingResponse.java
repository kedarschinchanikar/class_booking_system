package com.booking.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BookingResponse {

    private Long bookingId;
    private Long parentId;
    private String parentName;
    private String bookedAt;
    private OfferingResponse offering;
}
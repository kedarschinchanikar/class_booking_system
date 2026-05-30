package com.booking.controller;

import com.booking.dto.*;
import com.booking.service.BookingService;
import com.booking.service.OfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
public class ParentController {

    private final OfferingService offeringService;
    private final BookingService bookingService;

    /**
     * Get all available offerings (with remaining capacity).
     * Session times are converted to the viewer's timezone.
     * GET /api/parent/offerings?timezone=America/Los_Angeles
     */
    @GetMapping("/offerings")
    public ResponseEntity<List<OfferingResponse>> getAvailableOfferings(
            @RequestParam(required = false, defaultValue = "UTC") String timezone) {

        List<OfferingResponse> offerings =
                offeringService.getAvailableOfferings(timezone);

        return ResponseEntity.ok(offerings);
    }
    /**
     * Book an offering for a parent.
     * Handles concurrency, capacity checks, and time-conflict detection.
     * POST /api/parent/bookings
     */
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> bookOffering(
            @Valid @RequestBody BookOfferingRequest request) {

        BookingResponse response = bookingService.bookOffering(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all bookings for a parent.
     * Session times are displayed in the parent's timezone.
     * GET /api/parent/{parentId}/bookings
     */
    @GetMapping("/{parentId}/bookings")
    public ResponseEntity<List<BookingResponse>> getParentBookings(
            @PathVariable Long parentId) {

        List<BookingResponse> bookings =
                bookingService.getParentBookings(parentId);

        return ResponseEntity.ok(bookings);
    }
}
package com.booking.controller;

import com.booking.dto.*;
import com.booking.service.OfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final OfferingService offeringService;

    /**
     * Create a new offering for a course.
     * POST /api/teacher/offerings
     */
    @PostMapping("/offerings")
    public ResponseEntity<OfferingResponse> createOffering(
            @Valid @RequestBody CreateOfferingRequest request) {

        OfferingResponse response =
                offeringService.createOffering(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Add a session to an existing offering.
     * Teacher specifies times in their local timezone.
     * POST /api/teacher/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> addSession(
            @Valid @RequestBody AddSessionRequest request) {

        SessionResponse response =
                offeringService.addSession(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get all offerings for a teacher with their sessions.
     * Times are displayed in the teacher's timezone.
     * GET /api/teacher/{teacherId}/offerings
     */
    @GetMapping("/{teacherId}/offerings")
    public ResponseEntity<List<OfferingResponse>> getTeacherOfferings(
            @PathVariable Long teacherId) {

        List<OfferingResponse> offerings =
                offeringService.getTeacherOfferings(teacherId);

        return ResponseEntity.ok(offerings);
    }
}

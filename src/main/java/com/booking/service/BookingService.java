package com.booking.service;

import com.booking.dto.*;
import com.booking.entity.*;
import com.booking.entity.Session;
import com.booking.exception.*;
import com.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OfferingRepository offeringRepository;
    private final ParentRepository parentRepository;
    private final SessionRepository sessionRepository;

    /**
     * Books an offering for a parent with full concurrency safety.
     *
     * Concurrency is handled via:
     * 1. PESSIMISTIC_WRITE lock on the offering row - serializes concurrent bookings for the same offering
     * 2. Unique constraint (parent_id, offering_id) - prevents duplicate bookings at DB level
     * 3. Time-conflict check within the same transaction - prevents overlapping session bookings
     */
    @Transactional
    public BookingResponse bookOffering(BookOfferingRequest request) {
        Parent parent = parentRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found with id: " + request.getParentId()));

        // Acquire pessimistic lock on the offering row to serialize concurrent bookings
        Offering offering = offeringRepository.findByIdWithLock(request.getOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + request.getOfferingId()));

        // Check if already booked
        if (bookingRepository.existsByParentIdAndOfferingId(parent.getId(), offering.getId())) {
            throw new ConflictException("Parent has already booked this offering");
        }

        // Check capacity
        if (offering.getCurrentEnrollment() >= offering.getMaxCapacity()) {
            throw new ConflictException("Offering is fully booked (capacity: " + offering.getMaxCapacity() + ")");
        }

        // Check for time conflicts with parent's existing bookings
        List<Session> newSessions = sessionRepository.findByOfferingIdOrderByStartTime(offering.getId());
        if (newSessions.isEmpty()) {
            throw new InvalidRequestException("Cannot book an offering with no sessions");
        }

        for (Session session : newSessions) {
            List<Session> conflicts = sessionRepository.findConflictingSessionsForParent(
                    parent.getId(), session.getStartTime(), session.getEndTime());
            if (!conflicts.isEmpty()) {
                Session conflicting = conflicts.get(0);
                throw new TimeConflictException(
                        String.format("Time conflict detected: session %s (%s - %s) overlaps with already booked session %s (%s - %s)",
                                session.getId(), session.getStartTime(), session.getEndTime(),
                                conflicting.getId(), conflicting.getStartTime(), conflicting.getEndTime()));
            }
        }

        // Increment enrollment
        offering.setCurrentEnrollment(offering.getCurrentEnrollment() + 1);
        offeringRepository.save(offering);

        // Create booking
        Booking booking = Booking.builder()
                .parent(parent)
                .offering(offering)
                .build();

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Duplicate booking detected - parent has already booked this offering");
        }

        return toBookingResponse(booking, parent, offering, parent.getTimezone());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getParentBookings(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found with id: " + parentId));

        List<Booking> bookings = bookingRepository.findByParentIdWithDetails(parentId);
        return bookings.stream()
                .map(b -> {
                    List<Session> sessions = sessionRepository.findByOfferingIdOrderByStartTime(b.getOffering().getId());
                    return toBookingResponseWithSessions(b, parent, b.getOffering(), sessions, parent.getTimezone());
                })
                .collect(Collectors.toList());
    }

    private BookingResponse toBookingResponse(Booking booking, Parent parent, Offering offering, String timezone) {
        List<Session> sessions = sessionRepository.findByOfferingIdOrderByStartTime(offering.getId());
        return toBookingResponseWithSessions(booking, parent, offering, sessions, timezone);
    }

    private BookingResponse toBookingResponseWithSessions(Booking booking, Parent parent, Offering offering,
                                                          List<Session> sessions, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<SessionResponse> sessionResponses = sessions.stream()
                .map(s -> SessionResponse.builder()
                        .id(s.getId())
                        .offeringId(offering.getId())
                        .teacherId(offering.getTeacher().getId())
                        .startTime(s.getStartTime().atZone(zoneId).format(formatter))
                        .endTime(s.getEndTime().atZone(zoneId).format(formatter))
                        .timezone(timezone)
                        .build())
                .collect(Collectors.toList());

        OfferingResponse offeringResponse = OfferingResponse.builder()
                .id(offering.getId())
                .courseName(offering.getCourse().getName())
                .offeringName(offering.getName())
                .teacherName(offering.getTeacher().getName())
                .teacherTimezone(offering.getTeacher().getTimezone())
                .maxCapacity(offering.getMaxCapacity())
                .currentEnrollment(offering.getCurrentEnrollment())
                .availableSlots(offering.getMaxCapacity() - offering.getCurrentEnrollment())
                .sessions(sessionResponses)
                .build();

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .parentId(parent.getId())
                .parentName(parent.getName())
                .bookedAt(booking.getBookedAt() != null
                        ? booking.getBookedAt().atZone(zoneId).format(formatter)
                        : Instant.now().atZone(zoneId).format(formatter))
                .offering(offeringResponse)
                .build();
    }
}
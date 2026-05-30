package com.booking.service;

import com.booking.dto.*;
import com.booking.entity.*;
import com.booking.entity.Session;
import com.booking.exception.*;
import com.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferingService {

    private final OfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public OfferingResponse createOffering(CreateOfferingRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

        Offering offering = Offering.builder()
                .course(course)
                .teacher(teacher)
                .name(request.getName())
                .maxCapacity(request.getMaxCapacity())
                .currentEnrollment(0)
                .build();

        offering = offeringRepository.save(offering);
        return toResponse(offering, teacher.getTimezone());
    }

    @Transactional
    public SessionResponse addSession(AddSessionRequest request) {
        Offering offering = offeringRepository.findById(request.getOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + request.getOfferingId()));

        Teacher teacher = offering.getTeacher();
        String tz = request.getTimezone() != null ? request.getTimezone() : teacher.getTimezone();

        Instant startUtc = parseToUtc(request.getStartTime(), tz);
        Instant endUtc = parseToUtc(request.getEndTime(), tz);

        if (!endUtc.isAfter(startUtc)) {
            throw new InvalidRequestException("End time must be after start time");
        }

        Session session = Session.builder()
                .offering(offering)
                .startTime(startUtc)
                .endTime(endUtc)
                .build();

        session = sessionRepository.save(session);
        return toSessionResponse(session, teacher.getId(), tz);
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getTeacherOfferings(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        List<Offering> offerings = offeringRepository.findByTeacherId(teacherId);
        return offerings.stream()
                .map(o -> {
                    List<Session> sessions = sessionRepository.findByOfferingIdOrderByStartTime(o.getId());
                    return toResponseWithSessions(o, sessions, teacher.getTimezone());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getAvailableOfferings(String viewerTimezone) {
        String tz = viewerTimezone != null ? viewerTimezone : "UTC";
        validateTimezone(tz);

        List<Offering> offerings = offeringRepository.findAvailableOfferings();
        return offerings.stream()
                .map(o -> toResponseWithSessions(o, o.getSessions(), tz))
                .collect(Collectors.toList());
    }

    private OfferingResponse toResponse(Offering offering, String timezone) {
        List<Session> sessions = sessionRepository.findByOfferingIdOrderByStartTime(offering.getId());
        return toResponseWithSessions(offering, sessions, timezone);
    }

    private OfferingResponse toResponseWithSessions(Offering offering, List<Session> sessions, String timezone) {
        List<SessionResponse> sessionResponses = sessions.stream()
                .map(s -> toSessionResponse(s, offering.getTeacher().getId(), timezone))
                .collect(Collectors.toList());

        return OfferingResponse.builder()
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
    }

    private SessionResponse toSessionResponse(Session session, Long teacherId, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        return SessionResponse.builder()
                .id(session.getId())
                .offeringId(session.getOffering().getId())
                .teacherId(teacherId)
                .startTime(session.getStartTime().atZone(zoneId).format(formatter))
                .endTime(session.getEndTime().atZone(zoneId).format(formatter))
                .timezone(timezone)
                .build();
    }

    private Instant parseToUtc(String dateTimeStr, String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr);
            return localDateTime.atZone(zoneId).toInstant();
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(
                    "Invalid date-time format: '" + dateTimeStr + "'. Expected ISO-8601 format like '2025-06-07T17:00:00'");
        }
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid timezone: " + timezone);
        }
    }
}
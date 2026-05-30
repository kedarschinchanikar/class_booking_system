package com.booking.repository;

import com.booking.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByOfferingId(Long offeringId);

    @Query("SELECT s FROM Session s WHERE s.offering.id = :offeringId ORDER BY s.startTime")
    List<Session> findByOfferingIdOrderByStartTime(@Param("offeringId") Long offeringId);

    /**
     * Find all sessions that belong to offerings already booked by a parent
     * and that overlap with the given time range [start, end).
     * Two sessions overlap if: existingStart < newEnd AND existingEnd > newStart
     */
    @Query("""
            SELECT s FROM Session s
            WHERE s.offering.id IN (
                SELECT b.offering.id FROM Booking b WHERE b.parent.id = :parentId
            )
            AND s.startTime < :endTime
            AND s.endTime > :startTime
            """)
    List<Session> findConflictingSessionsForParent(
            @Param("parentId") Long parentId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
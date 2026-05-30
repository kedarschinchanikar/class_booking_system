package com.booking.repository;

import com.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByParentIdAndOfferingId(Long parentId, Long offeringId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.offering o JOIN FETCH o.course JOIN FETCH o.teacher WHERE b.parent.id = :parentId")
    List<Booking> findByParentIdWithDetails(@Param("parentId") Long parentId);
}
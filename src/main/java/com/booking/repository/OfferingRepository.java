package com.booking.repository;

import com.booking.entity.Offering;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfferingRepository extends JpaRepository<Offering, Long> {

    @Query("SELECT o FROM Offering o JOIN FETCH o.course JOIN FETCH o.teacher WHERE o.teacher.id = :teacherId")
    List<Offering> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT o FROM Offering o JOIN FETCH o.course JOIN FETCH o.teacher LEFT JOIN FETCH o.sessions WHERE o.currentEnrollment < o.maxCapacity")
    List<Offering> findAvailableOfferings();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offering o WHERE o.id = :id")
    Optional<Offering> findByIdWithLock(@Param("id") Long id);
}
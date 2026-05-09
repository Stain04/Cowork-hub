package com.coworking.booking.repository;

import com.coworking.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    @Query("SELECT b.workspace.id FROM Booking b WHERE b.status != 'CANCELLED' " +
           "AND (:start < b.endTime AND :end > b.startTime)")
    List<Long> findBookedWorkspaceIdsInPeriod(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.workspace.id = :workspaceId " +
           "AND b.status != 'CANCELLED' " +
           "AND (:start < b.endTime AND :end > b.startTime)")
    boolean existsOverlappingBooking(@Param("workspaceId") Long workspaceId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}
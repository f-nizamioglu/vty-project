package com.vty.vty.repository;

import com.vty.vty.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    @Query(value = "SELECT * FROM reservation WHERE user_id = :userId ORDER BY reservation_date DESC", nativeQuery = true)
    List<Reservation> findByUserId(@Param("userId") Long userId);
    
    @Override
    @Query(value = "SELECT * FROM reservation ORDER BY reservation_date DESC", nativeQuery = true)
    List<Reservation> findAll();
    
    @Query(value = "SELECT * FROM reservation WHERE id = :id", nativeQuery = true)
    Optional<Reservation> findById(@Param("id") Long id);
    
    @Query(value = "SELECT * FROM reservation WHERE user_id = :userId AND reservation_date < :date AND review_id IS NULL", nativeQuery = true)
    List<Reservation> findPastReservationsWithoutReview(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query(value = "SELECT * FROM reservation WHERE tool_id = :toolId", nativeQuery = true)
    List<Reservation> findByToolId(@Param("toolId") Long toolId);
    
    @Query(value = "SELECT * FROM reservation WHERE tool_id = :toolId AND reservation_date = :date", nativeQuery = true)
    List<Reservation> findByToolIdAndDate(@Param("toolId") Long toolId, @Param("date") LocalDate date);
    
    @Query(value = "SELECT user_id, COUNT(*) as reservation_count " +
            "FROM reservation " +
            "GROUP BY user_id " +
            "HAVING COUNT(*) >= :minReservations " +
            "ORDER BY reservation_count DESC", nativeQuery = true)
    List<Object[]> findUsersWithMinReservations(@Param("minReservations") Long minReservations);
    
    @Query(value = "SELECT * FROM get_user_reservations(:userId, :startDate, :endDate)", 
           nativeQuery = true)
    List<Object[]> getUserReservationsWithDetails(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT * FROM get_pending_review_reservations(:userId, :daysPast)", 
           nativeQuery = true)
    List<Object[]> getPendingReviewReservations(
        @Param("userId") Long userId,
        @Param("daysPast") Integer daysPast);
}


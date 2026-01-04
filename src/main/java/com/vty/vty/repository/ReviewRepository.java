package com.vty.vty.repository;

import com.vty.vty.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    @Query(value = "SELECT * FROM review WHERE reservation_id = :reservationId", nativeQuery = true)
    Optional<Review> findByReservationId(@Param("reservationId") Long reservationId);
    
    @Override
    @Query(value = "SELECT * FROM review WHERE id = :id", nativeQuery = true)
    Optional<Review> findById(@Param("id") Long id);
}


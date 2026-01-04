package com.vty.vty.repository;

import com.vty.vty.entity.ToolPerformanceView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolPerformanceViewRepository extends JpaRepository<ToolPerformanceView, Long> {
    
    @Query(value = "SELECT * FROM tool_performance_view ORDER BY total_reservations DESC", nativeQuery = true)
    List<ToolPerformanceView> findAllOrderByReservationsDesc();
    
    @Query(value = "SELECT * FROM tool_performance_view ORDER BY calculated_avg_review DESC", nativeQuery = true)
    List<ToolPerformanceView> findAllOrderByAvgReviewDesc();
    
    @Query(value = "SELECT * FROM tool_performance_view WHERE owner_id = :ownerId", nativeQuery = true)
    List<ToolPerformanceView> findByOwnerId(Long ownerId);
    
    @Query(value = "SELECT * FROM (" +
            "SELECT * FROM tool_performance_view WHERE total_reservations >= :minReservations " +
            "UNION " +
            "SELECT * FROM tool_performance_view WHERE calculated_avg_review >= :minReview " +
            ") AS combined_results ORDER BY total_reservations DESC, calculated_avg_review DESC", 
            nativeQuery = true)
    List<ToolPerformanceView> findTopPerformersByReservationsOrReviews(
            @Param("minReservations") Long minReservations, 
            @Param("minReview") Double minReview);
}


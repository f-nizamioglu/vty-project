package com.vty.vty.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Immutable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tool_performance_view")
public class ToolPerformanceView {
    
    @Id
    @Column(name = "tool_id")
    private Long toolId;
    
    @Column(name = "tool_name")
    private String toolName;
    
    @Column(name = "tool_created_date")
    private LocalDateTime toolCreatedDate;
    
    @Column(name = "tool_avg_review")
    private Double toolAvgReview;
    
    @Column(name = "owner_id")
    private Long ownerId;
    
    @Column(name = "owner_name")
    private String ownerName;
    
    @Column(name = "owner_email")
    private String ownerEmail;
    
    @Column(name = "owner_phone")
    private String ownerPhone;
    
    @Column(name = "total_reservations")
    private Long totalReservations;
    
    @Column(name = "total_reviews")
    private Long totalReviews;
    
    @Column(name = "calculated_avg_review")
    private Double calculatedAvgReview;
    
    @Column(name = "last_reservation_date")
    private LocalDate lastReservationDate;
    
    @Column(name = "first_reservation_date")
    private LocalDate firstReservationDate;
}


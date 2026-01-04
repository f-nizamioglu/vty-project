package com.vty.vty.repository;

import com.vty.vty.entity.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ToolRepository extends JpaRepository<Tool, Long> {
    
    @Query(value = "SELECT * FROM tools WHERE user_id = :userId", nativeQuery = true)
    List<Tool> findByUserId(@Param("userId") Long userId);
    
    @Query(value = "SELECT * FROM tools WHERE user_id != :userId ORDER BY created DESC", nativeQuery = true)
    List<Tool> findAllExceptUserId(@Param("userId") Long userId);
    
    @Query(value = "SELECT * FROM tools WHERE id = :id", nativeQuery = true)
    Optional<Tool> findById(@Param("id") Long id);
    
    @Override
    @Query(value = "SELECT * FROM tools ORDER BY created DESC", nativeQuery = true)
    List<Tool> findAll();
    
    @Query(value = "SELECT * FROM get_tool_reservation_stats(:toolId)", 
           nativeQuery = true)
    List<Object[]> getToolReservationStats(@Param("toolId") Long toolId);
}


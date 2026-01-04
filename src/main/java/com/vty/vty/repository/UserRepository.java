package com.vty.vty.repository;


import com.vty.vty.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


import java.util.List;

public interface UserRepository extends JpaRepository<User,Long> {
    @Query(value = "SELECT * FROM users u WHERE u.email = :email and u.phone_number = :phone", nativeQuery = true)
    Optional<User> findByEmailAAndPhoneNumber(@Param("email") String email, @Param("phone") String phoneNumber);
    
    @Query(value = "SELECT * FROM users u WHERE u.email = :email", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query(value = "SELECT * FROM users ORDER BY created DESC", nativeQuery = true)
    List<User> findAll();
}

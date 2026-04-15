package com.omnicharge.user.repository;

import com.omnicharge.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByGoogleId(String googleId);
    
    Optional<User> findByMobileNumber(String mobileNumber);
    
    boolean existsByEmail(String email);
    
    boolean existsByMobileNumber(String mobileNumber);
    
    boolean existsByGoogleId(String googleId);
    
    // ========== ANALYTICS QUERIES ==========
    
    long countByIsActive(Boolean isActive);
    
    long countByCreatedDateAfter(LocalDateTime date);
    
    long countByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT DATE(u.createdDate) as date, COUNT(u) as count " +
           "FROM User u " +
           "WHERE u.createdDate >= :startDate AND u.createdDate <= :endDate " +
           "GROUP BY DATE(u.createdDate) " +
           "ORDER BY DATE(u.createdDate)")
    List<Object[]> findDailyUserGrowth(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
}

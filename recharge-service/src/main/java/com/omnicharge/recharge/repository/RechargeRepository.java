package com.omnicharge.recharge.repository;

import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RechargeRepository extends JpaRepository<Recharge, Long> {

    Optional<Recharge> findByRechargeId(String rechargeId);

    Page<Recharge> findByUserId(Long userId, Pageable pageable);

    long countByStatus(RechargeStatus status);

    List<Recharge> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r FROM Recharge r WHERE r.status = :status AND r.planExpiryDate = :expiryDate")
    List<Recharge> findByStatusAndPlanExpiryDate(@Param("status") RechargeStatus status, @Param("expiryDate") LocalDate expiryDate);

    @Query("SELECT r FROM Recharge r WHERE r.status = :status AND r.planExpiryDate BETWEEN :startDate AND :endDate")
    List<Recharge> findByStatusAndPlanExpiryDateBetween(
            @Param("status") RechargeStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Downtime-resilient: finds ALL SUCCESS recharges whose planExpiryDate has passed */
    @Query("SELECT r FROM Recharge r WHERE r.status = :status AND r.planExpiryDate < :cutoffDate")
    List<Recharge> findByStatusAndPlanExpiryDateBefore(
            @Param("status") RechargeStatus status,
            @Param("cutoffDate") LocalDate cutoffDate);
    
    // ========== ENTERPRISE BI ANALYTICS QUERIES ==========
    
    /**
     * Get top performing plans by recharge count and revenue
     */
    @Query("SELECT r.planId, r.planName, r.operatorId, r.operatorName, COUNT(r), SUM(r.amount), AVG(r.amount) " +
           "FROM Recharge r " +
           "WHERE r.status = :status " +
           "GROUP BY r.planId, r.planName, r.operatorId, r.operatorName " +
           "ORDER BY SUM(r.amount) DESC")
    List<Object[]> findTopPlansByRevenue(@Param("status") RechargeStatus status, Pageable pageable);
    
    /**
     * Get top performing plans by recharge count and revenue (with date filter)
     */
    @Query("SELECT r.planId, r.planName, r.operatorId, r.operatorName, COUNT(r), SUM(r.amount), AVG(r.amount) " +
           "FROM Recharge r " +
           "WHERE r.status = :status AND r.createdDate BETWEEN :startDate AND :endDate " +
           "GROUP BY r.planId, r.planName, r.operatorId, r.operatorName " +
           "ORDER BY SUM(r.amount) DESC")
    List<Object[]> findTopPlansByRevenueWithDateFilter(
            @Param("status") RechargeStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    /**
     * Get operator market share (recharge count and revenue by operator)
     */
    @Query("SELECT r.operatorId, r.operatorName, COUNT(r), SUM(r.amount) " +
           "FROM Recharge r " +
           "WHERE r.status = :status " +
           "GROUP BY r.operatorId, r.operatorName " +
           "ORDER BY SUM(r.amount) DESC")
    List<Object[]> findOperatorMarketShare(@Param("status") RechargeStatus status);
    
    /**
     * Get operator market share (recharge count and revenue by operator) with date filter
     */
    @Query("SELECT r.operatorId, r.operatorName, COUNT(r), SUM(r.amount) " +
           "FROM Recharge r " +
           "WHERE r.status = :status AND r.createdDate BETWEEN :startDate AND :endDate " +
           "GROUP BY r.operatorId, r.operatorName " +
           "ORDER BY SUM(r.amount) DESC")
    List<Object[]> findOperatorMarketShareWithDateFilter(
            @Param("status") RechargeStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get recharge count for a specific date range
     */
    @Query("SELECT COUNT(r) FROM Recharge r " +
           "WHERE r.createdDate BETWEEN :startDate AND :endDate " +
           "AND r.status = :status")
    Long countRechargesBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") RechargeStatus status);
    
    /**
     * Get revenue for a specific date range
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Recharge r " +
           "WHERE r.createdDate BETWEEN :startDate AND :endDate " +
           "AND r.status = :status")
    BigDecimal sumAmountBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") RechargeStatus status);
    
    /**
     * Count active recharges (not expired)
     */
    @Query("SELECT COUNT(r) FROM Recharge r " +
           "WHERE r.status = :status AND r.planExpiryDate >= :today")
    Long countActiveRecharges(@Param("status") RechargeStatus status, @Param("today") LocalDate today);
    
    /**
     * Count expired recharges
     */
    @Query("SELECT COUNT(r) FROM Recharge r " +
           "WHERE r.status = :status AND r.planExpiryDate < :today")
    Long countExpiredRecharges(@Param("status") RechargeStatus status, @Param("today") LocalDate today);
    
    /**
     * Get all recharges for a specific plan (for drill-down)
     * Note: Removed hardcoded ORDER BY to allow Pageable sort to work
     */
    @Query("SELECT r FROM Recharge r WHERE r.planId = :planId")
    Page<Recharge> findAllByPlanId(@Param("planId") Long planId, Pageable pageable);
    
    /**
     * Get all recharges for a specific operator (for drill-down)
     * Note: Removed hardcoded ORDER BY to allow Pageable sort to work
     */
    @Query("SELECT r FROM Recharge r WHERE r.operatorId = :operatorId")
    Page<Recharge> findAllByOperatorId(@Param("operatorId") Long operatorId, Pageable pageable);
    
    /**
     * Get recharges for a plan filtered by status
     */
    @Query("SELECT r FROM Recharge r WHERE r.planId = :planId AND r.status = :status")
    Page<Recharge> findByPlanIdAndStatus(@Param("planId") Long planId, @Param("status") RechargeStatus status, Pageable pageable);
    
    /**
     * Get recharges for an operator filtered by status
     */
    @Query("SELECT r FROM Recharge r WHERE r.operatorId = :operatorId AND r.status = :status")
    Page<Recharge> findByOperatorIdAndStatus(@Param("operatorId") Long operatorId, @Param("status") RechargeStatus status, Pageable pageable);
    
    /**
     * Search recharges by rechargeId or mobileNumber for a plan
     */
    @Query("SELECT r FROM Recharge r WHERE r.planId = :planId AND (r.rechargeId LIKE %:search% OR r.mobileNumber LIKE %:search%)")
    Page<Recharge> searchByPlanId(@Param("planId") Long planId, @Param("search") String search, Pageable pageable);
    
    /**
     * Search recharges by rechargeId or mobileNumber for an operator
     */
    @Query("SELECT r FROM Recharge r WHERE r.operatorId = :operatorId AND (r.rechargeId LIKE %:search% OR r.mobileNumber LIKE %:search%)")
    Page<Recharge> searchByOperatorId(@Param("operatorId") Long operatorId, @Param("search") String search, Pageable pageable);
    
    /**
     * Get recharges for a user filtered by status
     */
    @Query("SELECT r FROM Recharge r WHERE r.userId = :userId AND r.status = :status")
    Page<Recharge> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") RechargeStatus status, Pageable pageable);
    
    /**
     * Search recharges by operator name, plan name, or mobile number for a user
     */
    @Query("SELECT r FROM Recharge r WHERE r.userId = :userId AND (r.operatorName LIKE %:search% OR r.planName LIKE %:search% OR r.mobileNumber LIKE %:search%)")
    Page<Recharge> searchByUserId(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);
}

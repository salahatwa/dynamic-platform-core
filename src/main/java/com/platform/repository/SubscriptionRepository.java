package com.platform.repository;

import com.platform.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    /**
     * Find subscription by corporate ID
     */
    Optional<Subscription> findByCorporateId(Long corporateId);
    
    /**
     * Find active subscription by corporate ID
     */
    @Query("SELECT s FROM Subscription s WHERE s.corporate.id = :corporateId AND s.status = 'ACTIVE' AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    Optional<Subscription> findActiveByCorporateId(@Param("corporateId") Long corporateId, @Param("now") LocalDateTime now);
    
//    @Query("""
//    		SELECT s FROM Subscription s
//    		WHERE s.corporate.id = :corporateId
//    		  AND s.status = 'ACTIVE'
//    		  AND (
//    		        s.expiresAt IS NULL 
//    		        OR s.expiresAt > COALESCE(:now, CURRENT_TIMESTAMP)
//    		      )
//    		""")
//    		Optional<Subscription> findActiveByCorporateId(
//    		    Long corporateId,
//    		    LocalDateTime now
//    		);



    
    /**
     * Find all subscriptions by tier
     */
    List<Subscription> findByTier(Subscription.SubscriptionTier tier);
    
    /**
     * Find all active subscriptions
     */
    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);
    
    /**
     * Find expiring subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.expiresAt BETWEEN :start AND :end")
    List<Subscription> findExpiringBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Count subscriptions by tier
     */
    long countByTier(Subscription.SubscriptionTier tier);
    
    /**
     * Check if corporate has active subscription
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s WHERE s.corporate.id = :corporateId AND s.status = 'ACTIVE'")
    boolean hasActiveSubscription(@Param("corporateId") Long corporateId);
}

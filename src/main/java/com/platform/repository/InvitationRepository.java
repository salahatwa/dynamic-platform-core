package com.platform.repository;

import com.platform.entity.Invitation;
import com.platform.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    
    @Query("SELECT i FROM Invitation i " +
           "LEFT JOIN FETCH i.corporate " +
           "LEFT JOIN FETCH i.invitedBy " +
           "LEFT JOIN FETCH i.roles " +
           "WHERE i.token = :token")
    Optional<Invitation> findByTokenWithDetails(@Param("token") String token);
    
    Optional<Invitation> findByToken(String token);
    
    @Query("SELECT i FROM Invitation i " +
           "LEFT JOIN FETCH i.invitedBy " +
           "LEFT JOIN FETCH i.acceptedBy " +
           "LEFT JOIN FETCH i.roles " +
           "WHERE i.corporate.id = :corporateId " +
           "ORDER BY i.createdAt DESC")
    List<Invitation> findByCorporateIdOrderByCreatedAtDesc(@Param("corporateId") Long corporateId);
    
    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);
    
    @Query("SELECT i FROM Invitation i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    List<Invitation> findExpiredInvitations(@Param("now") LocalDateTime now);
    
    boolean existsByEmailAndStatus(String email, InvitationStatus status);
}

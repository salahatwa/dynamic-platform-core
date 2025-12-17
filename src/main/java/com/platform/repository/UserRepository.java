package com.platform.repository;

import com.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.corporate WHERE u.id = :id")
    Optional<User> findByIdWithCorporate(@Param("id") Long id);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.corporate WHERE u.email = :email")
    Optional<User> findByEmailWithCorporate(@Param("email") String email);
    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
            String email, String name, Pageable pageable);
    Page<User> findByInvitedById(Long invitedById, Pageable pageable);
    java.util.List<User> findByInvitedById(Long invitedById);
    Page<User> findByInvitedByIdAndEmailContainingIgnoreCaseOrInvitedByIdAndNameContainingIgnoreCase(
            Long invitedById1, String email, Long invitedById2, String name, Pageable pageable);
	long countByEnabled(boolean b);
	
	// Corporate-filtered queries
	long countByCorporateId(Long corporateId);
	Page<User> findByCorporateId(Long corporateId, Pageable pageable);
	Page<User> findByCorporateIdAndEmailContainingIgnoreCaseOrCorporateIdAndNameContainingIgnoreCase(
            Long corporateId1, String email, Long corporateId2, String name, Pageable pageable);
    Page<User> findByCorporateIdAndInvitedById(Long corporateId, Long invitedById, Pageable pageable);
    Page<User> findByCorporateIdAndInvitedByIdAndEmailContainingIgnoreCaseOrCorporateIdAndInvitedByIdAndNameContainingIgnoreCase(
            Long corporateId1, Long invitedById1, String email, Long corporateId2, Long invitedById2, String name, Pageable pageable);
    
    // Corporate-filtered queries excluding current user
    Page<User> findByCorporateIdAndIdNot(Long corporateId, Long excludeUserId, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.corporate.id = :corporateId AND u.id != :excludeUserId AND " +
           "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByCorporateIdAndIdNotWithSearch(@Param("corporateId") Long corporateId, 
                                                   @Param("excludeUserId") Long excludeUserId, 
                                                   @Param("search") String search, 
                                                   Pageable pageable);
    
    Page<User> findByCorporateIdAndInvitedByIdAndIdNot(Long corporateId, Long invitedById, Long excludeUserId, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.corporate.id = :corporateId AND u.invitedBy.id = :invitedById AND u.id != :excludeUserId AND " +
           "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByCorporateIdAndInvitedByIdAndIdNotWithSearch(@Param("corporateId") Long corporateId, 
                                                                @Param("invitedById") Long invitedById,
                                                                @Param("excludeUserId") Long excludeUserId, 
                                                                @Param("search") String search, 
                                                                Pageable pageable);
}

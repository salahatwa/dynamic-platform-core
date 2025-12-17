package com.platform.repository;

import com.platform.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // Find all system roles (not corporate-specific)
    List<Role> findByIsSystemRoleTrue();
    
    // Find all custom roles for a specific corporate
    Page<Role> findByCorporateIdAndIsSystemRoleFalse(Long corporateId, Pageable pageable);
    
    // Find custom roles with search
    @Query("SELECT r FROM Role r WHERE r.corporate.id = :corporateId AND r.isSystemRole = false AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Role> findCustomRolesWithSearch(@Param("corporateId") Long corporateId, 
                                        @Param("search") String search, 
                                        Pageable pageable);
    
    // Find role by name and corporate (for uniqueness check)
    Optional<Role> findByNameAndCorporateId(String name, Long corporateId);
    
    // Find role by name (system roles)
    Optional<Role> findByNameAndIsSystemRoleTrue(String name);
    
    // Check if role name exists for corporate
    boolean existsByNameAndCorporateId(String name, Long corporateId);
    
    // Get all roles for a corporate (system + custom)
    @Query("SELECT r FROM Role r WHERE r.isSystemRole = true OR r.corporate.id = :corporateId")
    List<Role> findAllAvailableRoles(@Param("corporateId") Long corporateId);
    
    // Legacy methods for backward compatibility
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
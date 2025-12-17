package com.platform.repository;

import com.platform.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByName(String name);
    
    List<Permission> findByResource(String resource);
    
    List<Permission> findByAction(String action);
    
    Optional<Permission> findByResourceAndAction(String resource, String action);
    
    @Query("SELECT p FROM Permission p ORDER BY p.resource, p.action")
    List<Permission> findAllOrderedByResourceAndAction();
    
    @Query("SELECT DISTINCT p.resource FROM Permission p ORDER BY p.resource")
    List<String> findAllResources();
    
    @Query("SELECT DISTINCT p.action FROM Permission p ORDER BY p.action")
    List<String> findAllActions();
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource ORDER BY p.action")
    List<Permission> findByResourceOrderByAction(@Param("resource") String resource);
    
    // Legacy method for backward compatibility
    boolean existsByName(String name);
}
package com.platform.service;

import com.platform.entity.Permission;
import com.platform.entity.User;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final UserRepository userRepository;
    
    /**
     * Check if current user has permission for a specific resource and action
     */
    public boolean hasPermission(PermissionResource resource, PermissionAction action) {
        return hasPermission(resource.getResource(), action.getAction());
    }
    
    /**
     * Check if current user has permission for a specific resource and action
     */
    public boolean hasPermission(String resource, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return false;
        }
        
        return hasPermission(user, resource, action);
    }
    
    /**
     * Check if a specific user has permission for a resource and action
     */
    public boolean hasPermission(User user, String resource, String action) {
        String permissionName = resource.toUpperCase() + "_" + action.toUpperCase();
        
        // Check direct user permissions
        boolean hasDirectPermission = user.getPermissions().stream()
            .anyMatch(permission -> permission.getName().equals(permissionName));
        
        if (hasDirectPermission) {
            return true;
        }
        
        // Check role-based permissions
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(permission -> permission.getName().equals(permissionName));
    }
    
    /**
     * Get all permissions for current user
     */
    public Set<String> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return Set.of();
        }
        
        return getUserPermissions(user);
    }
    
    /**
     * Get all permissions for a specific user
     */
    public Set<String> getUserPermissions(User user) {
        Set<String> permissions = user.getPermissions().stream()
            .map(Permission::getName)
            .collect(Collectors.toSet());
        
        // Add role-based permissions
        user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getName)
            .forEach(permissions::add);
        
        return permissions;
    }
    
    /**
     * Get permissions grouped by resource for current user
     */
    public Set<String> getCurrentUserResourcePermissions(PermissionResource resource) {
        return getCurrentUserResourcePermissions(resource.getResource());
    }
    
    /**
     * Get permissions for a specific resource for current user
     */
    public Set<String> getCurrentUserResourcePermissions(String resource) {
        return getCurrentUserPermissions().stream()
            .filter(permission -> permission.startsWith(resource.toUpperCase() + "_"))
            .map(permission -> permission.substring(resource.length() + 1).toLowerCase())
            .collect(Collectors.toSet());
    }
    
    /**
     * Check if current user has any permission for a resource
     */
    public boolean hasAnyPermissionForResource(PermissionResource resource) {
        return hasAnyPermissionForResource(resource.getResource());
    }
    
    /**
     * Check if current user has any permission for a resource
     */
    public boolean hasAnyPermissionForResource(String resource) {
        return getCurrentUserPermissions().stream()
            .anyMatch(permission -> permission.startsWith(resource.toUpperCase() + "_"));
    }
    
    /**
     * Check if current user is super admin
     */
    public boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return false;
        }
        
        return user.getRoles().stream()
            .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));
    }
}
package com.platform.controller;

import com.platform.entity.Permission;
import com.platform.entity.Role;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.security.RequirePermission;
import com.platform.service.PermissionService;
import com.platform.repository.PermissionRepository;
import com.platform.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Permission Management", description = "Permission and Role Management APIs")
public class PermissionController {
    
    private final PermissionService permissionService;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    
    @GetMapping("/current-user")
    @Operation(summary = "Get current user permissions")
    public ResponseEntity<Map<String, Object>> getCurrentUserPermissions() {
        Set<String> permissions = permissionService.getCurrentUserPermissions();
        boolean isSuperAdmin = permissionService.isSuperAdmin();
        
        // Group permissions by resource
        Map<String, Set<String>> permissionsByResource = new HashMap<>();
        for (PermissionResource resource : PermissionResource.values()) {
            Set<String> resourcePermissions = permissionService.getCurrentUserResourcePermissions(resource);
            if (!resourcePermissions.isEmpty()) {
                permissionsByResource.put(resource.getResource(), resourcePermissions);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("permissions", permissions);
        response.put("permissionsByResource", permissionsByResource);
        response.put("isSuperAdmin", isSuperAdmin);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/resources")
    @Operation(summary = "Get all available resources")
    public ResponseEntity<List<Map<String, String>>> getResources() {
        List<Map<String, String>> resources = Arrays.stream(PermissionResource.values())
            .map(resource -> {
                Map<String, String> resourceMap = new HashMap<>();
                resourceMap.put("key", resource.getResource());
                resourceMap.put("name", resource.getDescription());
                return resourceMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(resources);
    }
    
    @GetMapping("/actions")
    @Operation(summary = "Get all available actions")
    public ResponseEntity<List<Map<String, String>>> getActions() {
        List<Map<String, String>> actions = Arrays.stream(PermissionAction.values())
            .map(action -> {
                Map<String, String> actionMap = new HashMap<>();
                actionMap.put("key", action.getAction());
                actionMap.put("name", action.getDescription());
                return actionMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/all")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get all permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return ResponseEntity.ok(permissions);
    }
    
    @GetMapping("/roles")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get all roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(roles);
    }
    
    @PostMapping("/roles")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.CREATE)
    @Operation(summary = "Create new role")
    public ResponseEntity<Role> createRole(@RequestBody CreateRoleRequest request) {
        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null) {
            permissions = request.getPermissionIds().stream()
                .map(permissionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        }
        
        Role role = Role.builder()
            .name(request.getName())
            .description(request.getDescription())
            .permissions(permissions)
            .build();
        
        Role savedRole = roleRepository.save(role);
        return ResponseEntity.ok(savedRole);
    }
    
    @PutMapping("/roles/{id}")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.UPDATE)
    @Operation(summary = "Update role")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
        
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        
        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = request.getPermissionIds().stream()
                .map(permissionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }
        
        Role savedRole = roleRepository.save(role);
        return ResponseEntity.ok(savedRole);
    }
    
    @DeleteMapping("/roles/{id}")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.DELETE)
    @Operation(summary = "Delete role")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // DTO classes
    public static class CreateRoleRequest {
        private String name;
        private String description;
        private Set<Long> permissionIds;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Set<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(Set<Long> permissionIds) { this.permissionIds = permissionIds; }
    }
}
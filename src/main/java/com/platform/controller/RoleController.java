package com.platform.controller;

import com.platform.dto.PermissionDTO;
import com.platform.dto.RoleDTO;
import com.platform.entity.Permission;
import com.platform.entity.Role;
import com.platform.entity.User;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.PermissionRepository;
import com.platform.repository.RoleRepository;
import com.platform.repository.UserRepository;
import com.platform.security.RequirePermission;
import com.platform.security.UserPrincipal;
import com.platform.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get all roles (system + custom) with pagination")
    public ResponseEntity<?> getAllRoles(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(defaultValue = "false") boolean customOnly) {
        
        Long corporateId = getCorporateId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        
        Page<Role> roles;
        
        if (customOnly) {
            if (search != null && !search.isEmpty()) {
                roles = roleRepository.findCustomRolesWithSearch(corporateId, search, pageable);
            } else {
                roles = roleRepository.findByCorporateIdAndIsSystemRoleFalse(corporateId, pageable);
            }
        } else {
            // For now, return custom roles only as system roles are handled separately
            if (search != null && !search.isEmpty()) {
                roles = roleRepository.findCustomRolesWithSearch(corporateId, search, pageable);
            } else {
                roles = roleRepository.findByCorporateIdAndIsSystemRoleFalse(corporateId, pageable);
            }
        }
        
        Page<RoleDTO> roleDTOs = roles.map(RoleDTO::fromEntity);
        return ResponseEntity.ok(roleDTOs);
    }

    @GetMapping("/system")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get all system roles")
    public ResponseEntity<?> getSystemRoles() {
        List<Role> systemRoles = roleRepository.findByIsSystemRoleTrue();
        List<RoleDTO> roleDTOs = systemRoles.stream()
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDTOs);
    }

    @GetMapping("/available")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get all available roles for assignment (system + custom)")
    public ResponseEntity<?> getAvailableRoles() {
        Long corporateId = getCorporateId();
        List<Role> roles = roleRepository.findAllAvailableRoles(corporateId);
        List<RoleDTO> roleDTOs = roles.stream()
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDTOs);
    }

    @GetMapping("/{id}")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get role by ID")
    public ResponseEntity<?> getRoleById(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        // Verify access: system roles are accessible, custom roles must belong to same corporate
        if (!role.getIsSystemRole() && 
            (role.getCorporate() == null || !role.getCorporate().getId().equals(corporateId))) {
            return ResponseEntity.status(403).body("Access denied: Role belongs to another organization");
        }
        
        return ResponseEntity.ok(RoleDTO.fromEntity(role));
    }

    @PostMapping
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.CREATE)
    @Operation(summary = "Create custom role")
    public ResponseEntity<?> createRole(@RequestBody CreateRoleRequest request, HttpServletRequest httpRequest) {
        Long corporateId = getCorporateId();
        
        // Check if role name already exists for this corporate
        if (roleRepository.existsByNameAndCorporateId(request.getName(), corporateId)) {
            return ResponseEntity.badRequest().body("Role name already exists in your organization");
        }
        
        // Get corporate entity
        User currentUser = getCurrentUserWithCorporate();
        
        // Create role
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .corporate(currentUser.getCorporate())
                .isSystemRole(false)
                .permissions(new HashSet<>())
                .build();
        
        // Add permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(
                permissionRepository.findAllById(request.getPermissionIds())
            );
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        
        // Log audit
        auditLogService.log("CREATE", "ROLE", role.getId(), role.getName(), currentUser,
                "Created custom role", httpRequest.getRemoteAddr());
        
        return ResponseEntity.ok(RoleDTO.fromEntity(role));
    }

    @PutMapping("/{id}")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.UPDATE)
    @Operation(summary = "Update custom role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request, 
                                       HttpServletRequest httpRequest) {
        Long corporateId = getCorporateId();
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        // Verify it's a custom role belonging to same corporate
        if (role.getIsSystemRole()) {
            return ResponseEntity.badRequest().body("Cannot modify system roles");
        }
        
        if (role.getCorporate() == null || !role.getCorporate().getId().equals(corporateId)) {
            return ResponseEntity.status(403).body("Access denied: Role belongs to another organization");
        }
        
        // Check name uniqueness if name is being changed
        if (!role.getName().equals(request.getName()) && 
            roleRepository.existsByNameAndCorporateId(request.getName(), corporateId)) {
            return ResponseEntity.badRequest().body("Role name already exists in your organization");
        }
        
        // Update role
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        
        // Update permissions
        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(
                permissionRepository.findAllById(request.getPermissionIds())
            );
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        
        // Log audit
        User currentUser = getCurrentUserWithCorporate();
        auditLogService.log("UPDATE", "ROLE", role.getId(), role.getName(), currentUser,
                "Updated custom role", httpRequest.getRemoteAddr());
        
        return ResponseEntity.ok(RoleDTO.fromEntity(role));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.DELETE)
    @Operation(summary = "Delete custom role")
    public ResponseEntity<?> deleteRole(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long corporateId = getCorporateId();
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        // Verify it's a custom role belonging to same corporate
        if (role.getIsSystemRole()) {
            return ResponseEntity.badRequest().body("Cannot delete system roles");
        }
        
        if (role.getCorporate() == null || !role.getCorporate().getId().equals(corporateId)) {
            return ResponseEntity.status(403).body("Access denied: Role belongs to another organization");
        }
        
        // Check if role is assigned to any users
        // This would require checking user_roles table - for now we'll allow deletion
        
        String roleName = role.getName();
        roleRepository.delete(role);
        
        // Log audit
        User currentUser = getCurrentUserWithCorporate();
        auditLogService.log("DELETE", "ROLE", id, roleName, currentUser,
                "Deleted custom role", httpRequest.getRemoteAddr());
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/permissions")
    @RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
    @Operation(summary = "Get all available permissions")
    public ResponseEntity<?> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        List<PermissionDTO> permissionDTOs = permissions.stream()
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissionDTOs);
    }

    // Helper methods
    private Long getCorporateId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long corporateId = userPrincipal.getCorporateId();
            
            if (corporateId == null) {
                throw new RuntimeException("User is not associated with any organization");
            }
            
            return corporateId;
        }
        
        throw new RuntimeException("Authentication required");
    }

    private User getCurrentUserWithCorporate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userRepository.findByIdWithCorporate(userPrincipal.getId()).orElse(null);
        }
        return null;
    }

    // Request DTOs
    @lombok.Data
    public static class CreateRoleRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;
    }

    @lombok.Data
    public static class UpdateRoleRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;
    }
}
package com.platform;

import com.platform.entity.Permission;
import com.platform.entity.Role;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.PermissionRepository;
import com.platform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionInitializationService implements CommandLineRunner {
    
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing permissions and roles...");
        
        // Create all permissions for all resources
        createPermissions();
        
        // Create default roles
        createDefaultRoles();
        
        log.info("Permission and role initialization completed.");
    }
    
    private void createPermissions() {
        for (PermissionResource resource : PermissionResource.values()) {
            for (PermissionAction action : PermissionAction.values()) {
                String permissionName = resource.getResource().toUpperCase() + "_" + action.getAction().toUpperCase();
                
                if (!permissionRepository.existsByName(permissionName)) {
                    Permission permission = Permission.builder()
                        .name(permissionName)
                        .description(action.getDescription() + " " + resource.getDescription())
                        .resource(resource.getResource())
                        .action(action.getAction())
                        .build();
                    
                    permissionRepository.save(permission);
                    log.debug("Created permission: {}", permissionName);
                }
            }
        }
    }
    
    private void createDefaultRoles() {
        // Create SUPER_ADMIN role with all permissions
        createSuperAdminRole();
        
        // Create ADMIN role with most permissions (excluding user management)
        createAdminRole();
        
        // Create EDITOR role with content management permissions
        createEditorRole();
        
        // Create VIEWER role with read-only permissions
        createViewerRole();
    }
    
    private void createSuperAdminRole() {
        if (!roleRepository.existsByName("SUPER_ADMIN")) {
            Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
            
            Role superAdminRole = Role.builder()
                .name("SUPER_ADMIN")
                .description("Super Administrator with full system access")
                .permissions(allPermissions)
                .build();
            
            roleRepository.save(superAdminRole);
            log.info("Created SUPER_ADMIN role with {} permissions", allPermissions.size());
        }
    }
    
    private void createAdminRole() {
        if (!roleRepository.existsByName("ADMIN")) {
            Set<Permission> adminPermissions = new HashSet<>();
            
            // Add all permissions except user/role management
            for (PermissionResource resource : PermissionResource.values()) {
                if (resource != PermissionResource.USERS) {
                    for (PermissionAction action : PermissionAction.values()) {
                        String permissionName = resource.getResource().toUpperCase() + "_" + action.getAction().toUpperCase();
                        permissionRepository.findByName(permissionName)
                            .ifPresent(adminPermissions::add);
                    }
                }
            }
            
            // Add read permissions for users and roles
            permissionRepository.findByName("USERS_READ").ifPresent(adminPermissions::add);
            permissionRepository.findByName("USERS_UPDATE").ifPresent(adminPermissions::add);
            permissionRepository.findByName("USERS_DELETE").ifPresent(adminPermissions::add);
            
            Role adminRole = Role.builder()
                .name("ADMIN")
                .description("Administrator with content management access")
                .permissions(adminPermissions)
                .build();
            
            roleRepository.save(adminRole);
            log.info("Created ADMIN role with {} permissions", adminPermissions.size());
        }
    }
    
    private void createEditorRole() {
        if (!roleRepository.existsByName("EDITOR")) {
            Set<Permission> editorPermissions = new HashSet<>();
            
            // Add CRUD permissions for content modules
            PermissionResource[] contentResources = {
                PermissionResource.TRANSLATIONS,
                PermissionResource.TEMPLATES,
                PermissionResource.LOV,
                PermissionResource.ERROR_CODES,
                PermissionResource.MEDIA
            };
            
            for (PermissionResource resource : contentResources) {
                for (PermissionAction action : PermissionAction.values()) {
                    String permissionName = resource.getResource().toUpperCase() + "_" + action.getAction().toUpperCase();
                    permissionRepository.findByName(permissionName)
                        .ifPresent(editorPermissions::add);
                }
            }
            
            // Add read permissions for other modules
            permissionRepository.findByName("DASHBOARD_READ").ifPresent(editorPermissions::add);
            permissionRepository.findByName("APP_CONFIG_READ").ifPresent(editorPermissions::add);
            permissionRepository.findByName("MEDIA_READ").ifPresent(editorPermissions::add);
            
            Role editorRole = Role.builder()
                .name("EDITOR")
                .description("Content Editor with CRUD access to content modules")
                .permissions(editorPermissions)
                .build();
            
            roleRepository.save(editorRole);
            log.info("Created EDITOR role with {} permissions", editorPermissions.size());
        }
    }
    
    private void createViewerRole() {
        if (!roleRepository.existsByName("VIEWER")) {
            Set<Permission> viewerPermissions = new HashSet<>();
            
            // Add only READ permissions for all resources
            for (PermissionResource resource : PermissionResource.values()) {
                String permissionName = resource.getResource().toUpperCase() + "_READ";
                permissionRepository.findByName(permissionName)
                    .ifPresent(viewerPermissions::add);
            }
            
            Role viewerRole = Role.builder()
                .name("VIEWER")
                .description("Read-only access to all modules")
                .permissions(viewerPermissions)
                .build();
            
            roleRepository.save(viewerRole);
            log.info("Created VIEWER role with {} permissions", viewerPermissions.size());
        }
    }
}
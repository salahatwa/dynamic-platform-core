package com.platform.service;

import com.platform.entity.Permission;
import com.platform.entity.Role;
import com.platform.entity.User;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PermissionService permissionService;

    private User testUser;
    private Role adminRole;
    private Permission translationCreatePermission;

    @BeforeEach
    void setUp() {
        // Create test permission
        translationCreatePermission = Permission.builder()
            .name("TRANSLATIONS_CREATE")
            .description("Create translations")
            .resource("translations")
            .action("create")
            .build();

        // Create test role with permission
        adminRole = Role.builder()
            .name("ADMIN")
            .description("Administrator")
            .permissions(Set.of(translationCreatePermission))
            .build();

        // Create test user with role
        testUser = User.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .roles(Set.of(adminRole))
            .permissions(new HashSet<>())
            .build();

        // Mock security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void testHasPermission_WithRoleBasedPermission_ShouldReturnTrue() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean hasPermission = permissionService.hasPermission(PermissionResource.TRANSLATIONS, PermissionAction.CREATE);

        // Then
        assertTrue(hasPermission);
    }

    @Test
    void testHasPermission_WithoutPermission_ShouldReturnFalse() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean hasPermission = permissionService.hasPermission(PermissionResource.TRANSLATIONS, PermissionAction.DELETE);

        // Then
        assertFalse(hasPermission);
    }

    @Test
    void testHasPermission_UserNotFound_ShouldReturnFalse() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // When
        boolean hasPermission = permissionService.hasPermission(PermissionResource.TRANSLATIONS, PermissionAction.CREATE);

        // Then
        assertFalse(hasPermission);
    }

    @Test
    void testHasPermission_NotAuthenticated_ShouldReturnFalse() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean hasPermission = permissionService.hasPermission(PermissionResource.TRANSLATIONS, PermissionAction.CREATE);

        // Then
        assertFalse(hasPermission);
    }

    @Test
    void testGetUserPermissions_ShouldReturnAllPermissions() {
        // When
        Set<String> permissions = permissionService.getUserPermissions(testUser);

        // Then
        assertEquals(1, permissions.size());
        assertTrue(permissions.contains("TRANSLATIONS_CREATE"));
    }

    @Test
    void testHasAnyPermissionForResource_ShouldReturnTrue() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean hasAnyPermission = permissionService.hasAnyPermissionForResource(PermissionResource.TRANSLATIONS);

        // Then
        assertTrue(hasAnyPermission);
    }

    @Test
    void testIsSuperAdmin_WithSuperAdminRole_ShouldReturnTrue() {
        // Given
        Role superAdminRole = Role.builder()
            .name("SUPER_ADMIN")
            .description("Super Administrator")
            .build();
        
        testUser.getRoles().clear();
        testUser.getRoles().add(superAdminRole);
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean isSuperAdmin = permissionService.isSuperAdmin();

        // Then
        assertTrue(isSuperAdmin);
    }

    @Test
    void testIsSuperAdmin_WithoutSuperAdminRole_ShouldReturnFalse() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean isSuperAdmin = permissionService.isSuperAdmin();

        // Then
        assertFalse(isSuperAdmin);
    }
}
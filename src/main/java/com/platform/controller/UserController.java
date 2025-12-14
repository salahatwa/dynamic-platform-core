package com.platform.controller;

import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.AuditLogService;
import com.platform.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
    
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean invitedByMe,
            Authentication authentication) {
        
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<User> users;
        Long corporateId = currentUser.getCorporate().getId();
        
        if (invitedByMe) {
            if (search != null && !search.isEmpty()) {
                users = userRepository.findByCorporateIdAndInvitedByIdAndEmailContainingIgnoreCaseOrCorporateIdAndInvitedByIdAndNameContainingIgnoreCase(
                        corporateId, currentUser.getId(), search, corporateId, currentUser.getId(), search, pageable);
            } else {
                users = userRepository.findByCorporateIdAndInvitedById(corporateId, currentUser.getId(), pageable);
            }
        } else {
            if (search != null && !search.isEmpty()) {
                users = userRepository.findByCorporateIdAndEmailContainingIgnoreCaseOrCorporateIdAndNameContainingIgnoreCase(
                        corporateId, search, corporateId, search, pageable);
            } else {
                users = userRepository.findByCorporateId(corporateId, pageable);
            }
        }
        
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user belongs to same corporate
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
        }
        
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate user")
    public ResponseEntity<?> activateUser(@PathVariable Long id, HttpServletRequest request) {
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user belongs to same corporate
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
        }
        
        user.setEnabled(true);
        userRepository.save(user);
        
        // Log audit
        auditLogService.log(
            "ACTIVATE",
            "USER",
            id,
            user.getEmail(),
            currentUser,
            "Activated user account",
            request.getRemoteAddr()
        );
        
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id, HttpServletRequest request) {
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user belongs to same corporate
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
        }
        
        user.setEnabled(false);
        userRepository.save(user);
        
        // Log audit
        auditLogService.log(
            "DEACTIVATE",
            "USER",
            id,
            user.getEmail(),
            currentUser,
            "Deactivated user account",
            request.getRemoteAddr()
        );
        
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/deletion-impact")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user deletion impact information")
    public ResponseEntity<?> getUserDeletionImpact(@PathVariable Long id) {
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user belongs to same corporate
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
        }
        
        UserService.UserDeletionImpact impact = userService.getDeletionImpact(id);
        return ResponseEntity.ok(impact);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user belongs to same corporate
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
        }
        
        // Prevent self-deletion
        if (user.getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body("Cannot delete your own account");
        }
        
        String userEmail = user.getEmail();
        
        try {
            // Use the service to handle foreign key constraints properly
            userService.deleteUser(id);
            
            // Log audit
            auditLogService.log(
                "DELETE",
                "USER",
                id,
                userEmail,
                currentUser,
                "Deleted user account with cascade handling",
                request.getRemoteAddr()
            );
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            // Log the error and return a user-friendly message
            auditLogService.log(
                "DELETE_FAILED",
                "USER",
                id,
                userEmail,
                currentUser,
                "Failed to delete user: " + e.getMessage(),
                request.getRemoteAddr()
            );
            
            return ResponseEntity.status(500).body("Failed to delete user: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete user (deactivate)")
    public ResponseEntity<?> softDeleteUser(@PathVariable Long id, HttpServletRequest request) {
        // Get current user with corporate
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify user belongs to same corporate
        if (user.getCorporate() == null || !user.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
        }
        
        // Prevent self-deletion
        if (user.getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body("Cannot delete your own account");
        }
        
        String userEmail = user.getEmail();
        userService.softDeleteUser(id);
        
        // Log audit
        auditLogService.log(
            "SOFT_DELETE",
            "USER",
            id,
            userEmail,
            currentUser,
            "Soft deleted user account (deactivated)",
            request.getRemoteAddr()
        );
        
        return ResponseEntity.ok().build();
    }
    
    private User getCurrentUserWithCorporate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userRepository.findByIdWithCorporate(userPrincipal.getId()).orElse(null);
        }
        return null;
    }
}

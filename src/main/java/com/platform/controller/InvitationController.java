package com.platform.controller;

import com.platform.dto.InvitationRequest;
import com.platform.dto.InvitationResponse;
import com.platform.dto.InvitationValidationResponse;
import com.platform.entity.Invitation;
import com.platform.entity.Role;
import com.platform.entity.User;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.UserRepository;
import com.platform.security.RequirePermission;
import com.platform.security.UserPrincipal;
import com.platform.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {
    
    private final InvitationService invitationService;
    private final UserRepository userRepository;
    
    @PostMapping
    @RequirePermission(resource = PermissionResource.INVITATIONS, action = PermissionAction.CREATE)
    public ResponseEntity<InvitationResponse> createInvitation(
            @RequestBody @Valid InvitationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Creating invitation for email: {}", request.getEmail());
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        User inviter = userRepository.findByIdWithCorporate(principal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Invitation invitation = invitationService.createInvitation(
            inviter,
            request.getEmail(),
            request.getRoleIds()
        );
        
        return ResponseEntity.ok(InvitationResponse.from(invitation));
    }
    
    @GetMapping("/validate/{token}")
    public ResponseEntity<InvitationValidationResponse> validateInvitation(
            @PathVariable String token) {
        
        log.info("Validating invitation token");
        
        try {
            Invitation invitation = invitationService.validateInvitation(token);
            
            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(invitation.getEmail());
            
            return ResponseEntity.ok(InvitationValidationResponse.builder()
                .valid(true)
                .email(invitation.getEmail())
                .corporateName(invitation.getCorporate().getName())
                .inviterName(invitation.getInvitedBy().getFirstName() + " " + 
                           invitation.getInvitedBy().getLastName())
                .roles(invitation.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()))
                .userExists(existingUser.isPresent())
                .expiresAt(invitation.getExpiresAt())
                .build());
                
        } catch (Exception e) {
            log.error("Invitation validation failed", e);
            return ResponseEntity.ok(InvitationValidationResponse.builder()
                .valid(false)
                .errorMessage(e.getMessage())
                .build());
        }
    }
    
    @PostMapping("/accept/{token}")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("User accepting invitation");
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        try {
            user = invitationService.acceptInvitation(token, user);
            
            return ResponseEntity.ok().body(Map.of(
                "message", "Invitation accepted successfully",
                "corporateName", user.getCorporate().getName()
            ));
        } catch (Exception e) {
            log.error("Failed to accept invitation", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping
    @RequirePermission(resource = PermissionResource.INVITATIONS, action = PermissionAction.READ)
    public ResponseEntity<List<InvitationResponse>> getCorporateInvitations(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("=== Fetching corporate invitations ===");
        log.info("Principal: {}", principal);
        log.info("Principal ID: {}", principal != null ? principal.getId() : "NULL");
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        try {
            User admin = userRepository.findByIdWithCorporate(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("User loaded: {}, Corporate: {}", admin.getEmail(), admin.getCorporate());
            
            if (admin.getCorporate() == null) {
                log.error("User {} has no corporate assigned", admin.getEmail());
                throw new RuntimeException("User has no corporate assigned");
            }
            
            log.info("Corporate ID: {}, Name: {}", admin.getCorporate().getId(), admin.getCorporate().getName());
            
            List<Invitation> invitations = invitationService
                .getCorporateInvitations(admin.getCorporate().getId());
            
            log.info("Found {} invitations", invitations.size());
            
            return ResponseEntity.ok(invitations.stream()
                .map(InvitationResponse::from)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("ERROR in getCorporateInvitations", e);
            throw e;
        }
    }
    
    @PutMapping("/{id}/cancel")
    @RequirePermission(resource = PermissionResource.INVITATIONS, action = PermissionAction.DELETE)
    public ResponseEntity<?> cancelInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Cancelling invitation ID: {}", id);
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        User admin = userRepository.findByIdWithCorporate(principal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        try {
            invitationService.cancelInvitation(id, admin);
            return ResponseEntity.ok().body(Map.of(
                "message", "Invitation cancelled successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to cancel invitation", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{id}/resend")
    @RequirePermission(resource = PermissionResource.INVITATIONS, action = PermissionAction.UPDATE)
    public ResponseEntity<?> resendInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Resending invitation ID: {}", id);
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        User admin = userRepository.findByIdWithCorporate(principal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        try {
            Invitation invitation = invitationService.resendInvitation(id, admin);
            return ResponseEntity.ok(InvitationResponse.from(invitation));
        } catch (Exception e) {
            log.error("Failed to resend invitation", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}

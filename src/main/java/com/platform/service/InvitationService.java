package com.platform.service;

import com.platform.entity.Invitation;
import com.platform.entity.Role;
import com.platform.entity.User;
import com.platform.enums.InvitationStatus;
import com.platform.repository.InvitationRepository;
import com.platform.repository.RoleRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvitationService {
    
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    
    private static final int INVITATION_EXPIRY_DAYS = 7;
    private static final int TOKEN_LENGTH = 64;
    
    public Invitation createInvitation(User inviter, String email, Set<Long> roleIds) {
        log.info("Creating invitation for email: {} by user: {}", email, inviter.getEmail());
        
        // Validate email
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        // Check if user already exists and belongs to a corporate
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && existingUser.get().getCorporate() != null) {
            throw new IllegalStateException("User already belongs to a corporate");
        }
        
        // Check for pending invitations
        if (invitationRepository.existsByEmailAndStatus(email, InvitationStatus.PENDING)) {
            throw new IllegalStateException("User already has a pending invitation");
        }
        
        // Load roles
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("One or more roles not found");
        }
        
        // Generate secure token
        String token = generateSecureToken();
        
        // Create invitation
        Invitation invitation = Invitation.builder()
            .token(token)
            .email(email.toLowerCase())
            .corporate(inviter.getCorporate())
            .invitedBy(inviter)
            .roles(roles)
            .expiresAt(LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS))
            .build();
        
        invitation = invitationRepository.save(invitation);
        
        log.info("Invitation created with ID: {} for email: {}", invitation.getId(), email);
        
        // Send invitation email
        try {
            emailService.sendInvitationEmail(invitation);
        } catch (Exception e) {
            log.error("Failed to send invitation email", e);
            // Don't fail the invitation creation if email fails
        }
        
        // Audit log
        auditLogService.log(
            "INVITATION_CREATED",
            "INVITATION",
            invitation.getId(),
            email,
            inviter,
            "Invited user with roles: " + roles.stream()
                .map(Role::getName)
                .collect(Collectors.joining(", ")),
            null
        );
        
        return invitation;
    }
    
    @Transactional(readOnly = true)
    public Invitation validateInvitation(String token) {
        log.info("Validating invitation token: {}", token.substring(0, 10) + "...");
        
        Invitation invitation = invitationRepository.findByTokenWithDetails(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not pending");
        }
        
        if (invitation.isExpired()) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalStateException("Invitation has expired");
        }
        
        log.info("Invitation validated successfully for email: {}", invitation.getEmail());
        return invitation;
    }
    
    public User acceptInvitation(String token, User user) {
        log.info("User {} accepting invitation", user.getEmail());
        
        Invitation invitation = validateInvitation(token);
        
        // Verify email matches
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Email does not match invitation");
        }
        
        // Check if user already belongs to a corporate
        if (user.getCorporate() != null) {
            throw new IllegalStateException("User already belongs to a corporate");
        }
        
        // Associate user with corporate
        user.setCorporate(invitation.getCorporate());
        user.setInvitedBy(invitation.getInvitedBy());
        user.setInvitationAcceptedAt(LocalDateTime.now());
        
        // Assign roles
        user.getRoles().addAll(invitation.getRoles());
        
        user = userRepository.save(user);
        
        // Update invitation status
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setAcceptedBy(user);
        invitationRepository.save(invitation);
        
        log.info("Invitation accepted by user: {}", user.getEmail());
        
        // Audit log
        auditLogService.log(
            "INVITATION_ACCEPTED",
            "INVITATION",
            invitation.getId(),
            user.getEmail(),
            user,
            "Accepted invitation to " + invitation.getCorporate().getName(),
            null
        );
        
        return user;
    }
    
    public void cancelInvitation(Long invitationId, User admin) {
        log.info("Cancelling invitation ID: {} by user: {}", invitationId, admin.getEmail());
        
        Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        
        // Verify admin belongs to same corporate
        if (!invitation.getCorporate().getId().equals(admin.getCorporate().getId())) {
            throw new IllegalStateException("Cannot cancel invitation from another corporate");
        }
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending invitations");
        }
        
        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
        
        log.info("Invitation cancelled: {}", invitationId);
        
        // Audit log
        auditLogService.log(
            "INVITATION_CANCELLED",
            "INVITATION",
            invitation.getId(),
            invitation.getEmail(),
            admin,
            "Cancelled invitation",
            null
        );
    }
    
    public Invitation resendInvitation(Long invitationId, User admin) {
        log.info("Resending invitation ID: {} by user: {}", invitationId, admin.getEmail());
        
        Invitation oldInvitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        
        // Verify admin belongs to same corporate
        if (!oldInvitation.getCorporate().getId().equals(admin.getCorporate().getId())) {
            throw new IllegalStateException("Cannot resend invitation from another corporate");
        }
        
        // Cancel old invitation
        if (oldInvitation.getStatus() == InvitationStatus.PENDING) {
            oldInvitation.setStatus(InvitationStatus.CANCELLED);
            invitationRepository.save(oldInvitation);
        }
        
        // Create new invitation
        Set<Long> roleIds = oldInvitation.getRoles().stream()
            .map(Role::getId)
            .collect(Collectors.toSet());
        
        return createInvitation(admin, oldInvitation.getEmail(), roleIds);
    }
    
    @Transactional(readOnly = true)
    public List<Invitation> getCorporateInvitations(Long corporateId) {
        log.info("Fetching invitations for corporate ID: {}", corporateId);
        return invitationRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId);
    }
    
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void expireOldInvitations() {
        log.info("Running scheduled task to expire old invitations");
        
        List<Invitation> expiredInvitations = invitationRepository
            .findExpiredInvitations(LocalDateTime.now());
        
        for (Invitation invitation : expiredInvitations) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            
            auditLogService.log(
                "INVITATION_EXPIRED",
                "INVITATION",
                invitation.getId(),
                invitation.getEmail(),
                null,
                "Invitation expired automatically",
                null
            );
        }
        
        log.info("Expired {} invitations", expiredInvitations.size());
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH / 2];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}

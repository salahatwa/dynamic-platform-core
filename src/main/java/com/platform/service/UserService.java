package com.platform.service;

import com.platform.entity.Invitation;
import com.platform.entity.User;
import com.platform.repository.InvitationRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    
    /**
     * Delete a user and handle foreign key constraints properly
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Handle invitations where this user is the inviter (invited_by_id)
        List<Invitation> invitationsByUser = invitationRepository.findByCorporateIdOrderByCreatedAtDesc(user.getCorporate().getId())
                .stream()
                .filter(invitation -> invitation.getInvitedBy() != null && invitation.getInvitedBy().getId().equals(userId))
                .toList();
        
        // Option 1: Set invited_by to null for these invitations
        for (Invitation invitation : invitationsByUser) {
            invitation.setInvitedBy(null);
            invitationRepository.save(invitation);
        }
        
        // Handle invitations where this user accepted the invitation (accepted_by_id)
        List<Invitation> acceptedInvitations = invitationRepository.findByCorporateIdOrderByCreatedAtDesc(user.getCorporate().getId())
                .stream()
                .filter(invitation -> invitation.getAcceptedBy() != null && invitation.getAcceptedBy().getId().equals(userId))
                .toList();
        
        // Set accepted_by to null for these invitations
        for (Invitation acceptedInvitation : acceptedInvitations) {
            acceptedInvitation.setAcceptedBy(null);
            invitationRepository.save(acceptedInvitation);
        }
        
        // Handle users who were invited by this user (invited_by_id in users table)
        List<User> usersInvitedByThisUser = userRepository.findByInvitedById(userId);
        for (User invitedUser : usersInvitedByThisUser) {
            invitedUser.setInvitedBy(null);
            userRepository.save(invitedUser);
        }
        
        // Now we can safely delete the user
        userRepository.deleteById(userId);
    }
    
    /**
     * Soft delete a user by deactivating instead of hard delete
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(false);
        // Optionally add a deleted flag or deleted timestamp
        userRepository.save(user);
    }
    
    /**
     * Check if user can be safely deleted (has no critical dependencies)
     */
    public boolean canDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user has sent invitations
        List<Invitation> sentInvitations = invitationRepository.findByCorporateIdOrderByCreatedAtDesc(user.getCorporate().getId())
                .stream()
                .filter(invitation -> invitation.getInvitedBy() != null && invitation.getInvitedBy().getId().equals(userId))
                .toList();
        
        // Check if user has accepted invitations
        List<Invitation> acceptedInvitations = invitationRepository.findByCorporateIdOrderByCreatedAtDesc(user.getCorporate().getId())
                .stream()
                .filter(invitation -> invitation.getAcceptedBy() != null && invitation.getAcceptedBy().getId().equals(userId))
                .toList();
        
        // Check if user has invited other users
        List<User> invitedUsers = userRepository.findByInvitedById(userId);
        
        // Return true if no dependencies, false if there are dependencies
        return sentInvitations.isEmpty() && acceptedInvitations.isEmpty() && invitedUsers.isEmpty();
    }
    
    /**
     * Get deletion impact information
     */
    public UserDeletionImpact getDeletionImpact(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Invitation> sentInvitations = invitationRepository.findByCorporateIdOrderByCreatedAtDesc(user.getCorporate().getId())
                .stream()
                .filter(invitation -> invitation.getInvitedBy() != null && invitation.getInvitedBy().getId().equals(userId))
                .toList();
        
        List<Invitation> acceptedInvitations = invitationRepository.findByCorporateIdOrderByCreatedAtDesc(user.getCorporate().getId())
                .stream()
                .filter(invitation -> invitation.getAcceptedBy() != null && invitation.getAcceptedBy().getId().equals(userId))
                .toList();
        
        List<User> invitedUsers = userRepository.findByInvitedById(userId);
        
        return UserDeletionImpact.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .sentInvitationsCount(sentInvitations.size())
                .acceptedInvitationsCount(acceptedInvitations.size())
                .invitedUsersCount(invitedUsers.size())
                .canSafelyDelete(sentInvitations.isEmpty() && acceptedInvitations.isEmpty() && invitedUsers.isEmpty())
                .build();
    }
    
    /**
     * Data class for deletion impact information
     */
    public static class UserDeletionImpact {
        private Long userId;
        private String userEmail;
        private int sentInvitationsCount;
        private int acceptedInvitationsCount;
        private int invitedUsersCount;
        private boolean canSafelyDelete;
        
        public static UserDeletionImpactBuilder builder() {
            return new UserDeletionImpactBuilder();
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public String getUserEmail() { return userEmail; }
        public int getSentInvitationsCount() { return sentInvitationsCount; }
        public int getAcceptedInvitationsCount() { return acceptedInvitationsCount; }
        public int getInvitedUsersCount() { return invitedUsersCount; }
        public boolean isCanSafelyDelete() { return canSafelyDelete; }
        
        public static class UserDeletionImpactBuilder {
            private Long userId;
            private String userEmail;
            private int sentInvitationsCount;
            private int acceptedInvitationsCount;
            private int invitedUsersCount;
            private boolean canSafelyDelete;
            
            public UserDeletionImpactBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public UserDeletionImpactBuilder userEmail(String userEmail) {
                this.userEmail = userEmail;
                return this;
            }
            
            public UserDeletionImpactBuilder sentInvitationsCount(int sentInvitationsCount) {
                this.sentInvitationsCount = sentInvitationsCount;
                return this;
            }
            
            public UserDeletionImpactBuilder acceptedInvitationsCount(int acceptedInvitationsCount) {
                this.acceptedInvitationsCount = acceptedInvitationsCount;
                return this;
            }
            
            public UserDeletionImpactBuilder invitedUsersCount(int invitedUsersCount) {
                this.invitedUsersCount = invitedUsersCount;
                return this;
            }
            
            public UserDeletionImpactBuilder canSafelyDelete(boolean canSafelyDelete) {
                this.canSafelyDelete = canSafelyDelete;
                return this;
            }
            
            public UserDeletionImpact build() {
                UserDeletionImpact impact = new UserDeletionImpact();
                impact.userId = this.userId;
                impact.userEmail = this.userEmail;
                impact.sentInvitationsCount = this.sentInvitationsCount;
                impact.acceptedInvitationsCount = this.acceptedInvitationsCount;
                impact.invitedUsersCount = this.invitedUsersCount;
                impact.canSafelyDelete = this.canSafelyDelete;
                return impact;
            }
        }
    }
}
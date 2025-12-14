package com.platform.entity;

import com.platform.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 64)
    private String token;
    
    @Column(nullable = false)
    private String email;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id", nullable = false)
    private Corporate corporate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime acceptedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_id")
    private User acceptedBy;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "invitation_roles",
        joinColumns = @JoinColumn(name = "invitation_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }
    
    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }
}

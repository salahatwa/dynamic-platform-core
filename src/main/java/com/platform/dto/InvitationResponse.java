package com.platform.dto;

import com.platform.entity.Invitation;
import com.platform.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    
    private Long id;
    private String email;
    private String corporateName;
    private String inviterName;
    private Set<String> roles;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private String acceptedByName;
    private LocalDateTime createdAt;
    
    public static InvitationResponse from(Invitation invitation) {
        return InvitationResponse.builder()
            .id(invitation.getId())
            .email(invitation.getEmail())
            .corporateName(invitation.getCorporate().getName())
            .inviterName(invitation.getInvitedBy().getFirstName() + " " + 
                       invitation.getInvitedBy().getLastName())
            .roles(invitation.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()))
            .status(invitation.getStatus().name())
            .expiresAt(invitation.getExpiresAt())
            .acceptedAt(invitation.getAcceptedAt())
            .acceptedByName(invitation.getAcceptedBy() != null ? 
                invitation.getAcceptedBy().getFirstName() + " " + 
                invitation.getAcceptedBy().getLastName() : null)
            .createdAt(invitation.getCreatedAt())
            .build();
    }
}

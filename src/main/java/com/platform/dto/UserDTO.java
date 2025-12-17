package com.platform.dto;

import com.platform.entity.User;
import com.platform.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String firstName;
    private String lastName;
    private AuthProvider provider;
    private String providerId;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime invitationAcceptedAt;
    private List<String> roles;
    private Long invitedById;
    private String invitedByName;
    
    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .invitationAcceptedAt(user.getInvitationAcceptedAt())
                .roles(user.getRoles() != null ? 
                       user.getRoles().stream()
                           .map(role -> role.getName())
                           .collect(Collectors.toList()) : 
                       List.of())
                .invitedById(user.getInvitedBy() != null ? user.getInvitedBy().getId() : null)
                .invitedByName(user.getInvitedBy() != null ? user.getInvitedBy().getName() : null)
                .build();
    }
}
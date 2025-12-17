package com.platform.dto;

import com.platform.entity.Role;
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
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean isSystemRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PermissionDTO> permissions;
    
    public static RoleDTO fromEntity(Role role) {
        if (role == null) {
            return null;
        }
        
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .permissions(role.getPermissions() != null ? 
                           role.getPermissions().stream()
                               .map(PermissionDTO::fromEntity)
                               .collect(Collectors.toList()) : 
                           List.of())
                .build();
    }
}
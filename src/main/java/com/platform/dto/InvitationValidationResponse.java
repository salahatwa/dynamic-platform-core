package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationValidationResponse {
    
    private Boolean valid;
    private String email;
    private String corporateName;
    private String inviterName;
    private Set<String> roles;
    private Boolean userExists;
    private LocalDateTime expiresAt;
    private String errorMessage;
}

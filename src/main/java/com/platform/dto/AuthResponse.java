package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Set<String> permissions;
    private UserInfo user;
    
    @Data
    @Builder
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String name;
        private String firstName;
        private String lastName;
        private Long corporateId;
        private String corporateName;
    }
}

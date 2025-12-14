package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    private Long id;
    private String keyValue;
    private String name;
    private String description;
    private Long appId;
    private String appName;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Boolean active;
    private LocalDateTime createdAt;
}

package com.platform.dto;

import lombok.Data;

@Data
public class ApiKeyRequest {
    private String name;
    private String description;
    private Integer expiryDays; // null = no expiry
    private Long appId; // Required for app-centric API keys
}

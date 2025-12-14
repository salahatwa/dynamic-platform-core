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
public class RecentActivityDto {
    private Long id;
    private String action;
    private String entityType;
    private String entityName;
    private String userName;
    private String userEmail;
    private LocalDateTime timestamp;
    private String details;
    private String ipAddress;
}
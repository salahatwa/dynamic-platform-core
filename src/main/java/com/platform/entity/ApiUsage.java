package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;
    
    @Column(name = "app_id")
    private Long appId;
    
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth; // Format: YYYY-MM
    
    @Column(name = "request_count", nullable = false)
    private Long requestCount = 0L;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

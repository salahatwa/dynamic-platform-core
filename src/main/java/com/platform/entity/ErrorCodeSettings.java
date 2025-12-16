package com.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_code_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"app_id"})
})
@Data
public class ErrorCodeSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private App app;
    
    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix = "E";
    
    @Column(name = "sequence_length", nullable = false)
    private Integer sequenceLength = 6;
    
    @Column(name = "current_sequence", nullable = false)
    private Long currentSequence = 0L;
    
    @Column(name = "separator", length = 5)
    private String separator = "";
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
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
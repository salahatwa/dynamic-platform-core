package com.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config_version")
@Data
public class AppConfigVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_id", nullable = false)
    private Long configId;
    
    @Column(name = "version", nullable = false)
    private Integer version;
    
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private ChangeType changeType;
    
    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;
    
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;
    
    @Column(name = "changed_at")
    private LocalDateTime changedAt = LocalDateTime.now();
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON
    
    public enum ChangeType {
        CREATE,
        UPDATE,
        DELETE
    }
    
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}

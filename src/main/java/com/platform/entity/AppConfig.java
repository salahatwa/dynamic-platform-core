package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config")
@Data
public class AppConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;
    
    @Column(name = "config_name", nullable = false, length = 200)
    private String configName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false, length = 50)
    private ConfigType configType;
    
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;
    
    @Column(name = "enum_values", columnDefinition = "TEXT")
    private String enumValues; // JSON array
    
    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules; // JSON object
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "is_required")
    private Boolean isRequired = false;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @Column(name = "group_id")
    private Long groupId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "corporate", "createdBy"})
    private App app;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum ConfigType {
        TEXT,
        NUMBER,
        BOOLEAN,
        ENUM,
        JSON,
        TEMPLATE,
        LIST
    }
    
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

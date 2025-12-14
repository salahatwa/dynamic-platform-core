package com.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config_audit")
@Data
public class AppConfigAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_id", nullable = false)
    private Long configId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private Action action;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "user_email", length = 255)
    private String userEmail;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public enum Action {
        VIEW,
        CREATE,
        UPDATE,
        DELETE,
        RESTORE
    }
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}

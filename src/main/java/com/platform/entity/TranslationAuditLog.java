package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    @JsonIgnore
    private TranslationApp app;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id")
    @JsonIgnore
    private TranslationKey key;
    
    @Column(length = 10)
    private String language;
    
    @Column(nullable = false, length = 50)
    private String action; // create, update, delete, import, export, rollback
    
    @Column(columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(columnDefinition = "TEXT")
    private String newValue;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    
    @Column(name = "user_email")
    private String userEmail;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lov_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LovAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "lov_id", nullable = false)
    private Long lovId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private Action action;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes; // JSON string
    
    public enum Action {
        CREATE, UPDATE, DELETE, VIEW
    }
}

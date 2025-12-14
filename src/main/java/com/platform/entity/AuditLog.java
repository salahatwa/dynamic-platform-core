package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user", "corporate"})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, GENERATE_PDF, LOGIN, etc.
    
    @Column(nullable = false)
    private String entityType; // TEMPLATE, USER, API_KEY, etc.
    
    private Long entityId;
    
    private String entityName;
    
    @Column(nullable = false)
    private String userName;
    
    @Column(nullable = false)
    private String userEmail;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(length = 1000)
    private String details;
    
    private String ipAddress;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id")
    private Corporate corporate;
    
    // Constructors
    public AuditLog() {}
    
    public AuditLog(String action, String entityType, Long entityId, String entityName, 
                    String userName, String userEmail, String details, String ipAddress) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = entityName;
        this.userName = userName;
        this.userEmail = userEmail;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getEntityName() {
        return entityName;
    }
    
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Corporate getCorporate() {
        return corporate;
    }
    
    public void setCorporate(Corporate corporate) {
        this.corporate = corporate;
    }
}

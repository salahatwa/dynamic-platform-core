package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String keyValue;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Corporate corporate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "corporate", "createdBy"})
    private App app;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime lastUsedAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}

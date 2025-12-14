package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lov_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LovVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "lov_id", nullable = false)
    private Long lovId;
    
    @Column(name = "version", nullable = false)
    private Integer version;
    
    @Column(name = "lov_code", nullable = false, length = 100)
    private String lovCode;
    
    @Column(name = "lov_type", nullable = false, length = 50)
    private String lovType;
    
    @Column(name = "value", nullable = true, length = 500)
    private String value;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    
    @Column(name = "active", nullable = false)
    private Boolean active;
    
    @Column(name = "translation_key", length = 200)
    private String translationKey;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;
    
    @Column(name = "change_description", length = 500)
    private String changeDescription;
    
    public enum ChangeType {
        CREATE, UPDATE, DELETE
    }
}

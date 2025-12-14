package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVersion extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;
    
    @Column(nullable = false)
    private Integer version;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String htmlContent;
    
    @Column(columnDefinition = "TEXT")
    private String cssStyles;
    
    @Column(columnDefinition = "TEXT")
    private String parameterSchema;
    
    @Column(columnDefinition = "TEXT")
    private String changeLog;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}

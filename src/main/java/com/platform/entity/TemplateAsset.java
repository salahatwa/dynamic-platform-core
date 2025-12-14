package com.platform.entity;

import com.platform.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateAsset extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String fileName;
    
    @Enumerated(EnumType.STRING)
    private AssetType assetType;
    
    @Column(nullable = false)
    private String filePath;
    
    private String mimeType;
    
    private Long fileSize;
}

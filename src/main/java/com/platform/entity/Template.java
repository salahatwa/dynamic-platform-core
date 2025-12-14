package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.platform.enums.TemplateType;
import com.platform.enums.PageOrientation;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Template extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private TemplateType type;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String htmlContent;
    
    @Column(columnDefinition = "TEXT")
    private String cssStyles;
    
    @ElementCollection
    @CollectionTable(name = "template_fonts", joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "font_name")
    @Column(name = "font_url")
    @Builder.Default
    private Map<String, String> customFonts = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "template_parameters", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "parameter_name")
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("template")
    @Builder.Default
    private java.util.List<TemplateVersion> versions = new java.util.ArrayList<>();
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("template")
    @Builder.Default
    private java.util.List<TemplateAsset> assets = new java.util.ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id")
    private Corporate corporate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "corporate", "createdBy"})
    private App app;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private TemplateFolder folder;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pageOrder ASC")
    @Builder.Default
    private java.util.List<TemplatePage> pages = new java.util.ArrayList<>();
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<TemplateAttribute> attributes = new java.util.ArrayList<>();
    
    private String subject; // For email templates
    
    @Enumerated(EnumType.STRING)
    @Column(name = "page_orientation")
    @Builder.Default
    private PageOrientation pageOrientation = PageOrientation.PORTRAIT;
}

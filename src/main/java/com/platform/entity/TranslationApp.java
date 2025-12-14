package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "translation_apps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationApp extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "api_key", unique = true, nullable = false)
    private String apiKey;
    
    @Column(name = "default_language", nullable = false)
    @Builder.Default
    private String defaultLanguage = "en";
    
    @Column(name = "supported_languages", columnDefinition = "json")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String supportedLanguages; // JSON array: ["en", "ar", "fr"]
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id", nullable = false)
    @JsonIgnore
    private Corporate corporate;
    
    @OneToMany(mappedBy = "app", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TranslationKey> keys = new ArrayList<>();
    
    @OneToMany(mappedBy = "app", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TranslationVersion> versions = new ArrayList<>();
    
    // Helper method to parse supported languages
    public Set<String> getSupportedLanguagesSet() {
        Set<String> languages = new HashSet<>();
        if (supportedLanguages != null && !supportedLanguages.isEmpty()) {
            // Parse JSON array
            String cleaned = supportedLanguages.replace("[", "").replace("]", "").replace("\"", "");
            for (String lang : cleaned.split(",")) {
                languages.add(lang.trim());
            }
        }
        return languages;
    }
    
    // Helper method to set supported languages
    public void setSupportedLanguagesSet(Set<String> languages) {
        if (languages == null || languages.isEmpty()) {
            this.supportedLanguages = "[]";
        } else {
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (String lang : languages) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(lang).append("\"");
                i++;
            }
            sb.append("]");
            this.supportedLanguages = sb.toString();
        }
    }
}

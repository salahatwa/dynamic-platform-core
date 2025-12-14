package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.enums.TranslationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "translations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id", nullable = false)
    @JsonIgnore
    private TranslationKey key;
    
    @Column(nullable = false, length = 10)
    private String language;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TranslationStatus status = TranslationStatus.PUBLISHED;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @JsonIgnore
    private User updatedBy;
}

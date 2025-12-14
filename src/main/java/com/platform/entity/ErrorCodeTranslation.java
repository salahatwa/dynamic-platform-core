package com.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_code_translation")
@Data
public class ErrorCodeTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_code_id", nullable = false)
    private ErrorCode errorCode;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "technical_details", columnDefinition = "TEXT")
    private String technicalDetails;

    @Column(name = "resolution_steps", columnDefinition = "TEXT")
    private String resolutionSteps;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

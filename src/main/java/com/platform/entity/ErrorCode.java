package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "error_code")
@Data
public class ErrorCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ErrorCodeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "corporate", "createdBy"})
    private App app;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ErrorSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ErrorStatus status = ErrorStatus.ACTIVE;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "is_retryable")
    private Boolean isRetryable = false;

    @Column(name = "default_message", nullable = false, columnDefinition = "TEXT")
    private String defaultMessage;

    @Column(name = "technical_details", columnDefinition = "TEXT")
    private String technicalDetails;

    @Column(name = "resolution_steps", columnDefinition = "TEXT")
    private String resolutionSteps;

    @Column(name = "documentation_url", length = 500)
    private String documentationUrl;

    @Column(name = "corporate_id")
    private Long corporateId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "errorCode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErrorCodeTranslation> translations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ErrorSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }

    public enum ErrorStatus {
        ACTIVE, DEPRECATED, REMOVED
    }
}

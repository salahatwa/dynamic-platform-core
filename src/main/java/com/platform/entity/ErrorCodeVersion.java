package com.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_code_version")
@Data
public class ErrorCodeVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_code_id", nullable = false)
    private Long errorCodeId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_retryable")
    private Boolean isRetryable;

    @Column(name = "default_message", nullable = false, columnDefinition = "TEXT")
    private String defaultMessage;

    @Column(name = "technical_details", columnDefinition = "TEXT")
    private String technicalDetails;

    @Column(name = "resolution_steps", columnDefinition = "TEXT")
    private String resolutionSteps;

    @Column(name = "documentation_url", length = 500)
    private String documentationUrl;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

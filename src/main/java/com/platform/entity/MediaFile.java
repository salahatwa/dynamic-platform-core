package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MediaFile extends BaseEntity {

    @Column(nullable = false)
    private String filename;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_hash")
    private String fileHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private MediaFolder folder;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private MediaProviderType providerType;

    @Column(name = "provider_key")
    private String providerKey;

    @Column(name = "provider_url")
    private String providerUrl;

    @Column(name = "public_url")
    private String publicUrl;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "app_id")
    private Long appId;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MediaStatus status = MediaStatus.ACTIVE;

    @Column(name = "upload_session_id")
    private String uploadSessionId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Enums
    public enum MediaProviderType {
        LOCAL("Local File System"),
        CLOUDFLARE_R2("Cloudflare R2"),
        AWS_S3("Amazon S3"),
        GOOGLE_DRIVE("Google Drive"),
        DROPBOX("Dropbox"),
        AZURE_BLOB("Azure Blob Storage");

        private final String displayName;

        MediaProviderType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum MediaStatus {
        UPLOADING,
        ACTIVE,
        PROCESSING,
        ARCHIVED,
        DELETED,
        ERROR
    }

    // Helper methods
    public String getFileExtension() {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    public boolean isImage() {
        String ext = getFileExtension();
        return ext.matches("jpg|jpeg|png|gif|bmp|webp|svg");
    }

    public boolean isVideo() {
        String ext = getFileExtension();
        return ext.matches("mp4|avi|mov|wmv|flv|webm|mkv");
    }

    public boolean isDocument() {
        String ext = getFileExtension();
        return ext.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf");
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
package com.platform.media.model;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaUrl {
    private String url;
    private AccessType accessType;
    private LocalDateTime expiresAt;
    private boolean isTemporary;
}
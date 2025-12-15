package com.platform.dto;

import lombok.Data;

@Data
public class UpdateFolderRequest {
    private String name;
    private Long parentId;
    private Boolean isPublic;
    private String description;
}
package com.platform.dto;

import lombok.Data;

@Data
public class CreateFolderRequest {
    private String name;
    private Long parentId;
    private Boolean isPublic = false;
    private String description;
}
package com.platform.dto;

import lombok.Data;

@Data
public class MoveFolderRequest {
    private Long targetParentId;
    private Integer position;
}
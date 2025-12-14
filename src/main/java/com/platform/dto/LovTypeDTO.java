package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LovTypeDTO {
    private String code;
    private String name;
    private String description;
    private Boolean allowHierarchy;
    private Long count;
}

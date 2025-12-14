package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LovContentResponse {
    private Long id;
    private String lovName;
    private String description;
    private Boolean isActive;
    private List<LovValueData> values;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LovValueData {
        private Long id;
        private String value;
        private String displayValue;
        private String description;
        private Integer sortOrder;
        private Boolean isActive;
    }
}
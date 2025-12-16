package com.platform.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorCodeGenerationResponse {
    private String generatedCode;
    private Long sequenceNumber;
    private String prefix;
    private String separator;
    private Integer sequenceLength;
}
package com.platform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BulkTranslationRequest {
    private Long appId;
    private String language;
    private Map<String, String> translations; // keyName -> value
    private List<TranslationRequest> items;
}

package com.platform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PdfOptions {
    
    @Builder.Default
    private String pageSize = "A4";
    
    @Builder.Default
    private String orientation = "Portrait";
    
    @Builder.Default
    private int marginTop = 20;
    
    @Builder.Default
    private int marginBottom = 20;
    
    @Builder.Default
    private int marginLeft = 20;
    
    @Builder.Default
    private int marginRight = 20;
    
    @Builder.Default
    private boolean enableJavaScript = true;
    
    @Builder.Default
    private int timeout = 60000;
    
    private String headerHtml;
    private String footerHtml;
}
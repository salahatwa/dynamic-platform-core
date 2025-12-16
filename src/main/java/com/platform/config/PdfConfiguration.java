package com.platform.config;

import com.platform.service.PdfGenerationService;
import com.platform.service.TemplateRenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@Slf4j
public class PdfConfiguration {

    /**
     * Wire PdfGenerationService to TemplateRenderService after context is refreshed
     * to avoid circular dependency issues
     */
    @EventListener(ContextRefreshedEvent.class)
    public void configurePdfServices(ContextRefreshedEvent event) {
        try {
            PdfGenerationService pdfGenerationService = event.getApplicationContext()
                .getBean(PdfGenerationService.class);
            TemplateRenderService templateRenderService = event.getApplicationContext()
                .getBean(TemplateRenderService.class);
            
            templateRenderService.setPdfGenerationService(pdfGenerationService);
            
            log.info("✅ PDF services configured successfully");
        } catch (Exception e) {
            log.warn("⚠️ Failed to configure PDF services: {}", e.getMessage());
        }
    }
}
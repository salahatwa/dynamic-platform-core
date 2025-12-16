package com.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive PDF Generation Service with multiple engines and fallback logic
 * 
 * Priority Order:
 * 1. IronPDF (Premium, best quality, commercial license)
 * 2. Playwright (High quality, modern CSS support, free)
 * 3. Flying Saucer (Basic quality, reliable fallback, free)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private final IronPdfService ironPdfService;
    private final PlaywrightPdfService playwrightPdfService;
    private final FlyingSaucerPdfService flyingSaucerPdfService;
    private final TemplateRenderService templateRenderService;

    @Value("${pdf.engine:auto}")
    private String preferredEngine;

    public enum PdfEngine {
        IRON_PDF("ironpdf", "IronPDF"),
        PLAYWRIGHT("playwright", "Playwright"),
        FLYING_SAUCER("flyingsaucer", "Flying Saucer"),
        AUTO("auto", "Auto (Best Available)");

        private final String code;
        private final String displayName;

        PdfEngine(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }

        public static PdfEngine fromCode(String code) {
            return Arrays.stream(values())
                .filter(engine -> engine.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(AUTO);
        }
    }

    /**
     * Generate PDF with automatic engine selection and fallback
     */
    public byte[] generatePdf(Long templateId, java.util.Map<String, Object> parameters) {
        return generatePdf(templateId, parameters, null, com.platform.enums.PageOrientation.PORTRAIT);
    }

    /**
     * Generate PDF with specific page number
     */
    public byte[] generatePdf(Long templateId, java.util.Map<String, Object> parameters, Integer pageNumber) {
        return generatePdf(templateId, parameters, pageNumber, com.platform.enums.PageOrientation.PORTRAIT);
    }

    /**
     * Generate PDF with full options
     */
    public byte[] generatePdf(Long templateId, java.util.Map<String, Object> parameters, 
                             Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        
        // Store template ID in thread-local for context
        currentTemplateId.set(templateId);
        
        try {
            // First, render HTML content
            String html = renderHtmlContent(templateId, parameters, pageNumber);
            
            // Enhance HTML for PDF generation
            String enhancedHtml = enhanceHtmlForPdf(html, templateId, orientation);
            
            // Determine engine order based on configuration
            List<PdfEngine> engineOrder = determineEngineOrder();
            
            log.info("🔄 Starting PDF generation for template {} with engines: {}", 
                templateId, engineOrder.stream().map(PdfEngine::getDisplayName).toList());

            Exception lastException = null;
            
            // Try each engine in order
            for (PdfEngine engine : engineOrder) {
                try {
                    byte[] pdf = generateWithEngine(engine, enhancedHtml, pageNumber, orientation);
                    if (pdf != null && pdf.length > 0) {
                        log.info("✅ PDF generated successfully with {} - Size: {} bytes, Pages: {}", 
                            engine.getDisplayName(), pdf.length, pageNumber != null ? "page " + pageNumber : "all");
                        return pdf;
                    }
                } catch (Exception e) {
                    lastException = e;
                    log.warn("⚠️ {} failed: {} - Trying next engine...", engine.getDisplayName(), e.getMessage());
                }
            }

            // All engines failed
            String errorMsg = "All PDF engines failed. Last error: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error");
            log.error("❌ PDF generation completely failed for template {}: {}", templateId, errorMsg);
            throw new RuntimeException(errorMsg, lastException);
        } finally {
            // Clean up thread-local
            currentTemplateId.remove();
        }
    }

    // Thread-local to store current template ID for context
    private final ThreadLocal<Long> currentTemplateId = new ThreadLocal<>();

    /**
     * Generate PDF using specific engine (for testing/debugging)
     */
    public byte[] generatePdfWithEngine(PdfEngine engine, Long templateId, 
                                       java.util.Map<String, Object> parameters, 
                                       Integer pageNumber, 
                                       com.platform.enums.PageOrientation orientation) {
        
        // Store template ID in thread-local for context
        currentTemplateId.set(templateId);
        
        try {
            String html = renderHtmlContent(templateId, parameters, pageNumber);
            String enhancedHtml = enhanceHtmlForPdf(html, templateId, orientation);
            
            return generateWithEngine(engine, enhancedHtml, pageNumber, orientation);
        } finally {
            // Clean up thread-local
            currentTemplateId.remove();
        }
    }

    /**
     * Get available PDF engines status
     */
    public java.util.Map<String, Object> getEngineStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        status.put("ironpdf", java.util.Map.of(
            "available", ironPdfService.isAvailable(),
            "description", "Premium PDF engine with best quality",
            "priority", 1
        ));
        
        status.put("playwright", java.util.Map.of(
            "available", playwrightPdfService.isAvailable(),
            "description", "Modern PDF engine with full CSS support",
            "priority", 2
        ));
        
        status.put("flyingsaucer", java.util.Map.of(
            "available", flyingSaucerPdfService.isAvailable(),
            "description", "Reliable fallback PDF engine",
            "priority", 3
        ));
        
        status.put("preferredEngine", preferredEngine);
        status.put("engineOrder", determineEngineOrder().stream()
            .map(PdfEngine::getDisplayName).toList());
        
        return status;
    }

    private String renderHtmlContent(Long templateId, java.util.Map<String, Object> parameters, Integer pageNumber) {
        try {
            if (pageNumber != null) {
                return templateRenderService.renderSpecificPage(templateId, pageNumber, parameters);
            } else {
                return templateRenderService.renderHtml(templateId, parameters);
            }
        } catch (Exception e) {
            log.error("❌ Failed to render HTML for template {}: {}", templateId, e.getMessage());
            throw new RuntimeException("Failed to render template HTML", e);
        }
    }

    private String enhanceHtmlForPdf(String html, Long templateId, com.platform.enums.PageOrientation orientation) {
        try {
            // Get template for CSS styles
            com.platform.entity.Template template = templateRenderService.templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            // Use existing enhancement logic from TemplateRenderService
            return templateRenderService.enhanceHtmlForPdf(html, template.getCssStyles(), orientation);
        } catch (Exception e) {
            log.warn("⚠️ Failed to enhance HTML, using original: {}", e.getMessage());
            return html;
        }
    }

    private List<PdfEngine> determineEngineOrder() {
        PdfEngine preferred = PdfEngine.fromCode(preferredEngine);
        
        if (preferred != PdfEngine.AUTO) {
            // Use specific engine first, then fallbacks
            return switch (preferred) {
                case IRON_PDF -> Arrays.asList(PdfEngine.IRON_PDF, PdfEngine.PLAYWRIGHT, PdfEngine.FLYING_SAUCER);
                case PLAYWRIGHT -> Arrays.asList(PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF, PdfEngine.FLYING_SAUCER);
                case FLYING_SAUCER -> Arrays.asList(PdfEngine.FLYING_SAUCER, PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF);
                default -> getAutoEngineOrder();
            };
        }
        
        return getAutoEngineOrder();
    }

    private List<PdfEngine> getAutoEngineOrder() {
        // Auto mode: Use best available engine first
        return Arrays.asList(PdfEngine.IRON_PDF, PdfEngine.PLAYWRIGHT, PdfEngine.FLYING_SAUCER);
    }

    private byte[] generateWithEngine(PdfEngine engine, String html, Integer pageNumber, 
                                     com.platform.enums.PageOrientation orientation) {
        return switch (engine) {
            case IRON_PDF -> {
                if (!ironPdfService.isAvailable()) {
                    throw new RuntimeException("IronPDF is not available");
                }
                yield ironPdfService.generatePdfFromHtml(html, pageNumber, orientation);
            }
            case PLAYWRIGHT -> {
                if (!playwrightPdfService.isAvailable()) {
                    throw new RuntimeException("Playwright is not available");
                }
                yield playwrightPdfService.generatePdfFromHtml(html, pageNumber, orientation);
            }
            case FLYING_SAUCER -> {
                if (!flyingSaucerPdfService.isAvailable()) {
                    throw new RuntimeException("Flying Saucer is not available");
                }
                // Extract template ID from the HTML rendering context if needed
                Long templateId = extractTemplateIdFromContext();
                yield flyingSaucerPdfService.generatePdfFromHtml(html, pageNumber, orientation, templateId);
            }
            default -> throw new RuntimeException("Unsupported PDF engine: " + engine);
        };
    }

    private Long extractTemplateIdFromContext() {
        return currentTemplateId.get();
    }
}
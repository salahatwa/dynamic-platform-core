package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive PDF Generation Service with multiple engines and fallback logic
 * 
 * Priority Order:
 * 1. IronPDF (Premium, best quality, commercial license)
 * 2. Playwright (High quality, modern CSS support, free)
 * 3. Flying Saucer (Basic quality, reliable fallback, free)
 */
@Service
@Slf4j
public class PdfGenerationService {

    private final IronPdfService ironPdfService;
    private final PlaywrightPdfService playwrightPdfService;
    private final GotenbergPdfService gotenbergPdfService;
    private final FlyingSaucerPdfService flyingSaucerPdfService;
    private final TemplateRenderService templateRenderService;

    // Constructor with optional PDF services
    public PdfGenerationService(
            @Autowired(required = false) IronPdfService ironPdfService,
            @Autowired(required = false) PlaywrightPdfService playwrightPdfService,
            @Autowired(required = false) GotenbergPdfService gotenbergPdfService,
            FlyingSaucerPdfService flyingSaucerPdfService,
            TemplateRenderService templateRenderService) {
        this.ironPdfService = ironPdfService;
        this.playwrightPdfService = playwrightPdfService;
        this.gotenbergPdfService = gotenbergPdfService;
        this.flyingSaucerPdfService = flyingSaucerPdfService;
        this.templateRenderService = templateRenderService;
        
        // Log available services
        StringBuilder availableServices = new StringBuilder("📊 Available PDF engines: ");
        if (ironPdfService != null) {
            availableServices.append("IronPDF ");
        }
        if (playwrightPdfService != null) {
            availableServices.append("Playwright ");
        }
        if (gotenbergPdfService != null) {
            availableServices.append("Gotenberg ");
        }
        availableServices.append("Flying Saucer");
        
        log.info(availableServices.toString());
        
        if (ironPdfService == null && playwrightPdfService == null && gotenbergPdfService == null) {
            log.warn("⚠️ Only Flying Saucer PDF engine available - limited PDF quality");
        }
    }

    @Value("${pdf.engine:auto}")
    private String preferredEngine;

    public enum PdfEngine {
        IRON_PDF("ironpdf", "IronPDF"),
        PLAYWRIGHT("playwright", "Playwright"),
        GOTENBERG("gotenberg", "Gotenberg"),
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
            "available", ironPdfService != null && ironPdfService.isAvailable(),
            "description", ironPdfService != null ? "Premium PDF engine with best quality" : "IronPDF service disabled",
            "priority", 1
        ));
        
        status.put("playwright", java.util.Map.of(
            "available", playwrightPdfService != null && playwrightPdfService.isAvailable(),
            "description", playwrightPdfService != null ? "Modern PDF engine with full CSS support" : "Playwright service disabled",
            "priority", 2
        ));
        
        status.put("gotenberg", java.util.Map.of(
            "available", gotenbergPdfService != null && gotenbergPdfService.isAvailable(),
            "description", gotenbergPdfService != null ? "Free Docker-based PDF engine with high quality" : "Gotenberg service disabled",
            "priority", 3
        ));
        
        status.put("flyingsaucer", java.util.Map.of(
            "available", flyingSaucerPdfService.isAvailable(),
            "description", "Reliable fallback PDF engine",
            "priority", 4
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
                case IRON_PDF -> Arrays.asList(PdfEngine.IRON_PDF, PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.FLYING_SAUCER);
                case PLAYWRIGHT -> Arrays.asList(PdfEngine.PLAYWRIGHT, PdfEngine.GOTENBERG, PdfEngine.IRON_PDF, PdfEngine.FLYING_SAUCER);
                case GOTENBERG -> Arrays.asList(PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF, PdfEngine.FLYING_SAUCER);
                case FLYING_SAUCER -> Arrays.asList(PdfEngine.FLYING_SAUCER, PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF);
                default -> getAutoEngineOrder();
            };
        }
        
        return getAutoEngineOrder();
    }

    private List<PdfEngine> getAutoEngineOrder() {
        // Configurable engine priority based on PDF_ENGINE setting
        String preferredEngineStr = preferredEngine.toLowerCase();
        
        switch (preferredEngineStr) {
            case "ironpdf":
                // IronPDF first, then Gotenberg, then Playwright, then Flying Saucer
                return buildEngineOrder(PdfEngine.IRON_PDF, PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.FLYING_SAUCER);
                
            case "playwright":
                // Playwright first, then Gotenberg, then IronPDF, then Flying Saucer
                return buildEngineOrder(PdfEngine.PLAYWRIGHT, PdfEngine.GOTENBERG, PdfEngine.IRON_PDF, PdfEngine.FLYING_SAUCER);
                
            case "gotenberg":
                // Gotenberg first, then Playwright, then IronPDF, then Flying Saucer
                return buildEngineOrder(PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF, PdfEngine.FLYING_SAUCER);
                
            case "flyingsaucer":
                // Flying Saucer first, then others
                return buildEngineOrder(PdfEngine.FLYING_SAUCER, PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF);
                
            default: // "auto"
                // Auto mode: Use best available free engine first (Gotenberg > Playwright > Flying Saucer, IronPDF if licensed)
                return buildEngineOrder(PdfEngine.GOTENBERG, PdfEngine.PLAYWRIGHT, PdfEngine.IRON_PDF, PdfEngine.FLYING_SAUCER);
        }
    }
    
    private List<PdfEngine> buildEngineOrder(PdfEngine first, PdfEngine second, PdfEngine third, PdfEngine fourth) {
        List<PdfEngine> engines = new ArrayList<>();
        
        // Add engines only if their services are available
        if (isEngineServiceAvailable(first)) {
            engines.add(first);
        }
        if (isEngineServiceAvailable(second)) {
            engines.add(second);
        }
        if (isEngineServiceAvailable(third)) {
            engines.add(third);
        }
        if (isEngineServiceAvailable(fourth)) {
            engines.add(fourth);
        }
        
        // Always include Flying Saucer as final fallback if not already included
        if (!engines.contains(PdfEngine.FLYING_SAUCER)) {
            engines.add(PdfEngine.FLYING_SAUCER);
        }
        
        return engines;
    }
    
    private boolean isEngineServiceAvailable(PdfEngine engine) {
        return switch (engine) {
            case IRON_PDF -> ironPdfService != null && ironPdfService.isAvailable();
            case PLAYWRIGHT -> playwrightPdfService != null && playwrightPdfService.isAvailable();
            case GOTENBERG -> gotenbergPdfService != null && gotenbergPdfService.isAvailable();
            case FLYING_SAUCER -> flyingSaucerPdfService != null && flyingSaucerPdfService.isAvailable();
            default -> false;
        };
    }

    private byte[] generateWithEngine(PdfEngine engine, String html, Integer pageNumber, 
                                     com.platform.enums.PageOrientation orientation) {
        return switch (engine) {
            case IRON_PDF -> {
                if (ironPdfService == null || !ironPdfService.isAvailable()) {
                    throw new RuntimeException("IronPDF is not available (service disabled or not initialized)");
                }
                yield ironPdfService.generatePdfFromHtml(html, pageNumber, orientation);
            }
            case PLAYWRIGHT -> {
                if (playwrightPdfService == null || !playwrightPdfService.isAvailable()) {
                    throw new RuntimeException("Playwright is not available (service disabled or not initialized)");
                }
                yield playwrightPdfService.generatePdfFromHtml(html, pageNumber, orientation);
            }
            case GOTENBERG -> {
                if (gotenbergPdfService == null || !gotenbergPdfService.isAvailable()) {
                    throw new RuntimeException("Gotenberg is not available (service disabled or not initialized)");
                }
                yield gotenbergPdfService.generatePdfFromHtml(html, pageNumber, orientation);
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

    /**
     * Generate PDF directly from HTML content (for testing purposes)
     */
    public byte[] generatePdfFromHtml(String html, Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        try {
            log.info("🔄 Generating PDF directly from HTML - Page: {}, Orientation: {}", 
                pageNumber != null ? pageNumber : "all", orientation);

            // Enhance HTML for PDF generation
            String enhancedHtml = enhanceHtmlForPdfDirect(html, orientation);
            
            // Determine engine order based on configuration
            List<PdfEngine> engineOrder = determineEngineOrder();
            
            Exception lastException = null;
            
            // Try each engine in order
            for (PdfEngine engine : engineOrder) {
                try {
                    byte[] pdf = generateWithEngine(engine, enhancedHtml, pageNumber, orientation);
                    if (pdf != null && pdf.length > 0) {
                        log.info("✅ PDF generated successfully with {} - Size: {} bytes", 
                            engine.getDisplayName(), pdf.length);
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
            log.error("❌ PDF generation completely failed: {}", errorMsg);
            throw new RuntimeException(errorMsg, lastException);
            
        } catch (Exception e) {
            log.error("❌ Direct PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PDF directly from HTML with specific engine
     */
    public byte[] generatePdfFromHtmlWithEngine(PdfEngine engine, String html, Integer pageNumber, 
                                              com.platform.enums.PageOrientation orientation) {
        try {
            log.info("🔄 Generating PDF directly with {} - HTML length: {}", engine.getDisplayName(), html.length());
            
            String enhancedHtml = enhanceHtmlForPdfDirect(html, orientation);
            return generateWithEngine(engine, enhancedHtml, pageNumber, orientation);
            
        } catch (Exception e) {
            log.error("❌ Direct PDF generation with {} failed: {}", engine.getDisplayName(), e.getMessage(), e);
            throw new RuntimeException("PDF generation with " + engine.getDisplayName() + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * Enhance HTML for PDF generation (direct mode - no template context)
     */
    private String enhanceHtmlForPdfDirect(String html, com.platform.enums.PageOrientation orientation) {
        try {
            // Add basic PDF-friendly CSS if not present
            if (!html.toLowerCase().contains("<style>") && !html.toLowerCase().contains("stylesheet")) {
                String pdfCss = """
                    <style>
                        @page {
                            size: %s;
                            margin: 20mm;
                        }
                        body {
                            font-family: Arial, sans-serif;
                            font-size: 12pt;
                            line-height: 1.4;
                            color: #333;
                            margin: 0;
                            padding: 20px;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            color: #333;
                            margin-top: 0;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                        }
                        th, td {
                            border: 1px solid #ddd;
                            padding: 8px;
                            text-align: left;
                        }
                        th {
                            background-color: #f2f2f2;
                        }
                        @media print {
                            body { margin: 0; }
                            .no-print { display: none; }
                        }
                    </style>
                    """.formatted(orientation.isLandscape() ? "A4 landscape" : "A4 portrait");
                
                // Insert CSS after <head> tag or at the beginning
                if (html.toLowerCase().contains("<head>")) {
                    html = html.replaceFirst("(?i)<head>", "<head>" + pdfCss);
                } else if (html.toLowerCase().contains("<html>")) {
                    html = html.replaceFirst("(?i)<html>", "<html><head>" + pdfCss + "</head>");
                } else {
                    html = "<html><head>" + pdfCss + "</head><body>" + html + "</body></html>";
                }
            }
            
            return html;
        } catch (Exception e) {
            log.warn("⚠️ Failed to enhance HTML for PDF, using original: {}", e.getMessage());
            return html;
        }
    }
}
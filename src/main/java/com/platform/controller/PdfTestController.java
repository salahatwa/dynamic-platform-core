package com.platform.controller;

import com.platform.service.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Public PDF Test Controller for testing PDF generation engines
 * 
 * This controller is excluded from security configuration for testing purposes.
 * It allows testing different PDF engines (Gotenberg, Playwright, IronPDF, Flying Saucer)
 * without authentication.
 */
@RestController
@RequestMapping("/api/public/pdf-test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDF Test", description = "Public PDF generation testing endpoints")
public class PdfTestController {

    private final PdfGenerationService pdfGenerationService;

    @PostMapping("/generate")
    @Operation(
        summary = "Generate PDF from HTML using specified engine",
        description = "Test PDF generation with different engines. Supports Gotenberg, Playwright, IronPDF, Flying Saucer, and auto selection."
    )
    public ResponseEntity<byte[]> generatePdf(
            @Parameter(description = "PDF engine to use", example = "gotenberg")
            @RequestParam(defaultValue = "auto") String engine,
            
            @Parameter(description = "HTML content to convert to PDF", required = true)
            @RequestBody String html,
            
            @Parameter(description = "Page orientation", example = "PORTRAIT")
            @RequestParam(defaultValue = "PORTRAIT") String orientation,
            
            @Parameter(description = "Specific page number (optional)")
            @RequestParam(required = false) Integer pageNumber) {

        try {
            log.info("🔄 PDF Test Request - Engine: {}, Orientation: {}, Page: {}, HTML length: {}", 
                engine, orientation, pageNumber, html != null ? html.length() : 0);

            // Validate HTML content
            if (html == null || html.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("HTML content is required".getBytes());
            }

            // Parse orientation
            com.platform.enums.PageOrientation pageOrientation;
            try {
                pageOrientation = com.platform.enums.PageOrientation.valueOf(orientation.toUpperCase());
            } catch (IllegalArgumentException e) {
                pageOrientation = com.platform.enums.PageOrientation.PORTRAIT;
            }

            // Generate PDF directly from HTML (bypass template system)
            byte[] pdf;
            if ("auto".equalsIgnoreCase(engine)) {
                // Use auto engine selection
                pdf = generatePdfDirectly(html, pageNumber, pageOrientation);
            } else {
                // Use specific engine
                PdfGenerationService.PdfEngine pdfEngine;
                try {
                    pdfEngine = PdfGenerationService.PdfEngine.fromCode(engine.toLowerCase());
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(("Invalid engine: " + engine + ". Supported: gotenberg, playwright, ironpdf, flyingsaucer, auto").getBytes());
                }
                
                pdf = generatePdfDirectlyWithEngine(pdfEngine, html, pageNumber, pageOrientation);
            }

            // Return PDF with appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "test-" + engine + ".pdf");
            headers.setContentLength(pdf.length);

            log.info("✅ PDF Test Success - Engine: {}, Size: {} bytes", engine, pdf.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);

        } catch (Exception e) {
            log.error("❌ PDF Test Failed - Engine: {}, Error: {}", engine, e.getMessage(), e);
            
            String errorMessage = "PDF generation failed with engine '" + engine + "': " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(errorMessage.getBytes());
        }
    }

    @PostMapping("/generate-with-template")
    @Operation(
        summary = "Generate PDF from HTML template with parameters",
        description = "Test PDF generation with HTML template and custom parameters"
    )
    public ResponseEntity<byte[]> generatePdfWithTemplate(
            @Parameter(description = "PDF engine to use", example = "gotenberg")
            @RequestParam(defaultValue = "auto") String engine,
            
            @RequestBody PdfTestRequest request) {

        try {
            log.info("🔄 PDF Template Test - Engine: {}, Template length: {}, Parameters: {}", 
                engine, request.getHtml() != null ? request.getHtml().length() : 0, request.getParameters().size());

            // Validate request
            if (request.getHtml() == null || request.getHtml().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("HTML content is required".getBytes());
            }

            // Parse orientation
            com.platform.enums.PageOrientation pageOrientation;
            try {
                pageOrientation = com.platform.enums.PageOrientation.valueOf(
                    request.getOrientation() != null ? request.getOrientation().toUpperCase() : "PORTRAIT");
            } catch (IllegalArgumentException e) {
                pageOrientation = com.platform.enums.PageOrientation.PORTRAIT;
            }

            // Create parameters map
            Map<String, Object> parameters = new HashMap<>(request.getParameters());
            parameters.put("testHtml", request.getHtml());

            // Process template with parameters
            String processedHtml = processTemplate(request.getHtml(), parameters);

            // Generate PDF directly from processed HTML
            byte[] pdf;
            if ("auto".equalsIgnoreCase(engine)) {
                pdf = generatePdfDirectly(processedHtml, request.getPageNumber(), pageOrientation);
            } else {
                PdfGenerationService.PdfEngine pdfEngine = PdfGenerationService.PdfEngine.fromCode(engine.toLowerCase());
                pdf = generatePdfDirectlyWithEngine(pdfEngine, processedHtml, request.getPageNumber(), pageOrientation);
            }

            // Return PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "template-test-" + engine + ".pdf");
            headers.setContentLength(pdf.length);

            log.info("✅ PDF Template Test Success - Engine: {}, Size: {} bytes", engine, pdf.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);

        } catch (Exception e) {
            log.error("❌ PDF Template Test Failed - Engine: {}, Error: {}", engine, e.getMessage(), e);
            
            String errorMessage = "PDF template generation failed with engine '" + engine + "': " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(errorMessage.getBytes());
        }
    }

    @GetMapping("/engines/status")
    @Operation(
        summary = "Get PDF engines status",
        description = "Check which PDF engines are available and their status"
    )
    public ResponseEntity<Map<String, Object>> getEnginesStatus() {
        try {
            Map<String, Object> status = pdfGenerationService.getEngineStatus();
            log.info("📊 PDF Engines Status requested");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Failed to get engines status: {}", e.getMessage(), e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("error", "Failed to get engines status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStatus);
        }
    }

    @GetMapping("/sample-html")
    @Operation(
        summary = "Get sample HTML for testing",
        description = "Returns sample HTML content that can be used for PDF generation testing"
    )
    public ResponseEntity<String> getSampleHtml() {
        String sampleHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>PDF Test Document</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 40px;
                        line-height: 1.6;
                        color: #333;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 2px solid #007bff;
                        padding-bottom: 20px;
                        margin-bottom: 30px;
                    }
                    .content {
                        margin: 20px 0;
                    }
                    .highlight {
                        background-color: #f8f9fa;
                        padding: 15px;
                        border-left: 4px solid #007bff;
                        margin: 20px 0;
                    }
                    .footer {
                        margin-top: 50px;
                        text-align: center;
                        font-size: 12px;
                        color: #666;
                    }
                    @media print {
                        body { margin: 20px; }
                        .no-print { display: none; }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>PDF Generation Test</h1>
                    <p>Testing PDF engines: Gotenberg, Playwright, IronPDF, Flying Saucer</p>
                </div>
                
                <div class="content">
                    <h2>Test Content</h2>
                    <p>This is a sample document to test PDF generation capabilities.</p>
                    
                    <div class="highlight">
                        <h3>Features Tested:</h3>
                        <ul>
                            <li>HTML to PDF conversion</li>
                            <li>CSS styling support</li>
                            <li>Font rendering</li>
                            <li>Layout and formatting</li>
                            <li>Print media queries</li>
                        </ul>
                    </div>
                    
                    <h3>Engine Comparison</h3>
                    <table border="1" style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                        <thead>
                            <tr style="background-color: #f8f9fa;">
                                <th style="padding: 10px;">Engine</th>
                                <th style="padding: 10px;">Quality</th>
                                <th style="padding: 10px;">Cost</th>
                                <th style="padding: 10px;">CSS Support</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td style="padding: 8px;">Gotenberg</td>
                                <td style="padding: 8px;">High</td>
                                <td style="padding: 8px;">Free</td>
                                <td style="padding: 8px;">Full</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px;">Playwright</td>
                                <td style="padding: 8px;">High</td>
                                <td style="padding: 8px;">Free</td>
                                <td style="padding: 8px;">Full</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px;">IronPDF</td>
                                <td style="padding: 8px;">Premium</td>
                                <td style="padding: 8px;">Paid</td>
                                <td style="padding: 8px;">Full</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px;">Flying Saucer</td>
                                <td style="padding: 8px;">Basic</td>
                                <td style="padding: 8px;">Free</td>
                                <td style="padding: 8px;">Limited</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                
                <div class="footer">
                    <p>Generated on: <span id="timestamp">{{timestamp}}</span></p>
                    <p>PDF Engine Test - Dynamic Platform</p>
                </div>
                
                <script>
                    document.getElementById('timestamp').textContent = new Date().toLocaleString();
                </script>
            </body>
            </html>
            """;

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(sampleHtml);
    }

    /**
     * Create test parameters for PDF generation
     */
    private Map<String, Object> createTestParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timestamp", java.time.LocalDateTime.now().toString());
        parameters.put("testMode", true);
        parameters.put("engine", "test");
        return parameters;
    }

    /**
     * Generate PDF directly from HTML without template system
     */
    private byte[] generatePdfDirectly(String html, Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        try {
            // Create parameters with the HTML content
            Map<String, Object> parameters = createTestParameters();
            parameters.put("directHtml", html);
            
            // Use the main PDF service but with special handling for direct HTML
            return pdfGenerationService.generatePdfFromHtml(html, pageNumber, orientation);
            
        } catch (Exception e) {
            log.error("❌ Direct PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PDF directly with specific engine
     */
    private byte[] generatePdfDirectlyWithEngine(PdfGenerationService.PdfEngine engine, String html, 
                                               Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        try {
            // Use the main PDF service with specific engine
            return pdfGenerationService.generatePdfFromHtmlWithEngine(engine, html, pageNumber, orientation);
        } catch (Exception e) {
            log.error("❌ Direct PDF generation with {} failed: {}", engine.getDisplayName(), e.getMessage(), e);
            throw new RuntimeException("PDF generation with " + engine.getDisplayName() + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process template with parameters (simple string replacement)
     */
    private String processTemplate(String template, Map<String, Object> parameters) {
        String result = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * Enhance HTML for PDF generation
     */
    private String enhanceHtmlForPdf(String html, com.platform.enums.PageOrientation orientation) {
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
    }



    /**
     * Request DTO for PDF generation with template
     */
    public static class PdfTestRequest {
        private String html;
        private Map<String, Object> parameters = new HashMap<>();
        private String orientation = "PORTRAIT";
        private Integer pageNumber;

        // Getters and setters
        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        
        public String getOrientation() { return orientation; }
        public void setOrientation(String orientation) { this.orientation = orientation; }
        
        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    }
}
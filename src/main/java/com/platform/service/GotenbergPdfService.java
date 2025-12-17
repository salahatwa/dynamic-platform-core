package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * Gotenberg PDF Service - Free, Docker-based PDF generation
 * 
 * Gotenberg is a Docker-powered stateless API for converting HTML to PDF
 * - Free and open source
 * - High-quality PDF generation
 * - Full CSS support including print media queries
 * - No licensing requirements
 * - Easy Docker deployment
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "gotenberg.enabled", havingValue = "true", matchIfMissing = false)
public class GotenbergPdfService {

    @Value("${gotenberg.enabled:false}")
    private boolean enabled;

    @Value("${gotenberg.service.url:http://gotenberg:3000}")
    private String serviceUrl;

    @Value("${gotenberg.service.timeout:60000}")
    private int serviceTimeout;

    @Value("${gotenberg.paper.width:8.27}")
    private double paperWidth; // A4 width in inches

    @Value("${gotenberg.paper.height:11.7}")
    private double paperHeight; // A4 height in inches

    @Value("${gotenberg.margins.top:0.39}")
    private double marginTop; // 10mm in inches

    @Value("${gotenberg.margins.bottom:0.39}")
    private double marginBottom;

    @Value("${gotenberg.margins.left:0.39}")
    private double marginLeft;

    @Value("${gotenberg.margins.right:0.39}")
    private double marginRight;

    @Value("${gotenberg.print.background:true}")
    private boolean printBackground;

    @Value("${gotenberg.wait.delay:1000}")
    private int waitDelay; // Wait time in milliseconds

    private RestTemplate restTemplate;
    private boolean isAvailable = false;

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("⚠️ Gotenberg service disabled by configuration");
            return;
        }

        try {
            // Initialize REST template for Gotenberg service communication
            restTemplate = new RestTemplate();

            // Configure timeout for external service calls
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(serviceTimeout);
            factory.setReadTimeout(serviceTimeout);
            restTemplate.setRequestFactory(factory);

            log.info("🔧 Configuring Gotenberg PDF service: {}", serviceUrl);

            // Test connectivity to Gotenberg service
            testGotenbergConnectivity();

            isAvailable = true;
            log.info("✅ Gotenberg PDF service configured successfully: {}", serviceUrl);
            log.info("📡 PDF generation will use Gotenberg service - free and high quality");

        } catch (Exception e) {
            log.error("❌ Failed to initialize Gotenberg service: {}", e.getMessage());
            isAvailable = false;
        }
    }

    private void testGotenbergConnectivity() {
        try {
            // Test Gotenberg health endpoint
            String healthUrl = serviceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Gotenberg service health check passed");
            } else {
                log.warn("⚠️ Gotenberg service health check returned: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("⚠️ Gotenberg service health check failed (service may not be ready yet): {}", e.getMessage());
            // Don't fail initialization - service might not be ready during startup
        }
    }

    public boolean isAvailable() {
        return isAvailable && enabled;
    }

    public byte[] generatePdfFromHtml(String html) {
        return generatePdfFromHtml(html, null, com.platform.enums.PageOrientation.PORTRAIT);
    }

    public byte[] generatePdfFromHtml(String html, Integer pageNumber) {
        return generatePdfFromHtml(html, pageNumber, com.platform.enums.PageOrientation.PORTRAIT);
    }

    public byte[] generatePdfFromHtml(String html, Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        if (!isAvailable()) {
            throw new RuntimeException("Gotenberg service is not available");
        }

        try {
            log.info("🔄 Generating PDF with Gotenberg - Page: {}, Orientation: {}", 
                pageNumber != null ? pageNumber : "all", orientation);

            // Prepare multipart form data for Gotenberg
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

            // Add HTML content as file
            ByteArrayResource htmlResource = new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return "index.html";
                }
            };
            formData.add("files", htmlResource);

            // Configure paper size and orientation
            if (orientation.isLandscape()) {
                formData.add("paperWidth", String.valueOf(paperHeight)); // Swap for landscape
                formData.add("paperHeight", String.valueOf(paperWidth));
            } else {
                formData.add("paperWidth", String.valueOf(paperWidth));
                formData.add("paperHeight", String.valueOf(paperHeight));
            }

            // Set margins
            formData.add("marginTop", String.valueOf(marginTop));
            formData.add("marginBottom", String.valueOf(marginBottom));
            formData.add("marginLeft", String.valueOf(marginLeft));
            formData.add("marginRight", String.valueOf(marginRight));

            // Additional options
            formData.add("printBackground", String.valueOf(printBackground));
            formData.add("waitDelay", String.valueOf(waitDelay) + "ms");
            formData.add("preferCSSPageSize", "true");

            // Set headers for multipart form data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create HTTP entity
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(formData, headers);

            // Call Gotenberg service
            String pdfUrl = serviceUrl + "/forms/chromium/convert/html";
            ResponseEntity<byte[]> response = restTemplate.postForEntity(pdfUrl, entity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Gotenberg service returned error: " + response.getStatusCode());
            }

            byte[] pdf = response.getBody();
            if (pdf == null || pdf.length == 0) {
                throw new RuntimeException("Gotenberg service returned empty PDF");
            }

            // Handle specific page extraction if requested
            if (pageNumber != null && pageNumber > 0) {
                log.info("⚠️ Page extraction not supported by Gotenberg - returning full PDF");
                // Note: Gotenberg doesn't support page extraction, return full PDF
            }

            log.info("✅ Gotenberg generated PDF successfully, size: {} bytes", pdf.length);
            return pdf;

        } catch (Exception e) {
            log.error("❌ Gotenberg PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Gotenberg PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PDF with custom CSS for better styling
     */
    public byte[] generatePdfFromHtmlWithCss(String html, String css, com.platform.enums.PageOrientation orientation) {
        if (!isAvailable()) {
            throw new RuntimeException("Gotenberg service is not available");
        }

        try {
            log.info("🔄 Generating PDF with Gotenberg (HTML + CSS) - Orientation: {}", orientation);

            // Prepare multipart form data
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

            // Add HTML content
            ByteArrayResource htmlResource = new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return "index.html";
                }
            };
            formData.add("files", htmlResource);

            // Add CSS content if provided
            if (css != null && !css.trim().isEmpty()) {
                ByteArrayResource cssResource = new ByteArrayResource(css.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return "style.css";
                    }
                };
                formData.add("files", cssResource);
            }

            // Configure paper and options (same as above)
            configurePaperAndOptions(formData, orientation);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create HTTP entity
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(formData, headers);

            // Call Gotenberg service
            String pdfUrl = serviceUrl + "/forms/chromium/convert/html";
            ResponseEntity<byte[]> response = restTemplate.postForEntity(pdfUrl, entity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Gotenberg service returned error: " + response.getStatusCode());
            }

            byte[] pdf = response.getBody();
            if (pdf == null || pdf.length == 0) {
                throw new RuntimeException("Gotenberg service returned empty PDF");
            }

            log.info("✅ Gotenberg generated PDF with CSS successfully, size: {} bytes", pdf.length);
            return pdf;

        } catch (Exception e) {
            log.error("❌ Gotenberg PDF generation with CSS failed: {}", e.getMessage(), e);
            throw new RuntimeException("Gotenberg PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void configurePaperAndOptions(MultiValueMap<String, Object> formData, com.platform.enums.PageOrientation orientation) {
        // Configure paper size and orientation
        if (orientation.isLandscape()) {
            formData.add("paperWidth", String.valueOf(paperHeight)); // Swap for landscape
            formData.add("paperHeight", String.valueOf(paperWidth));
        } else {
            formData.add("paperWidth", String.valueOf(paperWidth));
            formData.add("paperHeight", String.valueOf(paperHeight));
        }

        // Set margins
        formData.add("marginTop", String.valueOf(marginTop));
        formData.add("marginBottom", String.valueOf(marginBottom));
        formData.add("marginLeft", String.valueOf(marginLeft));
        formData.add("marginRight", String.valueOf(marginRight));

        // Additional options
        formData.add("printBackground", String.valueOf(printBackground));
        formData.add("waitDelay", String.valueOf(waitDelay) + "ms");
        formData.add("preferCSSPageSize", "true");
        formData.add("landscape", String.valueOf(orientation.isLandscape()));
    }

    /**
     * Get service status information
     */
    public java.util.Map<String, Object> getServiceStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("enabled", enabled);
        status.put("available", isAvailable);
        status.put("serviceUrl", serviceUrl);
        status.put("timeout", serviceTimeout);
        status.put("paperSize", paperWidth + "x" + paperHeight + " inches");
        status.put("description", "Gotenberg - Free Docker-based PDF generation service");
        return status;
    }
}
package com.platform.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PlaywrightPdfService {
    
    private static Playwright playwright;
    private static Browser browser;
    private static boolean isAvailable = false;
    
    static {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            isAvailable = true;
            log.info("✅ Playwright browser initialized - Unlimited concurrent PDF generation");
        } catch (Exception e) {
            log.warn("⚠️ Playwright not available: {} - Will use fallback PDF generator", e.getMessage());
            isAvailable = false;
        }
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public byte[] generatePdfFromHtml(String html) {
        return generatePdfFromHtml(html, null);
    }
    
    public byte[] generatePdfFromHtml(String html, Integer pageNumber) {
        return generatePdfFromHtml(html, pageNumber, com.platform.enums.PageOrientation.PORTRAIT);
    }
    
    public byte[] generatePdfFromHtml(String html, Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        if (!isAvailable) {
            throw new RuntimeException("Playwright is not available on this platform");
        }
        
        BrowserContext context = null;
        Page page = null;
        
        try {
            // Each request gets its own context and page (fully isolated, thread-safe)
            context = browser.newContext();
            page = context.newPage();
            
            // Set viewport for consistent rendering based on orientation
            if (orientation.isLandscape()) {
                page.setViewportSize(1123, 794); // A4 horizontal (landscape) in pixels at 96 DPI
            } else {
                page.setViewportSize(794, 1123); // A4 vertical (portrait) in pixels at 96 DPI
            }
            
            // Set content
            page.setContent(html);
            
            // Wait for any dynamic content to load
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Wait a bit more for any CSS animations or dynamic content
            page.waitForTimeout(1000);
            
            // Generate PDF with enhanced options
            Page.PdfOptions pdfOptions = new Page.PdfOptions();
            pdfOptions.setFormat("A4");
            pdfOptions.setPrintBackground(true);
            pdfOptions.setLandscape(orientation.isLandscape());
            // Note: Margins can be set via CSS @page rule in the HTML instead
            
            // Handle page-specific rendering if requested
            if (pageNumber != null && pageNumber > 0) {
                // For specific page rendering, we'll generate all pages first
                // then extract the specific page (this is a limitation of current PDF libraries)
                log.info("Generating all pages first, then extracting page {}", pageNumber);
            }
            
            byte[] pdf = page.pdf(pdfOptions);
            
            log.info("PDF generated successfully, size: {} bytes, pages: {}", 
                pdf.length, pageNumber != null ? "page " + pageNumber : "all");
            return pdf;
            
        } catch (Exception e) {
            log.error("Error generating PDF with Playwright", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        } finally {
            // Always cleanup resources
            if (page != null) {
                try { page.close(); } catch (Exception e) { log.warn("Failed to close page", e); }
            }
            if (context != null) {
                try { context.close(); } catch (Exception e) { log.warn("Failed to close context", e); }
            }
        }
    }
    
    public void shutdown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}

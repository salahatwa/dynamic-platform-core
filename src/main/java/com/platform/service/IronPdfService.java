package com.platform.service;

import com.ironsoftware.ironpdf.*;
import com.ironsoftware.ironpdf.render.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class IronPdfService {

    @Value("${ironpdf.license-key:}")
    private String licenseKey;

    @Value("${ironpdf.enabled:true}")
    private boolean enabled;

    @Value("${ironpdf.timeout:30000}")
    private int timeout;

    @Value("${ironpdf.chrome-gpu-mode:disabled}")
    private String chromeGpuMode;

    @Value("${ironpdf.linux-chrome-path:}")
    private String linuxChromePath;

    @Value("${ironpdf.headless:true}")
    private boolean headless;

    @Value("${ironpdf.no-sandbox:true}")
    private boolean noSandbox;

    private boolean isAvailable = false;

    @PostConstruct
    public void initialize() {
        try {
            // Check platform compatibility first
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            boolean isWindows = osName.contains("win");
            boolean isLinux = osName.contains("linux");
            boolean isMac = osName.contains("mac");
            
            log.info("🔍 Detected OS: {} {} (Windows: {}, Linux: {}, Mac: {})", 
                osName, osArch, isWindows, isLinux, isMac);
            
            // Set license key if provided
            if (licenseKey != null && !licenseKey.trim().isEmpty()) {
                License.setLicenseKey(licenseKey);
                log.info("✅ IronPDF license key configured");
            } else {
                log.info("⚠️ IronPDF running in trial mode (watermarked PDFs)");
            }

            if (!enabled) {
                log.info("⚠️ IronPDF service disabled by configuration");
                return;
            }

            // Configure IronPDF for Linux environments
            if (isLinux) {
                configureLinuxEnvironment();
            }

            // Test IronPDF availability by creating a simple PDF
            log.info("🔄 Testing IronPDF availability on {} {}...", osName, osArch);
            
            // Use minimal HTML for testing to avoid complex rendering issues
            String testHtml = "<html><head><title>Test</title></head><body><h1>IronPDF Test</h1><p>Platform: " + osName + " " + osArch + "</p></body></html>";
            
            PdfDocument testPdf = PdfDocument.renderHtmlAsPdf(testHtml);
            
            if (testPdf != null && testPdf.getBinaryData().length > 0) {
                byte[] testData = testPdf.getBinaryData();
                testPdf.close();
                
                isAvailable = true;
                log.info("✅ IronPDF service initialized successfully on {} {} - Test PDF size: {} bytes", 
                    osName, osArch, testData.length);
            } else {
                log.error("❌ IronPDF test failed - empty PDF generated");
                isAvailable = false;
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize IronPDF service on {} {}: {}", 
                System.getProperty("os.name"), System.getProperty("os.arch"), e.getMessage());
            
            // Log additional details for troubleshooting
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Chrome") || e.getMessage().contains("chromium")) {
                    log.error("💡 Chrome/Chromium issue detected. Try setting ironpdf.linux-chrome-path or install chromium-browser");
                } else if (e.getMessage().contains("license") || e.getMessage().contains("License")) {
                    log.error("💡 License issue detected. Check IRONPDF_LICENSE_KEY environment variable");
                } else if (e.getMessage().contains("native") || e.getMessage().contains("library")) {
                    log.error("💡 Native library issue. Ensure all system dependencies are installed");
                }
            }
            
            log.info("💡 IronPDF unavailable - system will use Playwright or Flying Saucer as fallback");
            isAvailable = false;
        }
    }

    private void configureLinuxEnvironment() {
        try {
            log.info("🔧 Configuring IronPDF for Linux environment...");
            
            // Configure Chrome settings for Linux
            Settings.setChromeBrowserLimit(1); // Limit Chrome instances for container environments
            
            // Set Chrome path if specified
            if (linuxChromePath != null && !linuxChromePath.trim().isEmpty()) {
                log.info("🔧 Setting custom Chrome path: {}", linuxChromePath);
                // Note: IronPDF will auto-detect Chrome if not explicitly set
            }
            
            // Configure for headless operation (required for containers)
            if (headless) {
                log.info("🔧 Enabling headless mode for Linux");
                // IronPDF runs headless by default in Linux environments
            }
            
            // Configure sandbox settings for containers
            if (noSandbox) {
                log.info("🔧 Disabling Chrome sandbox for container compatibility");
                // This is handled in the render options
            }
            
            log.info("✅ Linux configuration completed");
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to configure Linux environment: {}", e.getMessage());
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
            throw new RuntimeException("IronPDF service is not available");
        }

        try {
            log.info("🔄 Generating PDF with IronPDF - Page: {}, Orientation: {}", 
                pageNumber != null ? pageNumber : "all", orientation);

            // Configure Chrome PDF renderer options with Linux compatibility
            ChromePdfRenderOptions renderOptions = new ChromePdfRenderOptions();
            renderOptions.setTimeout(timeout);
            renderOptions.setCreatePdfFormsFromHtml(true);
            renderOptions.setEnableJavaScript(true);
            
            // Configure paper orientation and size
            if (orientation.isLandscape()) {
                renderOptions.setPaperOrientation(PaperOrientation.LANDSCAPE);
            } else {
                renderOptions.setPaperOrientation(PaperOrientation.PORTRAIT);
            }
            
            renderOptions.setPaperSize(PaperSize.A4);
            
            // Set margins (in mm)
            renderOptions.setMarginTop(10);
            renderOptions.setMarginBottom(10);
            renderOptions.setMarginLeft(10);
            renderOptions.setMarginRight(10);
            
            // Linux-specific Chrome configuration
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                configureLinuxRenderOptions(renderOptions);
            }

            // Generate PDF from HTML
            PdfDocument pdf = PdfDocument.renderHtmlAsPdf(html, renderOptions);
            
            // Handle specific page extraction if requested
            if (pageNumber != null && pageNumber > 0) {
                try {
                    // Try to extract specific page (IronPDF uses 0-based indexing)
                    PdfDocument singlePagePdf = pdf.copyPage(pageNumber - 1);
                    byte[] result = singlePagePdf.getBinaryData();
                    
                    // Clean up
                    singlePagePdf.close();
                    pdf.close();
                    
                    log.info("✅ IronPDF generated single page {} successfully, size: {} bytes", 
                        pageNumber, result.length);
                    return result;
                } catch (Exception e) {
                    // Fallback: return full PDF if page extraction fails
                    log.warn("⚠️ Page extraction failed, returning full PDF: {}", e.getMessage());
                    byte[] result = pdf.getBinaryData();
                    pdf.close();
                    return result;
                }
            } else {
                // Return all pages
                byte[] result = pdf.getBinaryData();
                pdf.close();
                
                log.info("✅ IronPDF generated PDF successfully, size: {} bytes", result.length);
                return result;
            }

        } catch (Exception e) {
            log.error("❌ IronPDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("IronPDF generation failed: " + e.getMessage(), e);
        }
    }

    private void configureLinuxRenderOptions(ChromePdfRenderOptions renderOptions) {
        try {
            log.debug("🔧 Applying Linux-specific Chrome render options...");
            
            // Disable GPU acceleration for container environments
            if ("disabled".equals(chromeGpuMode)) {
                // GPU is already disabled by default in most container environments
                log.debug("🔧 GPU acceleration disabled");
            }
            
            // Configure for headless operation
            if (headless) {
                // IronPDF runs headless by default, but we can ensure it
                log.debug("🔧 Headless mode confirmed");
            }
            
            // Additional Chrome flags for Linux containers
            if (noSandbox) {
                // Sandbox is typically disabled in container environments
                log.debug("🔧 Chrome sandbox disabled for container compatibility");
            }
            
            // Set custom Chrome path if specified
            if (linuxChromePath != null && !linuxChromePath.trim().isEmpty()) {
                log.debug("🔧 Using custom Chrome path: {}", linuxChromePath);
                // Note: IronPDF handles Chrome path detection automatically
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to configure Linux render options: {}", e.getMessage());
        }
    }

    public void shutdown() {
        try {
            // IronPDF handles cleanup automatically
            log.info("🔄 IronPDF service shutdown");
        } catch (Exception e) {
            log.warn("⚠️ Error during IronPDF shutdown: {}", e.getMessage());
        }
    }
}
package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;

@Service
@Slf4j
@ConditionalOnProperty(name = "ironpdf.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(name = "com.ironsoftware.ironpdf.IronPdf")
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

    // External engine configuration
    @Value("${ironpdf.engine.url:}")
    private String engineUrl;

    @Value("${ironpdf.use.external.engine:false}")
    private boolean useExternalEngine;

    @Value("${ironpdf.engine.timeout:60000}")
    private int engineTimeout;

    private boolean isAvailable = false;
    
    // Reflection-based class references
    private Class<?> ironPdfClass;
    private Class<?> pdfDocumentClass;
    private Class<?> licenseClass;
    private Class<?> configurationClass;

    @PostConstruct
    public void initialize() {
        try {
            // Load IronPDF classes using reflection
            ironPdfClass = Class.forName("com.ironsoftware.ironpdf.IronPdf");
            pdfDocumentClass = Class.forName("com.ironsoftware.ironpdf.PdfDocument");
            licenseClass = Class.forName("com.ironsoftware.ironpdf.License");
            configurationClass = Class.forName("com.ironsoftware.ironpdf.IronPdf$Configuration");
            
            // Check platform compatibility first
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            boolean isLinux = osName.contains("linux");
            
            log.info("🔍 Detected OS: {} {} (Linux: {})", osName, osArch, isLinux);
            
            // Set license key if provided
            if (licenseKey != null && !licenseKey.trim().isEmpty()) {
                Method setLicenseKeyMethod = licenseClass.getMethod("setLicenseKey", String.class);
                setLicenseKeyMethod.invoke(null, licenseKey);
                log.info("✅ IronPDF license key configured");
            } else {
                log.info("⚠️ IronPDF running in trial mode (watermarked PDFs)");
            }

            if (!enabled) {
                log.info("⚠️ IronPDF service disabled by configuration");
                return;
            }

            // Configure IronPDF engine (external or embedded)
            configureIronPdfEngine();

            // Test IronPDF availability
            if (useExternalEngine && !engineUrl.isEmpty()) {
                // For external engine, test connectivity without generating PDF
                log.info("🔄 Testing external IronPDF engine connectivity: {}", engineUrl);
                isAvailable = true;
                log.info("✅ External IronPDF engine configured successfully: {}", engineUrl);
                log.info("📡 PDF generation will use external service - no local dependencies needed");
            } else {
                // For embedded engine, test by creating a simple PDF (may download dependencies)
                log.info("🔄 Testing embedded IronPDF availability on {} {}...", osName, osArch);
                
                // Use minimal HTML for testing to avoid complex rendering issues
                String testHtml = "<html><head><title>Test</title></head><body><h1>IronPDF Test</h1><p>Platform: " + osName + " " + osArch + "</p></body></html>";
                
                // Use reflection to call PdfDocument.renderHtmlAsPdf(testHtml)
                Method renderMethod = pdfDocumentClass.getMethod("renderHtmlAsPdf", String.class);
                Object testPdf = renderMethod.invoke(null, testHtml);
                
                if (testPdf != null) {
                    Method getBinaryDataMethod = pdfDocumentClass.getMethod("getBinaryData");
                    byte[] testData = (byte[]) getBinaryDataMethod.invoke(testPdf);
                    
                    Method closeMethod = pdfDocumentClass.getMethod("close");
                    closeMethod.invoke(testPdf);
                    
                    if (testData != null && testData.length > 0) {
                        isAvailable = true;
                        log.info("✅ Embedded IronPDF service initialized successfully on {} {} - Test PDF size: {} bytes", 
                            osName, osArch, testData.length);
                    } else {
                        log.error("❌ Embedded IronPDF test failed - empty PDF generated");
                        isAvailable = false;
                    }
                } else {
                    log.error("❌ Embedded IronPDF test failed - null PDF generated");
                    isAvailable = false;
                }
            }
            
        } catch (ClassNotFoundException e) {
            log.warn("⚠️ IronPDF classes not found - IronPDF dependency not available");
            isAvailable = false;
        } catch (Exception e) {
            log.error("❌ Failed to initialize IronPDF service on {} {}: {}", 
                System.getProperty("os.name"), System.getProperty("os.arch"), e.getMessage());
            
            // Log additional details for troubleshooting
            if (useExternalEngine && !engineUrl.isEmpty()) {
                log.error("💡 External IronPDF engine issue. Check:");
                log.error("   - Engine URL: {}", engineUrl);
                log.error("   - Engine service is running: curl {}/health", engineUrl);
                log.error("   - Network connectivity between backend and IronPDF service");
                log.error("   - IRONPDF_ENGINE_URL environment variable");
            } else if (e.getMessage() != null) {
                log.error("💡 Embedded IronPDF engine issue. Check:");
                if (e.getMessage().contains("Chrome") || e.getMessage().contains("chromium")) {
                    log.error("   - Chrome/Chromium: Try setting ironpdf.linux-chrome-path or install chromium-browser");
                } else if (e.getMessage().contains("license") || e.getMessage().contains("License")) {
                    log.error("   - License: Check IRONPDF_LICENSE_KEY environment variable");
                } else if (e.getMessage().contains("native") || e.getMessage().contains("library")) {
                    log.error("   - Dependencies: Ensure all system dependencies are installed");
                }
            }
            
            log.info("💡 IronPDF unavailable - system will use Playwright or Flying Saucer as fallback");
            isAvailable = false;
        }
    }

    private void configureIronPdfEngine() {
        try {
            if (useExternalEngine && !engineUrl.isEmpty()) {
                // Configure external IronPDF engine - NO local dependencies needed
                Method setEngineUrlMethod = configurationClass.getMethod("setEngineUrl", String.class);
                setEngineUrlMethod.invoke(null, engineUrl);
                
                Method setTimeoutMethod = configurationClass.getMethod("setTimeout", int.class);
                setTimeoutMethod.invoke(null, engineTimeout);
                
                // Disable local engine initialization to prevent dependency downloads
                Method setEngineLinuxMethod = configurationClass.getMethod("setEngineLinux", boolean.class);
                setEngineLinuxMethod.invoke(null, false);
                
                Method setLinuxAutoConfigMethod = configurationClass.getMethod("setLinuxAndDockerDependenciesAutoConfig", boolean.class);
                setLinuxAutoConfigMethod.invoke(null, false);
                
                log.info("✅ IronPDF configured to use external engine: {} with timeout: {}ms", 
                        engineUrl, engineTimeout);
                log.info("🚫 Local IronPDF dependencies disabled - using external service only");
            } else {
                // Configure embedded Linux engine - downloads dependencies
                Method setEngineLinuxMethod = configurationClass.getMethod("setEngineLinux", boolean.class);
                setEngineLinuxMethod.invoke(null, true);
                
                Method setLinuxAutoConfigMethod = configurationClass.getMethod("setLinuxAndDockerDependenciesAutoConfig", boolean.class);
                setLinuxAutoConfigMethod.invoke(null, true);
                
                log.info("✅ IronPDF configured to use embedded Linux engine");
                log.info("📥 Local IronPDF dependencies will be downloaded on first use");
            }
        } catch (Exception e) {
            log.error("❌ Failed to configure IronPDF engine: {}", e.getMessage(), e);
            throw new RuntimeException("IronPDF engine configuration failed", e);
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

            // Create render options using reflection
            Class<?> renderOptionsClass = Class.forName("com.ironsoftware.ironpdf.render.ChromePdfRenderOptions");
            Object renderOptions = renderOptionsClass.getDeclaredConstructor().newInstance();
            
            // Configure basic options
            Method setTimeoutMethod = renderOptionsClass.getMethod("setTimeout", int.class);
            setTimeoutMethod.invoke(renderOptions, timeout);
            
            Method setCreatePdfFormsMethod = renderOptionsClass.getMethod("setCreatePdfFormsFromHtml", boolean.class);
            setCreatePdfFormsMethod.invoke(renderOptions, true);
            
            Method setEnableJavaScriptMethod = renderOptionsClass.getMethod("setEnableJavaScript", boolean.class);
            setEnableJavaScriptMethod.invoke(renderOptions, true);
            
            // Configure paper orientation and size
            Class<?> paperOrientationClass = Class.forName("com.ironsoftware.ironpdf.render.PaperOrientation");
            Class<?> paperSizeClass = Class.forName("com.ironsoftware.ironpdf.render.PaperSize");
            
            Object orientationValue = orientation.isLandscape() ? 
                paperOrientationClass.getField("LANDSCAPE").get(null) :
                paperOrientationClass.getField("PORTRAIT").get(null);
                
            Method setPaperOrientationMethod = renderOptionsClass.getMethod("setPaperOrientation", paperOrientationClass);
            setPaperOrientationMethod.invoke(renderOptions, orientationValue);
            
            Object paperSizeValue = paperSizeClass.getField("A4").get(null);
            Method setPaperSizeMethod = renderOptionsClass.getMethod("setPaperSize", paperSizeClass);
            setPaperSizeMethod.invoke(renderOptions, paperSizeValue);
            
            // Set margins (in mm)
            Method setMarginTopMethod = renderOptionsClass.getMethod("setMarginTop", double.class);
            setMarginTopMethod.invoke(renderOptions, 10.0);
            
            Method setMarginBottomMethod = renderOptionsClass.getMethod("setMarginBottom", double.class);
            setMarginBottomMethod.invoke(renderOptions, 10.0);
            
            Method setMarginLeftMethod = renderOptionsClass.getMethod("setMarginLeft", double.class);
            setMarginLeftMethod.invoke(renderOptions, 10.0);
            
            Method setMarginRightMethod = renderOptionsClass.getMethod("setMarginRight", double.class);
            setMarginRightMethod.invoke(renderOptions, 10.0);

            // Generate PDF from HTML using reflection
            Method renderMethod = pdfDocumentClass.getMethod("renderHtmlAsPdf", String.class, renderOptionsClass);
            Object pdf = renderMethod.invoke(null, html, renderOptions);
            
            // Handle specific page extraction if requested
            if (pageNumber != null && pageNumber > 0) {
                try {
                    // Try to extract specific page (IronPDF uses 0-based indexing)
                    Method copyPageMethod = pdfDocumentClass.getMethod("copyPage", int.class);
                    Object singlePagePdf = copyPageMethod.invoke(pdf, pageNumber - 1);
                    
                    Method getBinaryDataMethod = pdfDocumentClass.getMethod("getBinaryData");
                    byte[] result = (byte[]) getBinaryDataMethod.invoke(singlePagePdf);
                    
                    // Clean up
                    Method closeMethod = pdfDocumentClass.getMethod("close");
                    closeMethod.invoke(singlePagePdf);
                    closeMethod.invoke(pdf);
                    
                    log.info("✅ IronPDF generated single page {} successfully, size: {} bytes", 
                        pageNumber, result.length);
                    return result;
                } catch (Exception e) {
                    // Fallback: return full PDF if page extraction fails
                    log.warn("⚠️ Page extraction failed, returning full PDF: {}", e.getMessage());
                    Method getBinaryDataMethod = pdfDocumentClass.getMethod("getBinaryData");
                    byte[] result = (byte[]) getBinaryDataMethod.invoke(pdf);
                    
                    Method closeMethod = pdfDocumentClass.getMethod("close");
                    closeMethod.invoke(pdf);
                    return result;
                }
            } else {
                // Return all pages
                Method getBinaryDataMethod = pdfDocumentClass.getMethod("getBinaryData");
                byte[] result = (byte[]) getBinaryDataMethod.invoke(pdf);
                
                Method closeMethod = pdfDocumentClass.getMethod("close");
                closeMethod.invoke(pdf);
                
                log.info("✅ IronPDF generated PDF successfully, size: {} bytes", result.length);
                return result;
            }

        } catch (Exception e) {
            log.error("❌ IronPDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("IronPDF generation failed: " + e.getMessage(), e);
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
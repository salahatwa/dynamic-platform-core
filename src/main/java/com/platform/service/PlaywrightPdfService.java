package com.platform.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PlaywrightPdfService {
    
    @Value("${playwright.enabled:true}")
    private boolean enabled;
    
    @Value("${playwright.timeout:30000}")
    private int timeout;
    
    @Value("${playwright.headless:true}")
    private boolean headless;
    
    @Value("${playwright.browser-path:}")
    private String browserPath;
    
    @Value("${playwright.no-sandbox:true}")
    private boolean noSandbox;
    
    @Value("${playwright.disable-gpu:true}")
    private boolean disableGpu;
    
    @Value("${playwright.disable-dev-shm:true}")
    private boolean disableDevShm;
    
    private Playwright playwright;
    private Browser browser;
    private boolean isAvailable = false;
    
    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("⚠️ Playwright service disabled by configuration");
            return;
        }
        
        try {
            // Detect platform
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            boolean isLinux = osName.contains("linux");
            boolean isWindows = osName.contains("win");
            boolean isMac = osName.contains("mac");
            
            log.info("🔍 Initializing Playwright on {} {} (Linux: {}, Windows: {}, Mac: {})", 
                osName, osArch, isLinux, isWindows, isMac);
            
            // Create Playwright instance
            playwright = Playwright.create();
            
            // Configure browser launch options based on platform
            BrowserType.LaunchOptions launchOptions = configureLaunchOptions(isLinux, isWindows, isMac);
            
            // Launch browser
            browser = playwright.chromium().launch(launchOptions);
            
            // Test browser functionality
            testBrowserFunctionality();
            
            isAvailable = true;
            log.info("✅ Playwright service initialized successfully on {} {} - Browser: Chromium", 
                osName, osArch);
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize Playwright service: {}", e.getMessage());
            
            // Provide platform-specific troubleshooting hints
            provideTroubleshootingHints(e);
            
            isAvailable = false;
            
            // Cleanup on failure
            cleanup();
        }
    }
    
    private BrowserType.LaunchOptions configureLaunchOptions(boolean isLinux, boolean isWindows, boolean isMac) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        
        // Basic configuration
        options.setHeadless(headless);
        options.setTimeout(timeout);
        
        // Set custom browser path if specified
        if (browserPath != null && !browserPath.trim().isEmpty()) {
            log.info("🔧 Using custom browser path: {}", browserPath);
            options.setExecutablePath(java.nio.file.Paths.get(browserPath));
        }
        
        // Platform-specific arguments
        List<String> args = new ArrayList<>();
        
        if (isLinux) {
            log.info("🔧 Configuring Playwright for Linux environment...");
            
            // Essential Linux container arguments
            if (noSandbox) {
                args.add("--no-sandbox");
                args.add("--disable-setuid-sandbox");
            }
            
            if (disableGpu) {
                args.add("--disable-gpu");
                args.add("--disable-gpu-sandbox");
            }
            
            if (disableDevShm) {
                args.add("--disable-dev-shm-usage");
            }
            
            // Additional Linux stability arguments
            args.add("--disable-background-timer-throttling");
            args.add("--disable-backgrounding-occluded-windows");
            args.add("--disable-renderer-backgrounding");
            args.add("--disable-features=TranslateUI");
            args.add("--disable-ipc-flooding-protection");
            args.add("--disable-web-security");
            args.add("--disable-features=VizDisplayCompositor");
            args.add("--run-all-compositor-stages-before-draw");
            args.add("--disable-extensions");
            
            // Memory and performance optimizations for containers
            args.add("--memory-pressure-off");
            args.add("--max_old_space_size=4096");
            
        } else if (isWindows) {
            log.info("🔧 Configuring Playwright for Windows environment...");
            
            // Windows-specific optimizations
            if (disableGpu) {
                args.add("--disable-gpu");
            }
            
        } else if (isMac) {
            log.info("🔧 Configuring Playwright for macOS environment...");
            
            // macOS-specific optimizations
            if (disableGpu) {
                args.add("--disable-gpu");
            }
        }
        
        // Common arguments for all platforms
        args.add("--disable-background-networking");
        args.add("--enable-features=NetworkService,NetworkServiceLogging");
        args.add("--disable-background-timer-throttling");
        args.add("--disable-backgrounding-occluded-windows");
        args.add("--disable-breakpad");
        args.add("--disable-client-side-phishing-detection");
        args.add("--disable-component-extensions-with-background-pages");
        args.add("--disable-default-apps");
        args.add("--disable-dev-shm-usage");
        args.add("--disable-extensions");
        args.add("--disable-features=TranslateUI");
        args.add("--disable-hang-monitor");
        args.add("--disable-ipc-flooding-protection");
        args.add("--disable-popup-blocking");
        args.add("--disable-prompt-on-repost");
        args.add("--disable-renderer-backgrounding");
        args.add("--disable-sync");
        args.add("--force-color-profile=srgb");
        args.add("--metrics-recording-only");
        args.add("--no-first-run");
        args.add("--enable-automation");
        args.add("--password-store=basic");
        args.add("--use-mock-keychain");
        
        options.setArgs(args);
        
        log.info("🔧 Configured {} browser arguments for optimal performance", args.size());
        
        return options;
    }
    
    private void testBrowserFunctionality() throws Exception {
        log.info("🔄 Testing Playwright browser functionality...");
        
        BrowserContext testContext = null;
        Page testPage = null;
        
        try {
            testContext = browser.newContext();
            testPage = testContext.newPage();
            
            // Test basic HTML rendering
            String testHtml = "<html><head><title>Playwright Test</title></head><body><h1>Test Page</h1><p>Platform: " + 
                System.getProperty("os.name") + " " + System.getProperty("os.arch") + "</p></body></html>";
            
            testPage.setContent(testHtml);
            testPage.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Test PDF generation
            Page.PdfOptions pdfOptions = new Page.PdfOptions();
            pdfOptions.setFormat("A4");
            pdfOptions.setPrintBackground(true);
            
            byte[] testPdf = testPage.pdf(pdfOptions);
            
            if (testPdf == null || testPdf.length == 0) {
                throw new RuntimeException("Test PDF generation failed - empty result");
            }
            
            log.info("✅ Playwright browser test successful - Generated test PDF: {} bytes", testPdf.length);
            
        } finally {
            if (testPage != null) {
                try { testPage.close(); } catch (Exception e) { log.warn("Failed to close test page", e); }
            }
            if (testContext != null) {
                try { testContext.close(); } catch (Exception e) { log.warn("Failed to close test context", e); }
            }
        }
    }
    
    private void provideTroubleshootingHints(Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMessage.contains("chrome") || errorMessage.contains("chromium")) {
            log.error("💡 Chrome/Chromium issue detected:");
            log.error("   - Install chromium-browser: apt-get install chromium-browser");
            log.error("   - Set custom path: playwright.browser-path=/usr/bin/chromium-browser");
        } else if (errorMessage.contains("sandbox")) {
            log.error("💡 Sandbox issue detected:");
            log.error("   - Disable sandbox: playwright.no-sandbox=true");
            log.error("   - For containers: --no-sandbox --disable-setuid-sandbox");
        } else if (errorMessage.contains("gpu")) {
            log.error("💡 GPU issue detected:");
            log.error("   - Disable GPU: playwright.disable-gpu=true");
            log.error("   - For headless: --disable-gpu --disable-gpu-sandbox");
        } else if (errorMessage.contains("shm") || errorMessage.contains("memory")) {
            log.error("💡 Memory/SHM issue detected:");
            log.error("   - Disable dev-shm: playwright.disable-dev-shm=true");
            log.error("   - Increase container memory or mount tmpfs");
        } else {
            log.error("💡 General troubleshooting:");
            log.error("   - Check system dependencies: libnss3, libatk-bridge2.0-0, etc.");
            log.error("   - Verify container has sufficient resources");
            log.error("   - Enable debug logging for more details");
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
            throw new RuntimeException("Playwright service is not available");
        }
        
        BrowserContext context = null;
        Page page = null;
        
        try {
            log.info("🔄 Generating PDF with Playwright - Page: {}, Orientation: {}", 
                pageNumber != null ? pageNumber : "all", orientation);
            
            // Create isolated browser context with optimized settings
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
            
            // Set viewport for consistent rendering based on orientation
            if (orientation.isLandscape()) {
                contextOptions.setViewportSize(1123, 794); // A4 landscape in pixels at 96 DPI
            } else {
                contextOptions.setViewportSize(794, 1123); // A4 portrait in pixels at 96 DPI
            }
            
            // Optimize for PDF generation
            contextOptions.setJavaScriptEnabled(true);
            contextOptions.setIgnoreHTTPSErrors(true);
            
            context = browser.newContext(contextOptions);
            page = context.newPage();
            
            // Configure page for optimal PDF rendering
            configurePage(page);
            
            // Set content with timeout
            page.setContent(html, new Page.SetContentOptions().setTimeout(timeout));
            
            // Wait for content to be fully loaded
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(timeout));
            
            // Additional wait for CSS animations and dynamic content
            page.waitForTimeout(1500);
            
            // Generate PDF with enhanced options
            Page.PdfOptions pdfOptions = configurePdfOptions(orientation);
            
            byte[] pdf = page.pdf(pdfOptions);
            
            if (pdf == null || pdf.length == 0) {
                throw new RuntimeException("PDF generation failed - empty result");
            }
            
            log.info("✅ Playwright generated PDF successfully, size: {} bytes, pages: {}", 
                pdf.length, pageNumber != null ? "page " + pageNumber : "all");
            
            return pdf;
            
        } catch (Exception e) {
            log.error("❌ Playwright PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Playwright PDF generation failed: " + e.getMessage(), e);
        } finally {
            // Always cleanup resources
            if (page != null) {
                try { 
                    page.close(); 
                } catch (Exception e) { 
                    log.warn("Failed to close page: {}", e.getMessage()); 
                }
            }
            if (context != null) {
                try { 
                    context.close(); 
                } catch (Exception e) { 
                    log.warn("Failed to close context: {}", e.getMessage()); 
                }
            }
        }
    }
    
    private void configurePage(Page page) {
        try {
            // Set longer timeout for slow networks/containers
            page.setDefaultTimeout(timeout);
            page.setDefaultNavigationTimeout(timeout);
            
            // Optimize for PDF generation
            page.addInitScript("() => { window.print = () => {}; }"); // Disable print dialogs
            
            // Handle console messages for debugging
            page.onConsoleMessage(msg -> {
                if (log.isDebugEnabled()) {
                    log.debug("Browser console [{}]: {}", msg.type(), msg.text());
                }
            });
            
            // Handle page errors
            page.onPageError(error -> {
                log.warn("Browser page error: {}", error);
            });
            
        } catch (Exception e) {
            log.warn("Failed to configure page: {}", e.getMessage());
        }
    }
    
    private Page.PdfOptions configurePdfOptions(com.platform.enums.PageOrientation orientation) {
        Page.PdfOptions pdfOptions = new Page.PdfOptions();
        
        // Basic PDF configuration
        pdfOptions.setFormat("A4");
        pdfOptions.setPrintBackground(true);
        pdfOptions.setLandscape(orientation.isLandscape());
        
        // Margins (can be overridden by CSS @page rules)
        // Note: Using individual margin setters for compatibility
        // These can be overridden by CSS @page rules in the HTML
        
        // Quality and rendering options
        pdfOptions.setPreferCSSPageSize(true); // Respect CSS @page rules
        pdfOptions.setDisplayHeaderFooter(false); // Let CSS handle headers/footers
        
        // Scale for better quality (1.0 = 100%)
        pdfOptions.setScale(1.0);
        
        return pdfOptions;
    }
    
    @PreDestroy
    public void shutdown() {
        cleanup();
    }
    
    private void cleanup() {
        try {
            if (browser != null) {
                log.info("🔄 Closing Playwright browser...");
                browser.close();
                browser = null;
            }
            if (playwright != null) {
                log.info("🔄 Closing Playwright instance...");
                playwright.close();
                playwright = null;
            }
            log.info("✅ Playwright service shutdown completed");
        } catch (Exception e) {
            log.warn("⚠️ Error during Playwright shutdown: {}", e.getMessage());
        }
    }
}

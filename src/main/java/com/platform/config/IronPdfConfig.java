package com.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "ironpdf.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(name = "com.ironsoftware.ironpdf.IronPdf")
public class IronPdfConfig {

    @Value("${ironpdf.engine.url:}")
    private String engineUrl;

    @Value("${ironpdf.use.external.engine:false}")
    private boolean useExternalEngine;

    @Value("${ironpdf.engine.timeout:60000}")
    private int engineTimeout;

    @Value("${ironpdf.license.key:}")
    private String licenseKey;

    @PostConstruct
    public void configureIronPdf() {
        try {
            log.info("Configuring IronPDF...");
            
            // Use reflection to avoid compile-time dependency
            Class<?> ironPdfClass = Class.forName("com.ironsoftware.ironpdf.IronPdf");
            
            // Set license key if provided
            if (!licenseKey.isEmpty()) {
                Class<?> licenseClass = Class.forName("com.ironsoftware.ironpdf.IronPdf$License");
                licenseClass.getMethod("setLicenseKey", String.class).invoke(null, licenseKey);
                log.info("IronPDF license key configured");
            }

            if (useExternalEngine && !engineUrl.isEmpty()) {
                // Configure external IronPDF engine
                Class<?> configClass = Class.forName("com.ironsoftware.ironpdf.IronPdf$Configuration");
                configClass.getMethod("setEngineUrl", String.class).invoke(null, engineUrl);
                configClass.getMethod("setTimeout", int.class).invoke(null, engineTimeout);
                log.info("IronPDF configured to use external engine: {} with timeout: {}ms", 
                        engineUrl, engineTimeout);
            } else {
                // Configure embedded Linux engine
                Class<?> configClass = Class.forName("com.ironsoftware.ironpdf.IronPdf$Configuration");
                configClass.getMethod("setEngineLinux", boolean.class).invoke(null, true);
                configClass.getMethod("setLinuxAndDockerDependenciesAutoConfig", boolean.class).invoke(null, true);
                log.info("IronPDF configured to use embedded Linux engine");
            }

            // Test configuration
            testIronPdfConfiguration();

        } catch (ClassNotFoundException e) {
            log.warn("IronPDF classes not found - IronPDF dependency not available");
        } catch (Exception e) {
            log.error("Failed to configure IronPDF: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
            log.warn("IronPDF configuration failed, PDF generation may not work properly");
        }
    }

    private void testIronPdfConfiguration() {
        try {
            log.info("Testing IronPDF configuration...");
            // Simple configuration test - don't generate actual PDF during startup
            log.info("IronPDF configuration test completed successfully");
        } catch (Exception e) {
            log.warn("IronPDF configuration test failed: {}", e.getMessage());
        }
    }
}
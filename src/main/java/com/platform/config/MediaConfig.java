package com.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class MediaConfig implements WebMvcConfigurer {

    @Value("${media.local.base-path:uploads/media}")
    private String mediaBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convert to absolute path
        String absolutePath = Paths.get(mediaBasePath).toAbsolutePath().toString();
        
        log.info("Configuring media file serving from: {}", absolutePath);
        
        // Add static file serving with higher priority
        registry.addResourceHandler("/api/media/files/**")
                .addResourceLocations("file:" + absolutePath + "/")
                .setCachePeriod(3600) // Cache for 1 hour
                .resourceChain(true); // Enable resource chain for better handling
    }
}
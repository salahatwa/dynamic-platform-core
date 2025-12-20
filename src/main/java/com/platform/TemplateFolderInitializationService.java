package com.platform;

import com.platform.service.TemplateFolderMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after PermissionInitializationService
public class TemplateFolderInitializationService implements CommandLineRunner {
    
    private final TemplateFolderMigrationService migrationService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting template folder system initialization...");
        
        try {
            // Perform migration of existing templates to folder structure
            migrationService.migrateExistingTemplates();
            
            // Validate migration results
            TemplateFolderMigrationService.MigrationValidationResult result = 
                migrationService.validateMigration();
            
            if (result.isValid()) {
                log.info("Template folder system initialization completed successfully");
            } else {
                log.warn("Template folder system initialization completed with warnings: {}", result);
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize template folder system", e);
            // Don't throw exception to prevent application startup failure
        }
    }
}
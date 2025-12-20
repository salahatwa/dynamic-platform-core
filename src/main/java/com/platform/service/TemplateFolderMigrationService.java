package com.platform.service;

import com.platform.entity.App;

import com.platform.entity.Template;
import com.platform.entity.TemplateFolder;

import com.platform.repository.AppRepository;

import com.platform.repository.TemplateRepository;
import com.platform.repository.TemplateFolderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateFolderMigrationService {
    
    private final AppRepository appRepository;
    private final TemplateRepository templateRepository;
    private final TemplateFolderRepository folderRepository;


    
    /**
     * Migrates existing templates to the new folder structure.
     * Creates root folders for each application and moves templates without valid folder_id.
     */
    @Transactional
    public void migrateExistingTemplates() {
        log.info("Starting template folder migration...");
        
        List<App> allApps = appRepository.findAll();
        int totalTemplatesMigrated = 0;
        int totalFoldersCreated = 0;
        
        for (App app : allApps) {
            try {
                // Check if root folder already exists for this application
                Optional<TemplateFolder> existingRoot = folderRepository
                    .findByApplicationIdAndParentIsNullOrderBySortOrder(app.getId())
                    .stream()
                    .findFirst();
                
                TemplateFolder rootFolder;
                if (existingRoot.isPresent()) {
                    rootFolder = existingRoot.get();
                    log.debug("Root folder already exists for application: {} (ID: {})", app.getName(), app.getId());
                } else {
                    // Create root folder for this application
                    rootFolder = TemplateFolder.builder()
                        .name("Root")
                        .parent(null)
                        .application(app)
                        .corporate(app.getCorporate())
                        .path("/" + System.currentTimeMillis()) // Temporary path, will be updated by trigger
                        .level(0)
                        .sortOrder(0)
                        .build();
                    
                    rootFolder = folderRepository.save(rootFolder);
                    totalFoldersCreated++;
                    
                    log.info("Created root folder (ID: {}) for application: {} (ID: {})", 
                             rootFolder.getId(), app.getName(), app.getId());
                    

                }
                
                // Find templates that need to be migrated for this application
                List<Template> templatesToMigrate = templateRepository.findByApp_Id(app.getId())
                    .stream()
                    .filter(template -> template.getFolder() == null || 
                                      !folderRepository.existsById(template.getFolder().getId()))
                    .toList();
                
                // Move templates to root folder
                for (Template template : templatesToMigrate) {
                    template.setFolder(rootFolder);
                    templateRepository.save(template);
                    totalTemplatesMigrated++;
                }
                
                if (!templatesToMigrate.isEmpty()) {
                    log.info("Migrated {} templates to root folder for application: {}", 
                             templatesToMigrate.size(), app.getName());
                }
                
            } catch (Exception e) {
                log.error("Error migrating templates for application: {} (ID: {})", 
                          app.getName(), app.getId(), e);
            }
        }
        
        // Verify migration results
        long orphanedTemplates = templateRepository.findAll()
            .stream()
            .filter(template -> template.getFolder() == null)
            .count();
        
        log.info("=== MIGRATION COMPLETED ===");
        log.info("Total folders created: {}", totalFoldersCreated);
        log.info("Total templates migrated: {}", totalTemplatesMigrated);
        log.info("Orphaned templates remaining: {}", orphanedTemplates);
        
        if (orphanedTemplates == 0) {
            log.info("SUCCESS: All templates have been assigned to folders");
        } else {
            log.warn("WARNING: {} templates still have no folder assignment", orphanedTemplates);
        }
    }
    

    
    /**
     * Validates the migration by checking for data integrity issues.
     */
    @Transactional(readOnly = true)
    public MigrationValidationResult validateMigration() {
        log.info("Validating template folder migration...");
        
        // Count orphaned templates
        long orphanedTemplates = templateRepository.findAll()
            .stream()
            .filter(template -> template.getFolder() == null)
            .count();
        
        // Count folders without proper paths
        long foldersWithoutPath = folderRepository.findAll()
            .stream()
            .filter(folder -> folder.getPath() == null || folder.getPath().isEmpty())
            .count();
        
        // Count total entities
        long totalFolders = folderRepository.count();
        long totalTemplates = templateRepository.count();
        
        MigrationValidationResult result = MigrationValidationResult.builder()
            .totalFolders(totalFolders)
            .totalTemplates(totalTemplates)
            .totalPermissions(0)
            .orphanedTemplates(orphanedTemplates)
            .foldersWithoutPath(foldersWithoutPath)
            .isValid(orphanedTemplates == 0 && foldersWithoutPath == 0)
            .build();
        
        log.info("Validation results: {}", result);
        return result;
    }
    
    /**
     * Result object for migration validation.
     */
    @lombok.Data
    @lombok.Builder
    public static class MigrationValidationResult {
        private long totalFolders;
        private long totalTemplates;
        private long totalPermissions;
        private long orphanedTemplates;
        private long foldersWithoutPath;
        private boolean isValid;
        
        @Override
        public String toString() {
            return String.format(
                "MigrationValidationResult{folders=%d, templates=%d, permissions=%d, " +
                "orphaned=%d, invalidPaths=%d, valid=%s}",
                totalFolders, totalTemplates, totalPermissions, 
                orphanedTemplates, foldersWithoutPath, isValid
            );
        }
    }
}
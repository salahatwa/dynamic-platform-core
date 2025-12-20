package com.platform.controller;

import com.platform.service.TemplateFolderMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/template-folder-migration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Template Folder Migration", description = "Template folder migration management")
public class TemplateFolderMigrationController {
    
    private final TemplateFolderMigrationService migrationService;
    
    @PostMapping("/migrate")
    @Operation(summary = "Run template folder migration", 
               description = "Migrates existing templates to the new folder structure")
    public ResponseEntity<String> runMigration() {
        try {
            log.info("Manual migration requested");
            migrationService.migrateExistingTemplates();
            return ResponseEntity.ok("Migration completed successfully");
        } catch (Exception e) {
            log.error("Migration failed", e);
            return ResponseEntity.internalServerError()
                .body("Migration failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/validate")
    @Operation(summary = "Validate migration", 
               description = "Validates the current state of template folder migration")
    public ResponseEntity<TemplateFolderMigrationService.MigrationValidationResult> validateMigration() {
        try {
            TemplateFolderMigrationService.MigrationValidationResult result = 
                migrationService.validateMigration();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
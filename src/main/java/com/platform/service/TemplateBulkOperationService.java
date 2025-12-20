package com.platform.service;

import com.platform.dto.BulkOperationRequest;
import com.platform.dto.BulkOperationResponse;
import com.platform.entity.Template;
import com.platform.entity.TemplateFolder;
import com.platform.repository.TemplateRepository;
import com.platform.repository.TemplateFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateBulkOperationService {
    
    private final TemplateRepository templateRepository;
    private final TemplateFolderRepository folderRepository;
    
    @Transactional
    public BulkOperationResponse performBulkOperation(BulkOperationRequest request, Long applicationId) {
        List<BulkOperationResponse.OperationResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        // Validate target folder exists and belongs to the same application
        TemplateFolder targetFolder = null;
        if (request.getOperation() != BulkOperationRequest.BulkOperationType.DELETE) {
            Optional<TemplateFolder> folderOpt = folderRepository.findByIdAndApplicationId(
                request.getTargetFolderId(), applicationId);
            if (folderOpt.isEmpty()) {
                return BulkOperationResponse.builder()
                    .totalItems(request.getTemplateIds().size())
                    .successCount(0)
                    .failureCount(request.getTemplateIds().size())
                    .message("Target folder not found or access denied")
                    .results(new ArrayList<>())
                    .build();
            }
            targetFolder = folderOpt.get();
        }
        
        for (Long templateId : request.getTemplateIds()) {
            try {
                Optional<Template> templateOpt = templateRepository.findByIdAndApp_Id(templateId, applicationId);
                if (templateOpt.isEmpty()) {
                    results.add(BulkOperationResponse.OperationResult.builder()
                        .templateId(templateId)
                        .templateName("Unknown")
                        .success(false)
                        .errorMessage("Template not found or access denied")
                        .build());
                    failureCount++;
                    continue;
                }
                
                Template template = templateOpt.get();
                boolean success = false;
                String errorMessage = null;
                
                switch (request.getOperation()) {
                    case MOVE:
                        success = moveTemplate(template, targetFolder);
                        break;
                    case COPY:
                        success = copyTemplate(template, targetFolder);
                        break;
                    case DELETE:
                        success = deleteTemplate(template);
                        break;
                }
                
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                    errorMessage = "Operation failed";
                }
                
                results.add(BulkOperationResponse.OperationResult.builder()
                    .templateId(templateId)
                    .templateName(template.getName())
                    .success(success)
                    .errorMessage(errorMessage)
                    .build());
                    
            } catch (Exception e) {
                log.error("Error processing template {}: {}", templateId, e.getMessage(), e);
                results.add(BulkOperationResponse.OperationResult.builder()
                    .templateId(templateId)
                    .templateName("Unknown")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
                failureCount++;
            }
        }
        
        String message = String.format("Bulk %s completed: %d successful, %d failed", 
            request.getOperation().name().toLowerCase(), successCount, failureCount);
        
        return BulkOperationResponse.builder()
            .totalItems(request.getTemplateIds().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .results(results)
            .message(message)
            .build();
    }
    
    private boolean moveTemplate(Template template, TemplateFolder targetFolder) {
        try {
            template.setFolder(targetFolder);
            templateRepository.save(template);
            return true;
        } catch (Exception e) {
            log.error("Failed to move template {}: {}", template.getId(), e.getMessage());
            return false;
        }
    }
    
    private boolean copyTemplate(Template template, TemplateFolder targetFolder) {
        try {
            Template copy = Template.builder()
                .name(template.getName() + " (Copy)")
                .type(template.getType())
                .htmlContent(template.getHtmlContent())
                .cssStyles(template.getCssStyles())
                .customFonts(template.getCustomFonts())
                .parameters(template.getParameters())
                .subject(template.getSubject())
                .pageOrientation(template.getPageOrientation())
                .corporate(template.getCorporate())
                .app(template.getApp())
                .folder(targetFolder)
                .build();
            
            templateRepository.save(copy);
            return true;
        } catch (Exception e) {
            log.error("Failed to copy template {}: {}", template.getId(), e.getMessage());
            return false;
        }
    }
    
    private boolean deleteTemplate(Template template) {
        try {
            templateRepository.delete(template);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete template {}: {}", template.getId(), e.getMessage());
            return false;
        }
    }
}
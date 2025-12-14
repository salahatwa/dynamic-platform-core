package com.platform.controller.content;

import com.platform.dto.TemplateContentResponse;
import com.platform.entity.Template;
import com.platform.enums.TemplateType;
import com.platform.repository.TemplateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content/templates")
@RequiredArgsConstructor
@Tag(name = "Content API - Templates", description = "Public API for accessing templates with API key authentication")
@SecurityRequirement(name = "ApiKey")
public class ContentTemplateController {

    private final TemplateRepository templateRepository;

    @GetMapping("/page")
    @Operation(
        summary = "Get templates for app",
        description = "Retrieve templates for the authenticated app. Supports filtering, sorting, and pagination."
    )
    public ResponseEntity<Page<TemplateContentResponse>> getTemplates(
            @Parameter(description = "Filter by template type (EMAIL, PDF, HTML, WORD)") @RequestParam(required = false) String type,
            @Parameter(description = "Search in template name") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (name, type, createdAt, updatedAt)") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        // Create sort
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Long appId = getCurrentAppId();
        
        Page<Template> templates;
        
        if (type != null && search != null) {
            TemplateType templateType = TemplateType.valueOf(type.toUpperCase());
            templates = templateRepository.findByApp_IdAndTypeAndNameContainingIgnoreCase(appId, templateType, search, pageable);
        } else if (type != null) {
            TemplateType templateType = TemplateType.valueOf(type.toUpperCase());
            templates = templateRepository.findByApp_IdAndType(appId, templateType, pageable);
        } else if (search != null) {
            templates = templateRepository.findByApp_IdAndNameContainingIgnoreCase(appId, search, pageable);
        } else {
            templates = templateRepository.findByApp_Id(appId, pageable);
        }

        Page<TemplateContentResponse> response = templates.map(this::mapToContentResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all templates for app",
        description = "Retrieve all templates for the authenticated app. Supports filtering and sorting but returns complete list without pagination."
    )
    public ResponseEntity<List<TemplateContentResponse>> getAllTemplates(
            @Parameter(description = "Filter by template type (EMAIL, PDF, HTML, WORD)") @RequestParam(required = false) String type,
            @Parameter(description = "Search in template name") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (name, type, createdAt, updatedAt)") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Create sort
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);

        Long appId = getCurrentAppId();
        
        // Use a large page size to get all results, then extract content
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<Template> templatesPage;
        
        if (type != null && search != null) {
            TemplateType templateType = TemplateType.valueOf(type.toUpperCase());
            templatesPage = templateRepository.findByApp_IdAndTypeAndNameContainingIgnoreCase(appId, templateType, search, pageable);
        } else if (type != null) {
            TemplateType templateType = TemplateType.valueOf(type.toUpperCase());
            templatesPage = templateRepository.findByApp_IdAndType(appId, templateType, pageable);
        } else if (search != null) {
            templatesPage = templateRepository.findByApp_IdAndNameContainingIgnoreCase(appId, search, pageable);
        } else {
            templatesPage = templateRepository.findByApp_Id(appId, pageable);
        }

        List<TemplateContentResponse> response = templatesPage.getContent().stream()
            .map(this::mapToContentResponse)
            .toList();
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get specific template",
        description = "Retrieve a specific template by ID"
    )
    public ResponseEntity<TemplateContentResponse> getTemplate(
            @Parameter(description = "Template ID") @PathVariable Long id) {

        Long appId = getCurrentAppId();
        
        Template template = templateRepository.findByIdAndApp_Id(id, appId)
            .orElseThrow(() -> new RuntimeException("Template not found"));

        return ResponseEntity.ok(mapToContentResponse(template));
    }

    @GetMapping("/types")
    @Operation(
        summary = "Get template types",
        description = "Retrieve all available template types"
    )
    public ResponseEntity<List<TemplateType>> getTemplateTypes() {
        return ResponseEntity.ok(List.of(TemplateType.values()));
    }

    private TemplateContentResponse mapToContentResponse(Template template) {
        return TemplateContentResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .type(template.getType())
            .htmlContent(template.getHtmlContent())
            .cssStyles(template.getCssStyles())
            .customFonts(template.getCustomFonts() != null ? template.getCustomFonts().toString() : null)
            .parameters(template.getParameters() != null ? template.getParameters().toString() : null)
            .subject(template.getSubject())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }

    private Long getCurrentAppId() {
        return (Long) org.springframework.web.context.request.RequestContextHolder
            .currentRequestAttributes()
            .getAttribute("appId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
    }
}
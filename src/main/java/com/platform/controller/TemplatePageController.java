package com.platform.controller;

import com.platform.dto.TemplatePageRequest;
import com.platform.dto.TemplatePageResponse;
import com.platform.entity.Template;
import com.platform.entity.TemplatePage;
import com.platform.entity.User;
import com.platform.repository.TemplateRepository;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TemplatePageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates/{templateId}/pages")
@RequiredArgsConstructor
@Tag(name = "Template Pages", description = "Template page management")
public class TemplatePageController {
    
    private final TemplatePageService pageService;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    
    @GetMapping
    @Operation(summary = "Get all pages for template")
    public ResponseEntity<?> getAllPages(@PathVariable Long templateId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        List<TemplatePage> pages = pageService.getAllByTemplate(templateId);
        List<TemplatePageResponse> response = pages.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{pageId}")
    @Operation(summary = "Get page by ID")
    public ResponseEntity<?> getPage(@PathVariable Long templateId, @PathVariable Long pageId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplatePage page = pageService.getById(pageId)
            .orElseThrow(() -> new RuntimeException("Page not found"));
        
        if (!page.getTemplate().getId().equals(templateId)) {
            return ResponseEntity.status(403).body("Access denied: Page does not belong to this template");
        }
        
        return ResponseEntity.ok(toResponse(page));
    }
    
    @PostMapping
    @Operation(summary = "Create page")
    public ResponseEntity<?> createPage(@PathVariable Long templateId, @Valid @RequestBody TemplatePageRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplatePage page = TemplatePage.builder()
            .name(request.getName())
            .content(request.getContent())
            .pageOrder(request.getPageOrder())
            .build();
        
        TemplatePage created = pageService.create(page, template);
        return ResponseEntity.ok(toResponse(created));
    }
    
    @PutMapping("/{pageId}")
    @Operation(summary = "Update page")
    public ResponseEntity<?> updatePage(@PathVariable Long templateId, @PathVariable Long pageId, 
                                       @Valid @RequestBody TemplatePageRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplatePage page = pageService.getById(pageId)
            .orElseThrow(() -> new RuntimeException("Page not found"));
        
        if (!page.getTemplate().getId().equals(templateId)) {
            return ResponseEntity.status(403).body("Access denied: Page does not belong to this template");
        }
        
        page.setName(request.getName());
        page.setContent(request.getContent());
        if (request.getPageOrder() != null) {
            page.setPageOrder(request.getPageOrder());
        }
        
        TemplatePage updated = pageService.update(page);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{pageId}")
    @Operation(summary = "Delete page")
    public ResponseEntity<?> deletePage(@PathVariable Long templateId, @PathVariable Long pageId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplatePage page = pageService.getById(pageId)
            .orElseThrow(() -> new RuntimeException("Page not found"));
        
        if (!page.getTemplate().getId().equals(templateId)) {
            return ResponseEntity.status(403).body("Access denied: Page does not belong to this template");
        }
        
        pageService.delete(pageId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reorder")
    @Operation(summary = "Reorder pages")
    public ResponseEntity<?> reorderPages(@PathVariable Long templateId, @RequestBody List<Long> pageIds) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        pageService.reorderPages(templateId, pageIds);
        return ResponseEntity.ok().build();
    }
    
    private TemplatePageResponse toResponse(TemplatePage page) {
        return TemplatePageResponse.builder()
            .id(page.getId())
            .templateId(page.getTemplate().getId())
            .name(page.getName())
            .content(page.getContent())
            .pageOrder(page.getPageOrder())
            .createdAt(page.getCreatedAt())
            .updatedAt(page.getUpdatedAt())
            .build();
    }
    
    private User getCurrentUserWithCorporate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userRepository.findByIdWithCorporate(userPrincipal.getId()).orElse(null);
        }
        return null;
    }
}

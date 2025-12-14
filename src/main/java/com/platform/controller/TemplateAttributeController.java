package com.platform.controller;

import com.platform.dto.TemplateAttributeRequest;
import com.platform.dto.TemplateAttributeResponse;
import com.platform.entity.Template;
import com.platform.entity.TemplateAttribute;
import com.platform.entity.User;
import com.platform.repository.TemplateRepository;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TemplateAttributeService;
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
@RequestMapping("/api/templates/{templateId}/attributes")
@RequiredArgsConstructor
@Tag(name = "Template Attributes", description = "Template attribute management")
public class TemplateAttributeController {
    
    private final TemplateAttributeService attributeService;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    
    @GetMapping
    @Operation(summary = "Get all attributes for template")
    public ResponseEntity<?> getAllAttributes(@PathVariable Long templateId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        List<TemplateAttribute> attributes = attributeService.getAllByTemplate(templateId);
        List<TemplateAttributeResponse> response = attributes.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{attributeId}")
    @Operation(summary = "Get attribute by ID")
    public ResponseEntity<?> getAttribute(@PathVariable Long templateId, @PathVariable Long attributeId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplateAttribute attribute = attributeService.getById(attributeId)
            .orElseThrow(() -> new RuntimeException("Attribute not found"));
        
        if (!attribute.getTemplate().getId().equals(templateId)) {
            return ResponseEntity.status(403).body("Access denied: Attribute does not belong to this template");
        }
        
        return ResponseEntity.ok(toResponse(attribute));
    }
    
    @PostMapping
    @Operation(summary = "Create attribute")
    public ResponseEntity<?> createAttribute(@PathVariable Long templateId, @Valid @RequestBody TemplateAttributeRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        if (attributeService.keyExistsForTemplate(templateId, request.getAttributeKey(), null)) {
            return ResponseEntity.badRequest().body("Attribute key already exists for this template");
        }
        
        TemplateAttribute attribute = TemplateAttribute.builder()
            .attributeKey(request.getAttributeKey())
            .attributeValue(request.getAttributeValue())
            .attributeType(request.getAttributeType())
            .description(request.getDescription())
            .build();
        
        TemplateAttribute created = attributeService.create(attribute, template);
        return ResponseEntity.ok(toResponse(created));
    }
    
    @PutMapping("/{attributeId}")
    @Operation(summary = "Update attribute")
    public ResponseEntity<?> updateAttribute(@PathVariable Long templateId, @PathVariable Long attributeId,
                                            @Valid @RequestBody TemplateAttributeRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplateAttribute attribute = attributeService.getById(attributeId)
            .orElseThrow(() -> new RuntimeException("Attribute not found"));
        
        if (!attribute.getTemplate().getId().equals(templateId)) {
            return ResponseEntity.status(403).body("Access denied: Attribute does not belong to this template");
        }
        
        if (attributeService.keyExistsForTemplate(templateId, request.getAttributeKey(), attributeId)) {
            return ResponseEntity.badRequest().body("Attribute key already exists for this template");
        }
        
        attribute.setAttributeKey(request.getAttributeKey());
        attribute.setAttributeValue(request.getAttributeValue());
        attribute.setAttributeType(request.getAttributeType());
        attribute.setDescription(request.getDescription());
        
        TemplateAttribute updated = attributeService.update(attribute);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{attributeId}")
    @Operation(summary = "Delete attribute")
    public ResponseEntity<?> deleteAttribute(@PathVariable Long templateId, @PathVariable Long attributeId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
        }
        
        TemplateAttribute attribute = attributeService.getById(attributeId)
            .orElseThrow(() -> new RuntimeException("Attribute not found"));
        
        if (!attribute.getTemplate().getId().equals(templateId)) {
            return ResponseEntity.status(403).body("Access denied: Attribute does not belong to this template");
        }
        
        attributeService.delete(attributeId);
        return ResponseEntity.ok().build();
    }
    
    private TemplateAttributeResponse toResponse(TemplateAttribute attribute) {
        return TemplateAttributeResponse.builder()
            .id(attribute.getId())
            .templateId(attribute.getTemplate().getId())
            .attributeKey(attribute.getAttributeKey())
            .attributeValue(attribute.getAttributeValue())
            .attributeType(attribute.getAttributeType())
            .description(attribute.getDescription())
            .createdAt(attribute.getCreatedAt())
            .updatedAt(attribute.getUpdatedAt())
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

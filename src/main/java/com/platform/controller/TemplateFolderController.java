package com.platform.controller;

import com.platform.dto.TemplateFolderRequest;
import com.platform.dto.TemplateFolderResponse;
import com.platform.entity.TemplateFolder;
import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TemplateFolderService;
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
@RequestMapping("/api/template-folders")
@RequiredArgsConstructor
@Tag(name = "Template Folders", description = "Template folder management")
public class TemplateFolderController {
    
    private final TemplateFolderService folderService;
    private final UserRepository userRepository;
    
    @GetMapping
    @Operation(summary = "Get all template folders")
    public ResponseEntity<?> getAllFolders() {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        List<TemplateFolder> folders = folderService.getAllByCorporate(currentUser.getCorporate().getId());
        List<TemplateFolderResponse> response = folders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/root")
    @Operation(summary = "Get root folders")
    public ResponseEntity<?> getRootFolders() {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        List<TemplateFolder> folders = folderService.getRootFoldersByCorporate(currentUser.getCorporate().getId());
        List<TemplateFolderResponse> response = folders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get folder by ID")
    public ResponseEntity<?> getFolder(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TemplateFolder folder = folderService.getById(id)
            .orElseThrow(() -> new RuntimeException("Folder not found"));
        
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Folder belongs to another organization");
        }
        
        return ResponseEntity.ok(toResponse(folder));
    }
    
    @GetMapping("/{id}/children")
    @Operation(summary = "Get sub-folders")
    public ResponseEntity<?> getSubFolders(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        if (!folderService.belongsToCorporate(id, currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Folder belongs to another organization");
        }
        
        List<TemplateFolder> folders = folderService.getSubFolders(id);
        List<TemplateFolderResponse> response = folders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "Create folder")
    public ResponseEntity<?> createFolder(@Valid @RequestBody TemplateFolderRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TemplateFolder folder = TemplateFolder.builder()
            .name(request.getName())
            .build();
        
        if (request.getParentId() != null) {
            TemplateFolder parent = folderService.getById(request.getParentId())
                .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            
            if (!parent.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
                return ResponseEntity.status(403).body("Access denied: Parent folder belongs to another organization");
            }
            
            folder.setParent(parent);
        }
        
        TemplateFolder created = folderService.create(folder, currentUser.getCorporate());
        return ResponseEntity.ok(toResponse(created));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update folder")
    public ResponseEntity<?> updateFolder(@PathVariable Long id, @Valid @RequestBody TemplateFolderRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TemplateFolder folder = folderService.getById(id)
            .orElseThrow(() -> new RuntimeException("Folder not found"));
        
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Folder belongs to another organization");
        }
        
        folder.setName(request.getName());
        
        if (request.getParentId() != null) {
            TemplateFolder parent = folderService.getById(request.getParentId())
                .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            
            if (!parent.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
                return ResponseEntity.status(403).body("Access denied: Parent folder belongs to another organization");
            }
            
            folder.setParent(parent);
        } else {
            folder.setParent(null);
        }
        
        TemplateFolder updated = folderService.update(folder);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete folder")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TemplateFolder folder = folderService.getById(id)
            .orElseThrow(() -> new RuntimeException("Folder not found"));
        
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Folder belongs to another organization");
        }
        
        folderService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    private TemplateFolderResponse toResponse(TemplateFolder folder) {
        return TemplateFolderResponse.builder()
            .id(folder.getId())
            .name(folder.getName())
            .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
            .templatesCount(folderService.countTemplatesInFolder(folder.getId()))
            .createdAt(folder.getCreatedAt())
            .updatedAt(folder.getUpdatedAt())
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

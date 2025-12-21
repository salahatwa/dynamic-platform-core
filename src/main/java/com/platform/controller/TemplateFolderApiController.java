package com.platform.controller;

import com.platform.dto.*;
import com.platform.entity.App;
import com.platform.entity.Template;
import com.platform.entity.TemplateFolder;
import com.platform.entity.User;
import com.platform.repository.AppRepository;
import com.platform.repository.TemplateRepository;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TemplateBulkOperationService;
import com.platform.service.TemplateFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/template-folders")
@RequiredArgsConstructor
@Tag(name = "Template Folder Management", description = "Enhanced template folder management with application context")
public class TemplateFolderApiController {
    
    private final TemplateFolderService folderService;
    private final TemplateBulkOperationService bulkOperationService;
    private final TemplateRepository templateRepository;
    private final AppRepository appRepository;
    private final UserRepository userRepository;
    
    @GetMapping("/root")
    @Operation(summary = "Get root folders for current user's applications")
    public ResponseEntity<?> getRootFolders() {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Get all active applications for the user's corporate
        List<App> userApps = appRepository.findActiveByCorporateId(currentUser.getCorporate().getId());
        List<TemplateFolderResponse> allRootFolders = new ArrayList<>();
        
        // Get root folders for each application
        for (App app : userApps) {
            List<TemplateFolder> rootFolders = folderService.getRootFoldersByApplication(app.getId());
            allRootFolders.addAll(rootFolders.stream().map(this::toResponse).collect(Collectors.toList()));
        }
        
        return ResponseEntity.ok(allRootFolders);
    }

    @GetMapping("/tree/{applicationId}")
    @Operation(summary = "Get hierarchical folder tree for application")
    public ResponseEntity<?> getFolderTree(@PathVariable Long applicationId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application belongs to user's corporate
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Application not found or belongs to another organization");
        }
        
        FolderTreeResponse treeResponse = folderService.getFolderTree(applicationId);
        return ResponseEntity.ok(treeResponse);
    }
    
    @PostMapping
    @Operation(summary = "Create folder in application")
    public ResponseEntity<?> createFolder(@Valid @RequestBody TemplateFolderRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application belongs to user's corporate
        Optional<App> appOpt = appRepository.findById(request.getApplicationId());
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Application not found or belongs to another organization");
        }
        
        App application = appOpt.get();
        
        // Validate parent folder if specified
        TemplateFolder parent = null;
        if (request.getParentId() != null) {
            Optional<TemplateFolder> parentOpt = folderService.getByIdAndApplication(request.getParentId(), request.getApplicationId());
            if (parentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Parent folder not found");
            }
            parent = parentOpt.get();
        }
        
        // Check for duplicate names at the same level
        Optional<TemplateFolder> existing = folderService.findByApplicationIdAndNameAndParentId(
            request.getApplicationId(), request.getName(), request.getParentId());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Folder with this name already exists at this level");
        }
        
        TemplateFolder folder = TemplateFolder.builder()
            .name(request.getName())
            .parent(parent)
            .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
            .active(request.getActive() != null ? request.getActive() : true)
            .description(request.getDescription())
            .imageUrl(request.getImageUrl())
            .build();
        
        TemplateFolder created = folderService.createInApplication(folder, application, currentUser.getCorporate());
        return ResponseEntity.ok(toResponse(created));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update folder")
    public ResponseEntity<?> updateFolder(@PathVariable Long id, @Valid @RequestBody TemplateFolderRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Optional<TemplateFolder> folderOpt = folderService.getByIdAndApplication(id, request.getApplicationId());
        if (folderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Folder not found");
        }
        
        TemplateFolder folder = folderOpt.get();
        
        // Validate corporate access
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        // Check for duplicate names if name is changing
        if (!folder.getName().equals(request.getName())) {
            Optional<TemplateFolder> existing = folderService.findByApplicationIdAndNameAndParentId(
                request.getApplicationId(), request.getName(), 
                folder.getParent() != null ? folder.getParent().getId() : null);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body("Folder with this name already exists at this level");
            }
        }
        
        folder.setName(request.getName());
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            folder.setActive(request.getActive());
        }
        folder.setDescription(request.getDescription());
        folder.setImageUrl(request.getImageUrl());
        
        TemplateFolder updated = folderService.update(folder);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @PutMapping("/{id}/move")
    @Operation(summary = "Move folder to different parent")
    public ResponseEntity<?> moveFolder(@PathVariable Long id, @Valid @RequestBody MoveFolderRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Get the folder and validate access
        Optional<TemplateFolder> folderOpt = folderService.getById(id);
        if (folderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Folder not found");
        }
        
        TemplateFolder folder = folderOpt.get();
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        Long applicationId = folder.getApplication().getId();
        
        try {
            TemplateFolder moved = folderService.moveFolder(id, request.getTargetParentId(), applicationId);
            return ResponseEntity.ok(toResponse(moved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to move folder: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle folder active/inactive status")
    public ResponseEntity<?> toggleFolderStatus(@PathVariable Long id, @RequestParam Long applicationId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Optional<TemplateFolder> folderOpt = folderService.getByIdAndApplication(id, applicationId);
        if (folderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Folder not found");
        }
        
        TemplateFolder folder = folderOpt.get();
        
        // Validate corporate access
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        // Toggle the active status
        folder.setActive(!folder.getActive());
        
        TemplateFolder updated = folderService.update(folder);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete folder")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id, @RequestParam Long applicationId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Optional<TemplateFolder> folderOpt = folderService.getByIdAndApplication(id, applicationId);
        if (folderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Folder not found");
        }
        
        TemplateFolder folder = folderOpt.get();
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        // Check if folder is empty
        long templateCount = folderService.countTemplatesInFolder(id);
        long subfolderCount = folderService.countSubfoldersInFolder(id);
        
        if (templateCount > 0 || subfolderCount > 0) {
            return ResponseEntity.badRequest().body(
                String.format("Cannot delete non-empty folder. Contains %d templates and %d subfolders", 
                    templateCount, subfolderCount));
        }
        
        try {
            folderService.deleteByIdAndApplication(id, applicationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete folder: " + e.getMessage());
        }
    }
    
    @GetMapping("/{folderId}/templates")
    @Operation(summary = "Get templates in folder with pagination")
    public ResponseEntity<?> getFolderTemplates(
            @PathVariable Long folderId,
            @RequestParam Long applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate folder access
        Optional<TemplateFolder> folderOpt = folderService.getByIdAndApplication(folderId, applicationId);
        if (folderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Folder not found");
        }
        
        TemplateFolder folder = folderOpt.get();
        if (!folder.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        // Get subfolders (always show all subfolders first, then templates)
        List<TemplateFolder> allSubfolders = folderService.getSubFoldersByApplication(applicationId, folderId);
        
        // Filter subfolders by search if provided
        List<TemplateFolder> filteredSubfolders = allSubfolders;
        if (search != null && !search.trim().isEmpty()) {
            filteredSubfolders = allSubfolders.stream()
                .filter(subfolder -> subfolder.getName().toLowerCase().contains(search.trim().toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // Calculate pagination for combined content (subfolders + templates)
        int totalSubfolders = filteredSubfolders.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalSubfolders);
        
        List<TemplateFolder> paginatedSubfolders = new ArrayList<>();
        Page<Template> templates;
        long totalElements = totalSubfolders;
        
        // Create a default pageable for empty template pages
        Pageable defaultPageable = PageRequest.of(0, size, 
            Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
        
        if (startIndex < totalSubfolders) {
            // We're still showing subfolders
            paginatedSubfolders = filteredSubfolders.subList(startIndex, endIndex);
            
            // If we have remaining space in this page, fetch templates
            int remainingSpace = size - paginatedSubfolders.size();
            if (remainingSpace > 0 && endIndex >= totalSubfolders) {
                // Calculate template pagination
                int templatePage = 0; // Always start from first page of templates
                Pageable templatePageable = PageRequest.of(templatePage, remainingSpace, 
                    Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
                
                if (search != null && !search.trim().isEmpty()) {
                    templates = templateRepository.findByFolderIdAndApp_IdAndNameContainingIgnoreCase(
                        folderId, applicationId, search.trim(), templatePageable);
                } else {
                    templates = templateRepository.findByFolderIdAndApp_Id(folderId, applicationId, templatePageable);
                }
                totalElements += templates.getTotalElements();
            } else {
                // Create empty page with proper pagination info
                templates = new PageImpl<>(new ArrayList<>(), defaultPageable, 0);
            }
        } else {
            // We're past all subfolders, show only templates
            int templatePage = (startIndex - totalSubfolders) / size;
            Pageable templatePageable = PageRequest.of(templatePage, size, 
                Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
            
            if (search != null && !search.trim().isEmpty()) {
                templates = templateRepository.findByFolderIdAndApp_IdAndNameContainingIgnoreCase(
                    folderId, applicationId, search.trim(), templatePageable);
            } else {
                templates = templateRepository.findByFolderIdAndApp_Id(folderId, applicationId, templatePageable);
            }
            totalElements += templates.getTotalElements();
        }
        
        // Build breadcrumbs
        List<FolderContentResponse.BreadcrumbItem> breadcrumbs = buildBreadcrumbs(folder);
        
        FolderContentResponse response = FolderContentResponse.builder()
            .currentFolder(toResponse(folder))
            .templates(templates.map(this::toTemplateResponse))
            .subfolders(paginatedSubfolders.stream().map(this::toResponse).collect(Collectors.toList()))
            .breadcrumbs(breadcrumbs)
            .totalItems(totalElements)
            .isLoading(false)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/root/{applicationId}/templates")
    @Operation(summary = "Get templates in root folder (no folder assigned)")
    public ResponseEntity<?> getRootTemplates(
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application access
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        // Get root folders (always show all folders first, then templates)
        List<TemplateFolder> allRootFolders = folderService.getRootFoldersByApplication(applicationId);
        
        // Filter folders by search if provided
        List<TemplateFolder> filteredFolders = allRootFolders;
        if (search != null && !search.trim().isEmpty()) {
            filteredFolders = allRootFolders.stream()
                .filter(folder -> folder.getName().toLowerCase().contains(search.trim().toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // Calculate pagination for combined content (folders + templates)
        int totalFolders = filteredFolders.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalFolders);
        
        List<TemplateFolder> paginatedFolders = new ArrayList<>();
        Page<Template> templates;
        long totalElements = totalFolders;
        
        // Create a default pageable for empty template pages
        Pageable defaultPageable = PageRequest.of(0, size, 
            Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
        
        if (startIndex < totalFolders) {
            // We're still showing folders
            paginatedFolders = filteredFolders.subList(startIndex, endIndex);
            
            // If we have remaining space in this page, fetch templates
            int remainingSpace = size - paginatedFolders.size();
            if (remainingSpace > 0 && endIndex >= totalFolders) {
                // Calculate template pagination
                int templatePage = 0; // Always start from first page of templates
                Pageable templatePageable = PageRequest.of(templatePage, remainingSpace, 
                    Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
                
                if (search != null && !search.trim().isEmpty()) {
                    templates = templateRepository.findByFolderIsNullAndApp_IdAndNameContainingIgnoreCase(
                        applicationId, search.trim(), templatePageable);
                } else {
                    templates = templateRepository.findByFolderIsNullAndApp_Id(applicationId, templatePageable);
                }
                totalElements += templates.getTotalElements();
            } else {
                // Create empty page with proper pagination info
                templates = new PageImpl<>(new ArrayList<>(), defaultPageable, 0);
            }
        } else {
            // We're past all folders, show only templates
            int templatePage = (startIndex - totalFolders) / size;
            Pageable templatePageable = PageRequest.of(templatePage, size, 
                Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
            
            if (search != null && !search.trim().isEmpty()) {
                templates = templateRepository.findByFolderIsNullAndApp_IdAndNameContainingIgnoreCase(
                    applicationId, search.trim(), templatePageable);
            } else {
                templates = templateRepository.findByFolderIsNullAndApp_Id(applicationId, templatePageable);
            }
            totalElements += templates.getTotalElements();
        }
        
        FolderContentResponse response = FolderContentResponse.builder()
            .currentFolder(null) // Root level
            .templates(templates.map(this::toTemplateResponse))
            .subfolders(paginatedFolders.stream().map(this::toResponse).collect(Collectors.toList()))
            .breadcrumbs(new ArrayList<>()) // Empty breadcrumbs for root
            .totalItems(totalElements)
            .isLoading(false)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/templates/bulk")
    @Operation(summary = "Perform bulk operations on templates")
    public ResponseEntity<?> bulkTemplateOperation(@Valid @RequestBody BulkOperationRequest request,
                                                  @RequestParam Long applicationId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application access
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        BulkOperationResponse response = bulkOperationService.performBulkOperation(request, applicationId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/{applicationId}")
    @Operation(summary = "Search templates and folders within application")
    public ResponseEntity<?> searchTemplatesAndFolders(
            @PathVariable Long applicationId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application access
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Template> searchResults = templateRepository.searchTemplatesAndFolders(applicationId, query, pageable);
        
        return ResponseEntity.ok(searchResults.map(this::toTemplateResponse));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search folders within application")
    public ResponseEntity<?> searchFolders(
            @RequestParam String query,
            @RequestParam Long applicationId) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application access
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<TemplateFolder> folders = folderService.searchFolders(query, applicationId);
        return ResponseEntity.ok(folders.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    
    @GetMapping("/search/suggestions")
    @Operation(summary = "Get search suggestions")
    public ResponseEntity<?> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam Long applicationId,
            @RequestParam(defaultValue = "5") int limit) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application access
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<String> suggestions = folderService.getSearchSuggestions(query, applicationId, limit);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/templates/search")
    @Operation(summary = "Search templates within application")
    public ResponseEntity<?> searchTemplates(
            @RequestParam String query,
            @RequestParam Long applicationId,
            @RequestParam(required = false) Long folderId) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Validate application access
        Optional<App> appOpt = appRepository.findById(applicationId);
        if (appOpt.isEmpty() || !appOpt.get().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<Template> templates;
        if (folderId != null) {
            templates = templateRepository.findByFolderIdAndApp_IdAndNameContainingIgnoreCase(
                folderId, applicationId, query, PageRequest.of(0, 50)).getContent();
        } else {
            templates = templateRepository.findByApp_IdAndNameContainingIgnoreCase(
                applicationId, query, PageRequest.of(0, 50)).getContent();
        }
        
        return ResponseEntity.ok(templates.stream()
            .map(this::toTemplateResponse)
            .collect(Collectors.toList()));
    }
    
    private List<FolderContentResponse.BreadcrumbItem> buildBreadcrumbs(TemplateFolder folder) {
        List<FolderContentResponse.BreadcrumbItem> breadcrumbs = new ArrayList<>();
        
        // Always add root as the first breadcrumb
        breadcrumbs.add(FolderContentResponse.BreadcrumbItem.builder()
            .id(null) // null ID represents root
            .name("Root")
            .path("/")
            .build());
        
        TemplateFolder current = folder;
        while (current != null) {
            breadcrumbs.add(FolderContentResponse.BreadcrumbItem.builder()
                .id(current.getId())
                .name(current.getName())
                .path(current.getPath())
                .build());
            current = current.getParent();
        }
        
        return breadcrumbs;
    }
    
    private TemplateFolderResponse toResponse(TemplateFolder folder) {
        return TemplateFolderResponse.builder()
            .id(folder.getId())
            .name(folder.getName())
            .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
            .applicationId(folder.getApplication() != null ? folder.getApplication().getId() : null)
            .corporateId(folder.getCorporate() != null ? folder.getCorporate().getId() : null)
            .path(folder.getPath())
            .level(folder.getLevel())
            .sortOrder(folder.getSortOrder())
            .active(folder.getActive())
            .description(folder.getDescription())
            .imageUrl(folder.getImageUrl())
            .templatesCount(folderService.countTemplatesInFolder(folder.getId()))
            .subfoldersCount(folderService.countSubfoldersInFolder(folder.getId()))
            .createdAt(folder.getCreatedAt())
            .updatedAt(folder.getUpdatedAt())
            .build();
    }
    

    
    private FolderContentResponse.TemplateResponse toTemplateResponse(Template template) {
        return FolderContentResponse.TemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .type(template.getType() != null ? template.getType().name() : null)
            .folderId(template.getFolder() != null ? template.getFolder().getId() : null)
            .folderPath(template.getFolder() != null ? template.getFolder().getPath() : "/")
            .applicationId(template.getApp() != null ? template.getApp().getId() : null)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .permissions(buildTemplatePermissions(template))
            .build();
    }
    
    private FolderContentResponse.TemplatePermissions buildTemplatePermissions(Template template) {
        // Return default permissions - no complex permission system needed
        return FolderContentResponse.TemplatePermissions.builder()
            .canView(true)
            .canEdit(true)
            .canDelete(true)
            .canCopy(true)
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
package com.platform.service;

import com.platform.dto.FolderTreeResponse;
import com.platform.entity.App;
import com.platform.entity.Corporate;
import com.platform.entity.TemplateFolder;
import com.platform.repository.TemplateFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateFolderService {
    
    private final TemplateFolderRepository folderRepository;
    
    // Application-scoped methods (new primary methods)
    @Transactional(readOnly = true)
    public List<TemplateFolder> getAllByApplication(Long applicationId) {
        return folderRepository.findByApplicationIdOrderByPath(applicationId);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateFolder> getRootFoldersByApplication(Long applicationId) {
        return folderRepository.findByApplicationIdAndParentIsNullOrderBySortOrder(applicationId);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateFolder> getActiveFoldersByApplication(Long applicationId) {
        return folderRepository.findByApplicationIdAndActiveOrderBySortOrder(applicationId, true);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateFolder> getActiveRootFoldersByApplication(Long applicationId) {
        return folderRepository.findByApplicationIdAndParentIsNullAndActiveOrderBySortOrder(applicationId, true);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateFolder> getSubFoldersByApplication(Long applicationId, Long parentId) {
        return folderRepository.findByApplicationIdAndParentIdOrderBySortOrder(applicationId, parentId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateFolder> getByIdAndApplication(Long id, Long applicationId) {
        return folderRepository.findByIdAndApplicationId(id, applicationId);
    }
    
    @Transactional(readOnly = true)
    public FolderTreeResponse getFolderTree(Long applicationId) {
        List<TemplateFolder> rootFolders = getRootFoldersByApplication(applicationId);
        List<FolderTreeResponse.FolderTreeNode> rootNodes = rootFolders.stream()
            .map(this::buildTreeNode)
            .collect(Collectors.toList());
        
        // For simplicity, we'll return the first root as the main root
        // In a real implementation, you might want to create a virtual root
        FolderTreeResponse.FolderTreeNode mainRoot = rootNodes.isEmpty() ? 
            FolderTreeResponse.FolderTreeNode.builder()
                .id(null)
                .name("Root")
                .children(rootNodes)
                .build() :
            rootNodes.get(0);
            
        if (rootNodes.size() > 1) {
            mainRoot = FolderTreeResponse.FolderTreeNode.builder()
                .id(null)
                .name("Root")
                .children(rootNodes)
                .build();
        }
        
        return FolderTreeResponse.builder()
            .rootFolder(mainRoot)
            .totalFolders((long) getAllByApplication(applicationId).size())
            .build();
    }
    
    private FolderTreeResponse.FolderTreeNode buildTreeNode(TemplateFolder folder) {
        List<FolderTreeResponse.FolderTreeNode> childNodes = folder.getChildren().stream()
            .map(this::buildTreeNode)
            .collect(Collectors.toList());
            
        return FolderTreeResponse.FolderTreeNode.builder()
            .id(folder.getId())
            .name(folder.getName())
            .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
            .applicationId(folder.getApplication() != null ? folder.getApplication().getId() : null)
            .path(folder.getPath())
            .level(folder.getLevel())
            .sortOrder(folder.getSortOrder())
            .children(childNodes)
            .hasChildren(!childNodes.isEmpty())
            .templateCount(countTemplatesInFolder(folder.getId()))
            .subfolderCount(countSubfoldersInFolder(folder.getId()))
            .permissions(buildFolderPermissions(folder))
            .build();
    }
    
    private FolderTreeResponse.FolderPermissions buildFolderPermissions(TemplateFolder folder) {
        // Return default permissions since folder permissions are not implemented
        return FolderTreeResponse.FolderPermissions.builder()
            .canView(true)
            .canCreate(true)
            .canEdit(true)
            .canDelete(true)
            .build();
    }
    
    @Transactional
    public TemplateFolder createInApplication(TemplateFolder folder, App application, Corporate corporate) {
        folder.setApplication(application);
        folder.setCorporate(corporate);
        
        // Calculate path and level
        if (folder.getParent() != null) {
            TemplateFolder parent = folder.getParent();
            folder.setPath(parent.getPath() + "/" + folder.getName());
            folder.setLevel(parent.getLevel() + 1);
        } else {
            folder.setPath("/" + folder.getName());
            folder.setLevel(0);
        }
        
        return folderRepository.save(folder);
    }
    
    @Transactional
    public TemplateFolder moveFolder(Long folderId, Long targetParentId, Long applicationId) {
        TemplateFolder folder = getByIdAndApplication(folderId, applicationId)
            .orElseThrow(() -> new RuntimeException("Folder not found"));
            
        TemplateFolder targetParent = null;
        if (targetParentId != null) {
            targetParent = getByIdAndApplication(targetParentId, applicationId)
                .orElseThrow(() -> new RuntimeException("Target parent folder not found"));
        }
        
        folder.setParent(targetParent);
        updatePathAndLevel(folder);
        
        return folderRepository.save(folder);
    }
    
    private void updatePathAndLevel(TemplateFolder folder) {
        if (folder.getParent() != null) {
            folder.setPath(folder.getParent().getPath() + "/" + folder.getName());
            folder.setLevel(folder.getParent().getLevel() + 1);
        } else {
            folder.setPath("/" + folder.getName());
            folder.setLevel(0);
        }
        
        // Update all descendants
        for (TemplateFolder child : folder.getChildren()) {
            updatePathAndLevel(child);
            folderRepository.save(child);
        }
    }
    
    public boolean belongsToApplication(Long folderId, Long applicationId) {
        return folderRepository.findByIdAndApplicationId(folderId, applicationId).isPresent();
    }
    
    @Transactional
    public void deleteByIdAndApplication(Long id, Long applicationId) {
        if (folderRepository.existsByIdAndApplicationId(id, applicationId)) {
            folderRepository.deleteByIdAndApplicationId(id, applicationId);
        } else {
            throw new RuntimeException("Folder not found or access denied");
        }
    }
    
    // Legacy corporate-scoped methods (for backward compatibility)
    @Deprecated
    @Transactional(readOnly = true)
    public List<TemplateFolder> getAllByCorporate(Long corporateId) {
        return folderRepository.findByCorporateId(corporateId);
    }
    
    @Deprecated
    @Transactional(readOnly = true)
    public List<TemplateFolder> getRootFoldersByCorporate(Long corporateId) {
        return folderRepository.findByCorporateIdAndParentIsNull(corporateId);
    }
    
    @Deprecated
    @Transactional(readOnly = true)
    public List<TemplateFolder> getSubFolders(Long parentId) {
        return folderRepository.findByParentId(parentId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateFolder> getById(Long id) {
        return folderRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateFolder> getByIdWithChildren(Long id) {
        return folderRepository.findByIdWithChildren(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateFolder> getByIdWithTemplates(Long id) {
        return folderRepository.findByIdWithTemplates(id);
    }
    
    @Deprecated
    @Transactional
    public TemplateFolder create(TemplateFolder folder, Corporate corporate) {
        folder.setCorporate(corporate);
        return folderRepository.save(folder);
    }
    
    @Transactional
    public TemplateFolder update(TemplateFolder folder) {
        return folderRepository.save(folder);
    }
    
    @Transactional
    public void delete(Long id) {
        folderRepository.deleteById(id);
    }
    
    public boolean belongsToCorporate(Long folderId, Long corporateId) {
        return folderRepository.findById(folderId)
            .map(folder -> folder.getCorporate().getId().equals(corporateId))
            .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public long countTemplatesInFolder(Long folderId) {
        return folderRepository.countTemplatesInFolder(folderId);
    }
    
    @Transactional(readOnly = true)
    public long countSubfoldersInFolder(Long folderId) {
        return folderRepository.countSubfoldersInFolder(folderId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateFolder> findByApplicationIdAndNameAndParentId(Long applicationId, String name, Long parentId) {
        return folderRepository.findByApplicationIdAndNameAndParentId(applicationId, name, parentId);
    }
    
    // Search methods
    @Transactional(readOnly = true)
    public List<TemplateFolder> searchFolders(String query, Long applicationId) {
        return folderRepository.findByApplicationIdAndNameContainingIgnoreCase(applicationId, query);
    }
    
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String query, Long applicationId, int limit) {
        List<TemplateFolder> folders = folderRepository.findByApplicationIdAndNameContainingIgnoreCase(applicationId, query);
        return folders.stream()
            .map(TemplateFolder::getName)
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }
}

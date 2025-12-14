package com.platform.service;

import com.platform.entity.Corporate;
import com.platform.entity.TemplateFolder;
import com.platform.repository.TemplateFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplateFolderService {
    
    private final TemplateFolderRepository folderRepository;
    
    @Transactional(readOnly = true)
    public List<TemplateFolder> getAllByCorporate(Long corporateId) {
        return folderRepository.findByCorporateId(corporateId);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateFolder> getRootFoldersByCorporate(Long corporateId) {
        return folderRepository.findByCorporateIdAndParentIsNull(corporateId);
    }
    
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
}

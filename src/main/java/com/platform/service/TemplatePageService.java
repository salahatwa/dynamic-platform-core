package com.platform.service;

import com.platform.entity.Template;
import com.platform.entity.TemplatePage;
import com.platform.repository.TemplatePageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplatePageService {
    
    private final TemplatePageRepository pageRepository;
    
    @Transactional(readOnly = true)
    public List<TemplatePage> getAllByTemplate(Long templateId) {
        return pageRepository.findByTemplateIdOrderByPageOrderAsc(templateId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplatePage> getById(Long id) {
        return pageRepository.findById(id);
    }
    
    @Transactional
    public TemplatePage create(TemplatePage page, Template template) {
        page.setTemplate(template);
        if (page.getPageOrder() == null) {
            Integer maxOrder = pageRepository.findMaxPageOrderByTemplateId(template.getId());
            page.setPageOrder(maxOrder != null ? maxOrder + 1 : 0);
        }
        return pageRepository.save(page);
    }
    
    @Transactional
    public TemplatePage update(TemplatePage page) {
        return pageRepository.save(page);
    }
    
    @Transactional
    public void delete(Long id) {
        pageRepository.deleteById(id);
    }
    
    @Transactional
    public void reorderPages(Long templateId, List<Long> pageIds) {
        for (int i = 0; i < pageIds.size(); i++) {
            pageRepository.updatePageOrder(pageIds.get(i), i);
        }
    }
    
    @Transactional
    public void deleteAllByTemplate(Long templateId) {
        pageRepository.deleteByTemplateId(templateId);
    }
    
    public boolean belongsToTemplate(Long pageId, Long templateId) {
        return pageRepository.findById(pageId)
            .map(page -> page.getTemplate().getId().equals(templateId))
            .orElse(false);
    }
}

package com.platform.service;

import com.platform.entity.Template;
import com.platform.entity.TemplateAttribute;
import com.platform.repository.TemplateAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplateAttributeService {
    
    private final TemplateAttributeRepository attributeRepository;
    
    @Transactional(readOnly = true)
    public List<TemplateAttribute> getAllByTemplate(Long templateId) {
        return attributeRepository.findByTemplateId(templateId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateAttribute> getById(Long id) {
        return attributeRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateAttribute> getByTemplateAndKey(Long templateId, String key) {
        return attributeRepository.findByTemplateIdAndAttributeKey(templateId, key);
    }
    
    @Transactional
    public TemplateAttribute create(TemplateAttribute attribute, Template template) {
        attribute.setTemplate(template);
        return attributeRepository.save(attribute);
    }
    
    @Transactional
    public TemplateAttribute update(TemplateAttribute attribute) {
        return attributeRepository.save(attribute);
    }
    
    @Transactional
    public void delete(Long id) {
        attributeRepository.deleteById(id);
    }
    
    @Transactional
    public void deleteAllByTemplate(Long templateId) {
        attributeRepository.deleteByTemplateId(templateId);
    }
    
    public boolean belongsToTemplate(Long attributeId, Long templateId) {
        return attributeRepository.findById(attributeId)
            .map(attr -> attr.getTemplate().getId().equals(templateId))
            .orElse(false);
    }
    
    public boolean keyExistsForTemplate(Long templateId, String key, Long excludeId) {
        if (excludeId != null) {
            return attributeRepository.existsByTemplateIdAndAttributeKeyAndIdNot(templateId, key, excludeId);
        }
        return attributeRepository.existsByTemplateIdAndAttributeKey(templateId, key);
    }
}

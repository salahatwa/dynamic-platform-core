package com.platform.service;

import com.platform.entity.Corporate;
import com.platform.repository.CorporateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorporateService {

    private final CorporateRepository corporateRepository;

    /**
     * Get current user's corporate information
     */
    public Corporate getCurrentCorporate() {
        Long corporateId = getCurrentCorporateId();
        return corporateRepository.findById(corporateId)
                .orElseThrow(() -> new RuntimeException("Corporate not found"));
    }

    /**
     * Update corporate information
     */
    @Transactional
    public Corporate updateCorporate(Corporate corporateData) {
        Long corporateId = getCurrentCorporateId();
        
        Corporate existingCorporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new RuntimeException("Corporate not found"));

        // Validate unique constraints
        if (!existingCorporate.getName().equals(corporateData.getName()) && 
            corporateRepository.existsByName(corporateData.getName())) {
            throw new RuntimeException("Corporate name already exists");
        }

        if (!existingCorporate.getDomain().equals(corporateData.getDomain()) && 
            corporateRepository.existsByDomain(corporateData.getDomain())) {
            throw new RuntimeException("Corporate domain already exists");
        }

        // Update fields
        existingCorporate.setName(corporateData.getName());
        existingCorporate.setDomain(corporateData.getDomain());
        existingCorporate.setDescription(corporateData.getDescription());

        Corporate updatedCorporate = corporateRepository.save(existingCorporate);
        log.info("Corporate updated successfully: {}", updatedCorporate.getName());
        
        return updatedCorporate;
    }

    /**
     * Check if corporate name is available (excluding current corporate)
     */
    public boolean isNameAvailable(String name) {
        Long corporateId = getCurrentCorporateId();
        Corporate currentCorporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new RuntimeException("Corporate not found"));
        
        // If it's the same name as current, it's available
        if (currentCorporate.getName().equals(name)) {
            return true;
        }
        
        return !corporateRepository.existsByName(name);
    }

    /**
     * Check if corporate domain is available (excluding current corporate)
     */
    public boolean isDomainAvailable(String domain) {
        Long corporateId = getCurrentCorporateId();
        Corporate currentCorporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new RuntimeException("Corporate not found"));
        
        // If it's the same domain as current, it's available
        if (currentCorporate.getDomain().equals(domain)) {
            return true;
        }
        
        return !corporateRepository.existsByDomain(domain);
    }

    /**
     * Get corporate by ID (with access validation)
     */
    public Corporate getCorporateById(Long id) {
        Long currentCorporateId = getCurrentCorporateId();
        
        // Users can only access their own corporate information
        if (!currentCorporateId.equals(id)) {
            throw new RuntimeException("Access denied");
        }
        
        return corporateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Corporate not found"));
    }

    /**
     * Get current user's corporate ID from security context
     */
    private Long getCurrentCorporateId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.platform.security.UserPrincipal) {
            com.platform.security.UserPrincipal userPrincipal = (com.platform.security.UserPrincipal) auth.getPrincipal();
            return userPrincipal.getCorporateId();
        }
        throw new RuntimeException("Unable to get corporate ID from security context");
    }
}
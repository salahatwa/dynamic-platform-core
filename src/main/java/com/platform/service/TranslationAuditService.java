package com.platform.service;

import com.platform.entity.TranslationApp;
import com.platform.entity.TranslationAuditLog;
import com.platform.entity.TranslationKey;
import com.platform.entity.User;
import com.platform.repository.TranslationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TranslationAuditService {
    
    private final TranslationAuditLogRepository auditRepository;
    
    @Transactional(readOnly = true)
    public Page<TranslationAuditLog> getAllByApp(Long appId, Pageable pageable) {
        return auditRepository.findByAppId(appId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationAuditLog> getByAppAndAction(Long appId, String action, Pageable pageable) {
        return auditRepository.findByAppIdAndAction(appId, action, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationAuditLog> getByAppAndUser(Long appId, Long userId, Pageable pageable) {
        return auditRepository.findByAppIdAndUserId(appId, userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationAuditLog> getByAppAndDateRange(
            Long appId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditRepository.findByAppIdAndTimestampBetween(appId, startDate, endDate, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<TranslationAuditLog> getByKey(Long keyId) {
        return auditRepository.findByKeyIdOrderByTimestampDesc(keyId);
    }
    
    @Transactional
    public void log(TranslationApp app, TranslationKey key, String language, String action,
                    String oldValue, String newValue, User user, String ipAddress) {
        TranslationAuditLog log = TranslationAuditLog.builder()
            .app(app)
            .key(key)
            .language(language)
            .action(action)
            .oldValue(oldValue)
            .newValue(newValue)
            .user(user)
            .userEmail(user != null ? user.getEmail() : null)
            .ipAddress(ipAddress)
            .timestamp(LocalDateTime.now())
            .build();
        
        auditRepository.save(log);
    }
    
    @Transactional
    public void logBulk(TranslationApp app, String action, User user, String ipAddress, String details) {
        TranslationAuditLog log = TranslationAuditLog.builder()
            .app(app)
            .action(action)
            .newValue(details)
            .user(user)
            .userEmail(user != null ? user.getEmail() : null)
            .ipAddress(ipAddress)
            .timestamp(LocalDateTime.now())
            .build();
        
        auditRepository.save(log);
    }
    
    public long countByApp(Long appId) {
        return auditRepository.countByAppId(appId);
    }
}

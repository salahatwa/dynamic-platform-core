package com.platform.service;

import com.platform.entity.AuditLog;
import com.platform.entity.User;
import com.platform.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Transactional
    public AuditLog log(String action, String entityType, Long entityId, String entityName, 
                        User user, String details, String ipAddress) {
        String userName = "System";
        String userEmail = "system@platform.com";
        
        if (user != null) {
            // Try to get name, if null use firstName + lastName, if those are null use email
            if (user.getName() != null && !user.getName().isEmpty()) {
                userName = user.getName();
            } else if (user.getFirstName() != null && user.getLastName() != null) {
                userName = user.getFirstName() + " " + user.getLastName();
            } else if (user.getEmail() != null) {
                userName = user.getEmail();
            }
            userEmail = user.getEmail();
        }
        
        AuditLog auditLog = new AuditLog(
            action,
            entityType,
            entityId,
            entityName,
            userName,
            userEmail,
            details,
            ipAddress
        );
        
        if (user != null) {
            auditLog.setUser(user);
            auditLog.setCorporate(user.getCorporate());
        }
        
        return auditLogRepository.save(auditLog);
    }
    
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    public Page<AuditLog> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }
    
    public Page<AuditLog> getLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType, pageable);
    }
    
    public Page<AuditLog> getLogsByUser(String userEmail, Pageable pageable) {
        return auditLogRepository.findByUserEmailOrderByTimestampDesc(userEmail, pageable);
    }
}

package com.platform.repository;

import com.platform.entity.AppConfigAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppConfigAuditRepository extends JpaRepository<AppConfigAudit, Long> {
    
    List<AppConfigAudit> findByConfigIdOrderByTimestampDesc(Long configId);
}

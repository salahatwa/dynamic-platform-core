package com.platform.repository;

import com.platform.entity.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {
    List<TemplateVersion> findByTemplateIdOrderByVersionDesc(Long templateId);
    Optional<TemplateVersion> findByTemplateIdAndVersion(Long templateId, Integer version);
}

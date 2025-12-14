package com.platform.repository;

import com.platform.entity.TemplateAsset;
import com.platform.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateAssetRepository extends JpaRepository<TemplateAsset, Long> {
    List<TemplateAsset> findByTemplateId(Long templateId);
    List<TemplateAsset> findByTemplateIdAndAssetType(Long templateId, AssetType assetType);
}

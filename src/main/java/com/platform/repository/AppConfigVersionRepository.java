package com.platform.repository;

import com.platform.entity.AppConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppConfigVersionRepository extends JpaRepository<AppConfigVersion, Long> {
    
    List<AppConfigVersion> findByConfigIdOrderByVersionDesc(Long configId);
    
    AppConfigVersion findTopByConfigIdOrderByVersionDesc(Long configId);
}

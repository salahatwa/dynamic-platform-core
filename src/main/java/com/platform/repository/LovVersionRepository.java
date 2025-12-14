package com.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.entity.LovVersion;

import java.util.List;

@Repository
public interface LovVersionRepository extends JpaRepository<LovVersion, Long> {
    
    List<LovVersion> findByLovIdOrderByVersionDesc(Long lovId);
    
    LovVersion findTopByLovIdOrderByVersionDesc(Long lovId);
}

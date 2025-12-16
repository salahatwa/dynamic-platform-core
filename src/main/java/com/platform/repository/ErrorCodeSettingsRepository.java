package com.platform.repository;

import com.platform.entity.ErrorCodeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErrorCodeSettingsRepository extends JpaRepository<ErrorCodeSettings, Long> {
    
    Optional<ErrorCodeSettings> findByAppId(Long appId);
    
    @Modifying
    @Query("UPDATE ErrorCodeSettings e SET e.currentSequence = e.currentSequence + 1 WHERE e.app.id = :appId")
    int incrementSequence(@Param("appId") Long appId);
    
    @Query("SELECT e.currentSequence FROM ErrorCodeSettings e WHERE e.app.id = :appId")
    Optional<Long> getCurrentSequence(@Param("appId") Long appId);
}
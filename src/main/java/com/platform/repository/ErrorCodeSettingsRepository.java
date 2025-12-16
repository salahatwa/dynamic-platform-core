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
    
    Optional<ErrorCodeSettings> findByCorporateId(Long corporateId);
    
    @Modifying
    @Query("UPDATE ErrorCodeSettings e SET e.currentSequence = e.currentSequence + 1 WHERE e.corporateId = :corporateId")
    int incrementSequence(@Param("corporateId") Long corporateId);
    
    @Query("SELECT e.currentSequence FROM ErrorCodeSettings e WHERE e.corporateId = :corporateId")
    Optional<Long> getCurrentSequence(@Param("corporateId") Long corporateId);
}
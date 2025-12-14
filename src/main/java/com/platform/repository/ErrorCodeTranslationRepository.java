package com.platform.repository;

import com.platform.entity.ErrorCodeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorCodeTranslationRepository extends JpaRepository<ErrorCodeTranslation, Long> {
    
    List<ErrorCodeTranslation> findByErrorCodeId(Long errorCodeId);
    
    @Query("SELECT t FROM ErrorCodeTranslation t WHERE t.errorCode.id = :errorCodeId AND t.languageCode = :languageCode")
    Optional<ErrorCodeTranslation> findByErrorCodeIdAndLanguageCode(@Param("errorCodeId") Long errorCodeId, @Param("languageCode") String languageCode);
    
    void deleteByErrorCodeId(Long errorCodeId);
}

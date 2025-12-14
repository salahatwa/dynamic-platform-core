package com.platform.repository;

import com.platform.entity.ErrorCodeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorCodeVersionRepository extends JpaRepository<ErrorCodeVersion, Long> {
    
    @Query("SELECT v FROM ErrorCodeVersion v WHERE v.errorCodeId = :errorCodeId ORDER BY v.versionNumber DESC")
    List<ErrorCodeVersion> findByErrorCodeIdOrderByVersionNumberDesc(@Param("errorCodeId") Long errorCodeId);
    
    @Query("SELECT MAX(v.versionNumber) FROM ErrorCodeVersion v WHERE v.errorCodeId = :errorCodeId")
    Integer findMaxVersionNumber(@Param("errorCodeId") Long errorCodeId);
    
    @Query("SELECT MAX(v.versionNumber) FROM ErrorCodeVersion v WHERE v.errorCodeId = :errorCodeId")
    Optional<Integer> findMaxVersionByErrorCodeId(@Param("errorCodeId") Long errorCodeId);
    
    @Query("SELECT v FROM ErrorCodeVersion v WHERE v.errorCodeId = :errorCodeId AND v.versionNumber = :versionNumber")
    Optional<ErrorCodeVersion> findByErrorCodeIdAndVersionNumber(@Param("errorCodeId") Long errorCodeId, @Param("versionNumber") Integer versionNumber);
}

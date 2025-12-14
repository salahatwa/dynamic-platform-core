package com.platform.repository;

import com.platform.entity.Corporate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorporateRepository extends JpaRepository<Corporate, Long> {
    Optional<Corporate> findByDomain(String domain);
    Boolean existsByDomain(String domain);
    Boolean existsByName(String name);
}

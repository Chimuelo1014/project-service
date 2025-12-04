package com.sentinel.project_service.repository;

import com.sentinel.project_service.entity.DomainEntity;
import com.sentinel.project_service.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {
    List<DomainEntity> findByProjectId(UUID projectId);
    Optional<DomainEntity> findByDomainUrl(String domainUrl);
    long countByProjectId(UUID projectId);
    long countByProjectIdAndVerificationStatus(UUID projectId, VerificationStatus status);
    boolean existsByDomainUrl(String domainUrl);
}

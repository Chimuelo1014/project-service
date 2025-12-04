package com.sentinel.project_service.repository;

import com.sentinel.project_service.entity.ProjectEntity;
import com.sentinel.project_service.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    List<ProjectEntity> findByTenantIdAndStatus(UUID tenantId, ProjectStatus status);
    List<ProjectEntity> findByTenantId(UUID tenantId);
    Optional<ProjectEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, ProjectStatus status);
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}

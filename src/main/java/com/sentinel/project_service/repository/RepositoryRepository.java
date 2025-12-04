package com.sentinel.project_service.repository;

import com.sentinel.project_service.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, UUID> {
    List<RepositoryEntity> findByProjectId(UUID projectId);
    Optional<RepositoryEntity> findByRepoUrl(String repoUrl);
    long countByProjectId(UUID projectId);
    boolean existsByRepoUrl(String repoUrl);
}

package com.sentinel.project_service.service.impl;

import com.sentinel.project_service.client.TenantServiceClient;
import com.sentinel.project_service.dto.request.CreateProjectRequest;
import com.sentinel.project_service.dto.response.ProjectDTO;
import com.sentinel.project_service.entity.ProjectEntity;
import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import com.sentinel.project_service.enums.ProjectStatus;
import com.sentinel.project_service.events.ProjectEventPublisher;
import com.sentinel.project_service.exception.*;
import com.sentinel.project_service.repository.ProjectRepository;
import com.sentinel.project_service.repository.TenantLimitsCacheRepository;
import com.sentinel.project_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantLimitsCacheRepository limitsCache;
    private final TenantServiceClient tenantClient;
    private final ProjectEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProjectDTO createProject(CreateProjectRequest request, UUID tenantId, UUID userId) {
        log.info("Creating project for tenant: {}", tenantId);

        // Validar límites
        validateProjectLimit(tenantId);

        // Crear proyecto
        ProjectEntity project = ProjectEntity.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(userId)
                .status(ProjectStatus.ACTIVE)
                .build();

        projectRepository.save(project);

        // Incrementar contador en tenant-service
        try {
            tenantClient.incrementResource(tenantId, "PROJECT");
        } catch (Exception e) {
            log.error("Failed to increment project count in tenant-service: {}", e.getMessage());
        }

        // Publicar evento
        eventPublisher.publishProjectCreated(project);

        log.info("Project created: {}", project.getId());

        return mapToDTO(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByTenant(UUID tenantId) {
        return projectRepository.findByTenantIdAndStatus(tenantId, ProjectStatus.ACTIVE)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        return mapToDTO(project);
    }

    @Override
    @Transactional
    public ProjectDTO updateProject(UUID projectId, CreateProjectRequest request, UUID userId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Verificar ownership
        if (!project.getOwnerId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to update this project");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        projectRepository.save(project);

        log.info("Project updated: {}", projectId);

        return mapToDTO(project);
    }

    @Override
    @Transactional
    public void deleteProject(UUID projectId, UUID userId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Verificar ownership
        if (!project.getOwnerId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete this project");
        }

        // Soft delete
        project.setStatus(ProjectStatus.DELETED);
        projectRepository.save(project);

        // Decrementar contador en tenant-service
        try {
            tenantClient.decrementResource(project.getTenantId(), "PROJECT");
        } catch (Exception e) {
            log.error("Failed to decrement project count: {}", e.getMessage());
        }

        // Publicar evento
        eventPublisher.publishProjectDeleted(projectId, project.getTenantId());

        log.info("Project deleted: {}", projectId);
    }

    @Override
    @Transactional
    public void incrementDomainCount(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
        
        project.incrementDomains();
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public void incrementRepoCount(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
        
        project.incrementRepos();
        projectRepository.save(project);
    }

    // Validaciones
    private void validateProjectLimit(UUID tenantId) {
        long currentCount = projectRepository.countByTenantIdAndStatus(tenantId, ProjectStatus.ACTIVE);

        // Buscar límites en cache
        TenantLimitsCacheEntity limits = limitsCache.findById(tenantId)
                .orElseGet(() -> fetchAndCacheLimits(tenantId));

        if (!limits.canCreateProject((int) currentCount)) {
            throw new LimitExceededException(
                String.format("Project limit reached (%d/%d). Upgrade your plan.", 
                    currentCount, limits.getMaxProjects())
            );
        }
    }

    private TenantLimitsCacheEntity fetchAndCacheLimits(UUID tenantId) {
        try {
            var tenantDTO = tenantClient.getTenant(tenantId);
            
            TenantLimitsCacheEntity cache = TenantLimitsCacheEntity.builder()
                    .tenantId(tenantId)
                    .maxProjects(tenantDTO.getLimits().getMaxProjects())
                    .maxDomains(tenantDTO.getLimits().getMaxDomains())
                    .maxRepos(tenantDTO.getLimits().getMaxRepos())
                    .build();

            limitsCache.save(cache);
            return cache;
        } catch (Exception e) {
            log.error("Failed to fetch tenant limits: {}", e.getMessage());
            throw new ServiceUnavailableException("Unable to validate project limits");
        }
    }

    private ProjectDTO mapToDTO(ProjectEntity entity) {
        return ProjectDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .ownerId(entity.getOwnerId())
                .domainCount(entity.getDomainCount())
                .repoCount(entity.getRepoCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
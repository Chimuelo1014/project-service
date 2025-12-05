package com.sentinel.project_service.service.impl;

import com.sentinel.project_service.client.TenantServiceClient;
import com.sentinel.project_service.client.UserManagementServiceClient;
import com.sentinel.project_service.client.dto.TenantDTO;
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
    private final UserManagementServiceClient userMgmtClient;
    private final ProjectEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProjectDTO createProject(CreateProjectRequest request, UUID tenantId, UUID userId) {
        log.info("Creating project '{}' for tenant: {} by user: {}", 
            request.getName(), tenantId, userId);

        // 1. VALIDAR PERMISOS: Usuario debe ser miembro del tenant
        validateUserTenantMembership(userId, tenantId);

        // 2. Validar límites ANTES de crear
        long currentCount = projectRepository.countByTenantIdAndStatus(tenantId, ProjectStatus.ACTIVE);
        TenantLimitsCacheEntity limits = getCachedLimits(tenantId);

        if (!limits.canCreateProject((int) currentCount)) {
            log.warn("Project limit reached for tenant {}: {}/{}", 
                tenantId, currentCount, limits.getMaxProjects());
            
            throw new LimitExceededException(
                String.format("Project limit reached (%d/%d). Upgrade your plan.", 
                    currentCount, limits.getMaxProjects())
            );
        }

        // 3. Crear proyecto
        ProjectEntity project = ProjectEntity.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(userId)
                .status(ProjectStatus.ACTIVE)
                .build();

        projectRepository.save(project);
        log.info("Project created with ID: {}", project.getId());

        // 4. Incrementar contador en tenant-service (con manejo de errores)
        try {
            tenantClient.incrementResource(tenantId, "PROJECT");
            log.debug("Project count incremented in tenant-service");
        } catch (Exception e) {
            log.error("Failed to increment project count: {}", e.getMessage());
            // No fallar la transacción - el cache local se sincronizará
        }

        // 5. Publicar evento (para user-management asignar PROJECT_ADMIN)
        eventPublisher.publishProjectCreated(project);

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
        log.info("Updating project: {} by user: {}", projectId, userId);
        
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Verificar ownership O ser admin del tenant
        if (!project.getOwnerId().equals(userId)) {
            validateUserIsAdmin(userId, project.getTenantId());
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
        log.info("Deleting project: {} by user: {}", projectId, userId);
        
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Verificar ownership O ser admin del tenant
        if (!project.getOwnerId().equals(userId)) {
            validateUserIsAdmin(userId, project.getTenantId());
        }

        // Soft delete
        project.setStatus(ProjectStatus.DELETED);
        projectRepository.save(project);

        // Decrementar contador en tenant-service
        try {
            tenantClient.decrementResource(project.getTenantId(), "PROJECT");
            log.debug("Project count decremented in tenant-service");
        } catch (Exception e) {
            log.error("Failed to decrement project count: {}", e.getMessage());
        }

        // Publicar evento (para scan-orchestrator cancelar scans activos)
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

    // ============================================
    // PERMISSION VALIDATION METHODS
    // ============================================

    /**
     * Valida que el usuario sea miembro del tenant.
     * Lanza UnauthorizedException si no lo es.
     */
    private void validateUserTenantMembership(UUID userId, UUID tenantId) {
        try {
            String role = userMgmtClient.getTenantRole(tenantId, userId);
            
            if (role == null) {
                log.warn("User {} is not a member of tenant {}", userId, tenantId);
                throw new UnauthorizedException(
                    "You don't have access to this tenant"
                );
            }
            
            log.debug("User {} has role {} in tenant {}", userId, role, tenantId);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate user membership: {}", e.getMessage());
            throw new ServiceUnavailableException(
                "Unable to validate permissions. Please try again."
            );
        }
    }

    /**
     * Valida que el usuario sea TENANT_ADMIN.
     */
    private void validateUserIsAdmin(UUID userId, UUID tenantId) {
        try {
            String role = userMgmtClient.getTenantRole(tenantId, userId);
            
            if (!"TENANT_ADMIN".equals(role)) {
                log.warn("User {} is not admin of tenant {}", userId, tenantId);
                throw new UnauthorizedException(
                    "Only tenant admins can perform this action"
                );
            }
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate admin role: {}", e.getMessage());
            throw new ServiceUnavailableException(
                "Unable to validate permissions. Please try again."
            );
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Obtiene límites del cache local.
     * Si no existe, consulta tenant-service y cachea.
     */
    private TenantLimitsCacheEntity getCachedLimits(UUID tenantId) {
        return limitsCache.findById(tenantId)
                .orElseGet(() -> fetchAndCacheLimits(tenantId));
    }

    /**
     * Consulta tenant-service y guarda en cache local.
     */
    private TenantLimitsCacheEntity fetchAndCacheLimits(UUID tenantId) {
        log.info("Cache MISS - Fetching limits for tenant: {}", tenantId);
        
        try {
            TenantDTO tenant = tenantClient.getTenant(tenantId);
            
            TenantLimitsCacheEntity cache = TenantLimitsCacheEntity.builder()
                    .tenantId(tenantId)
                    .maxProjects(tenant.getLimits().getMaxProjects())
                    .maxDomains(tenant.getLimits().getMaxDomains())
                    .maxRepos(tenant.getLimits().getMaxRepos())
                    .build();

            limitsCache.save(cache);
            log.info("Cache STORED for tenant: {}", tenantId);
            
            return cache;
        } catch (Exception e) {
            log.error("Failed to fetch tenant limits: {}", e.getMessage());
            throw new ServiceUnavailableException(
                "Unable to validate project limits. Please try again."
            );
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
package com.sentinel.project_service.service.impl;

import com.sentinel.project_service.dto.request.AddRepositoryRequest;
import com.sentinel.project_service.dto.response.RepositoryDTO;
import com.sentinel.project_service.entity.ProjectEntity;
import com.sentinel.project_service.entity.RepositoryEntity;
import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import com.sentinel.project_service.events.ProjectEventPublisher;
import com.sentinel.project_service.exception.*;
import com.sentinel.project_service.repository.ProjectRepository;
import com.sentinel.project_service.repository.RepositoryRepository;
import com.sentinel.project_service.repository.TenantLimitsCacheRepository;
import com.sentinel.project_service.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryServiceImpl implements RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final ProjectRepository projectRepository;
    private final TenantLimitsCacheRepository limitsCache;
    private final ProjectEventPublisher eventPublisher;
    
    // ⚠️ En producción, usar un secret desde variables de entorno
    private static final String ENCRYPTION_KEY = "MySuperSecretKey"; // 16 chars

    @Override
    @Transactional
    public RepositoryDTO addRepository(UUID projectId, AddRepositoryRequest request) {
        log.info("Adding repository to project: {}", projectId);

        // Validar proyecto existe
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        // Validar duplicado
        if (repositoryRepository.existsByRepoUrl(request.getRepoUrl())) {
            throw new RepositoryAlreadyExistsException("Repository already exists: " + request.getRepoUrl());
        }

        // Validar límites
        validateRepoLimit(project.getTenantId(), projectId);

        // Encriptar access token si existe
        String encryptedToken = null;
        if (request.getAccessToken() != null && !request.getAccessToken().isEmpty()) {
            encryptedToken = encryptToken(request.getAccessToken());
        }

        // Crear repositorio
        RepositoryEntity repository = RepositoryEntity.builder()
                .projectId(projectId)
                .repoUrl(request.getRepoUrl())
                .repoType(request.getRepoType())
                .accessTokenEncrypted(encryptedToken)
                .branch(request.getBranch())
                .build();

        repositoryRepository.save(repository);

        // Incrementar contador en proyecto
        project.incrementRepos();
        projectRepository.save(project);

        // Publicar evento
        eventPublisher.publishRepositoryAdded(repository);

        log.info("Repository added: {}", repository.getId());

        return mapToDTO(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryDTO> getRepositoriesByProject(UUID projectId) {
        return repositoryRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRepository(UUID repositoryId) {
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found"));

        // Decrementar contador
        ProjectEntity project = projectRepository.findById(repository.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        project.decrementRepos();
        projectRepository.save(project);

        repositoryRepository.delete(repository);

        log.info("Repository deleted: {}", repositoryId);
    }

    @Override
    @Transactional
    public void updateLastScan(UUID repositoryId) {
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found"));

        repository.updateLastScan();
        repositoryRepository.save(repository);

        log.info("Repository last scan updated: {}", repositoryId);
    }

    // Helper methods
    private void validateRepoLimit(UUID tenantId, UUID projectId) {
        long currentCount = repositoryRepository.countByProjectId(projectId);

        TenantLimitsCacheEntity limits = limitsCache.findById(tenantId)
                .orElseThrow(() -> new ServiceUnavailableException("Unable to validate limits"));

        if (!limits.canAddRepo((int) currentCount)) {
            throw new LimitExceededException(
                String.format("Repository limit reached (%d/%d)", currentCount, limits.getMaxRepos())
            );
        }
    }

    private String encryptToken(String token) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(token.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encrypt token: {}", e.getMessage());
            throw new RuntimeException("Token encryption failed");
        }
    }

    private RepositoryDTO mapToDTO(RepositoryEntity entity) {
        return RepositoryDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .repoUrl(entity.getRepoUrl())
                .repoType(entity.getRepoType())
                .branch(entity.getBranch())
                .hasAccessToken(entity.hasAccessToken())
                .lastScanAt(entity.getLastScanAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

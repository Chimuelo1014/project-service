package com.sentinel.project_service.service.impl;

import com.sentinel.project_service.dto.request.AddDomainRequest;
import com.sentinel.project_service.dto.response.DomainDTO;
import com.sentinel.project_service.entity.DomainEntity;
import com.sentinel.project_service.entity.ProjectEntity;
import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import com.sentinel.project_service.enums.VerificationMethod;
import com.sentinel.project_service.enums.VerificationStatus;
import com.sentinel.project_service.events.ProjectEventPublisher;
import com.sentinel.project_service.exception.*;
import com.sentinel.project_service.repository.DomainRepository;
import com.sentinel.project_service.repository.ProjectRepository;
import com.sentinel.project_service.repository.TenantLimitsCacheRepository;
import com.sentinel.project_service.service.DomainService;
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
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;
    private final ProjectRepository projectRepository;
    private final TenantLimitsCacheRepository limitsCache;
    private final ProjectEventPublisher eventPublisher;

    @Override
    @Transactional
    public DomainDTO addDomain(UUID projectId, AddDomainRequest request) {
        log.info("Adding domain '{}' to project: {}", request.getDomainUrl(), projectId);

        // 1. Validar proyecto existe
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        // 2. Normalizar URL
        String normalizedUrl = normalizeDomainUrl(request.getDomainUrl());

        // 3. Validar duplicado GLOBAL (no solo por proyecto)
        if (domainRepository.existsByDomainUrl(normalizedUrl)) {
            throw new DomainAlreadyExistsException(
                "Domain already registered in the system: " + normalizedUrl
            );
        }

        // 4. Validar límites del TENANT (no solo del proyecto)
        validateDomainLimit(project.getTenantId(), projectId);

        // 5. Generar token de verificación
        String verificationToken = UUID.randomUUID().toString().replace("-", "");

        // 6. Determinar método de verificación
        VerificationMethod method = request.getVerificationMethod() != null 
            ? request.getVerificationMethod() 
            : VerificationMethod.DNS_TXT;

        // 7. Crear dominio
        DomainEntity domain = DomainEntity.builder()
                .projectId(projectId)
                .domainUrl(normalizedUrl)
                .verificationStatus(VerificationStatus.PENDING)
                .verificationMethod(method)
                .verificationToken(verificationToken)
                .build();

        domainRepository.save(domain);
        log.info("Domain created with ID: {}", domain.getId());

        // 8. Incrementar contador en proyecto
        project.incrementDomains();
        projectRepository.save(project);

        // 9. Publicar evento para domain-verification-service (C#)
        eventPublisher.publishDomainAdded(domain);
        log.info("Domain verification requested: {} via {}", domain.getId(), method);

        return mapToDTO(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainDTO> getDomainsByProject(UUID projectId) {
        return domainRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDomain(UUID domainId) {
        log.info("Deleting domain: {}", domainId);
        
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new DomainNotFoundException("Domain not found"));

        // Decrementar contador en proyecto
        ProjectEntity project = projectRepository.findById(domain.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        project.decrementDomains();
        projectRepository.save(project);

        // Eliminar dominio
        domainRepository.delete(domain);

        log.info("Domain deleted: {}", domainId);
    }

    @Override
    @Transactional
    public void markDomainAsVerified(UUID domainId) {
        log.info("Marking domain as verified: {}", domainId);
        
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new DomainNotFoundException("Domain not found"));

        domain.markAsVerified();
        domainRepository.save(domain);

        log.info("Domain verified: {} ({})", domainId, domain.getDomainUrl());
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Valida límites del tenant (consultando cache local).
     */
    private void validateDomainLimit(UUID tenantId, UUID projectId) {
        // Contar dominios del PROYECTO (no del tenant completo)
        long currentCount = domainRepository.countByProjectId(projectId);

        // Obtener límites del tenant desde cache
        TenantLimitsCacheEntity limits = limitsCache.findById(tenantId)
                .orElseThrow(() -> new ServiceUnavailableException(
                    "Unable to validate limits. Cache not available."
                ));

        if (!limits.canAddDomain((int) currentCount)) {
            throw new LimitExceededException(
                String.format("Domain limit reached (%d/%d). Upgrade your plan.", 
                    currentCount, limits.getMaxDomains())
            );
        }

        log.debug("Domain limit OK: {}/{} for tenant {}", 
            currentCount, limits.getMaxDomains(), tenantId);
    }

    /**
     * Normaliza URL del dominio:
     * - Remueve protocolo (http/https)
     * - Remueve www.
     * - Remueve trailing slash
     * - Convierte a lowercase
     */
    private String normalizeDomainUrl(String url) {
        String normalized = url.trim();
        
        // Remover protocolo
        normalized = normalized.replaceAll("^https?://", "");
        
        // Remover www.
        normalized = normalized.replaceAll("^www\\.", "");
        
        // Remover trailing slash y paths
        normalized = normalized.replaceAll("/.*$", "");
        
        // Lowercase
        normalized = normalized.toLowerCase();
        
        log.debug("Normalized URL: {} -> {}", url, normalized);
        
        return normalized;
    }

    private DomainDTO mapToDTO(DomainEntity entity) {
        return DomainDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .domainUrl(entity.getDomainUrl())
                .verificationStatus(entity.getVerificationStatus())
                .verificationMethod(entity.getVerificationMethod())
                .verificationToken(entity.getVerificationToken())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
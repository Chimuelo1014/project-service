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
        log.info("Adding domain to project: {}", projectId);

        // Validar proyecto existe
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        // Normalizar URL
        String normalizedUrl = normalizeDomainUrl(request.getDomainUrl());

        // Validar duplicado
        if (domainRepository.existsByDomainUrl(normalizedUrl)) {
            throw new DomainAlreadyExistsException("Domain already exists: " + normalizedUrl);
        }

        // Validar límites
        validateDomainLimit(project.getTenantId(), projectId);

        // Generar token de verificación
        String verificationToken = UUID.randomUUID().toString();

        // Crear dominio
        DomainEntity domain = DomainEntity.builder()
                .projectId(projectId)
                .domainUrl(normalizedUrl)
                .verificationStatus(VerificationStatus.PENDING)
                .verificationMethod(request.getVerificationMethod() != null 
                    ? request.getVerificationMethod() 
                    : VerificationMethod.DNS_TXT)
                .verificationToken(verificationToken)
                .build();

        domainRepository.save(domain);

        // Incrementar contador en proyecto
        project.incrementDomains();
        projectRepository.save(project);

        // Publicar evento para domain-verification-service (C#)
        eventPublisher.publishDomainAdded(domain);

        log.info("Domain added: {}", domain.getId());

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
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new DomainNotFoundException("Domain not found"));

        // Decrementar contador
        ProjectEntity project = projectRepository.findById(domain.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        project.decrementDomains();
        projectRepository.save(project);

        domainRepository.delete(domain);

        log.info("Domain deleted: {}", domainId);
    }

    @Override
    @Transactional
    public void markDomainAsVerified(UUID domainId) {
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new DomainNotFoundException("Domain not found"));

        domain.markAsVerified();
        domainRepository.save(domain);

        log.info("Domain verified: {}", domainId);
    }

    // Helper methods
    private void validateDomainLimit(UUID tenantId, UUID projectId) {
        long currentCount = domainRepository.countByProjectId(projectId);

        TenantLimitsCacheEntity limits = limitsCache.findById(tenantId)
                .orElseThrow(() -> new ServiceUnavailableException("Unable to validate limits"));

        if (!limits.canAddDomain((int) currentCount)) {
            throw new LimitExceededException(
                String.format("Domain limit reached (%d/%d)", currentCount, limits.getMaxDomains())
            );
        }
    }

    private String normalizeDomainUrl(String url) {
        // Remover protocolo
        String normalized = url.replaceAll("^https?://", "");
        
        // Remover www.
        normalized = normalized.replaceAll("^www\\.", "");
        
        // Remover trailing slash
        normalized = normalized.replaceAll("/$", "");
        
        return normalized.toLowerCase();
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
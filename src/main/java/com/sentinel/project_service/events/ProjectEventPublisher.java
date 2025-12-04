package com.sentinel.project_service.events;

import com.sentinel.project_service.entity.DomainEntity;
import com.sentinel.project_service.entity.ProjectEntity;
import com.sentinel.project_service.entity.RepositoryEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${project.events.exchange}")
    private String projectExchange;

    /**
     * Publica: project.created
     * Consumidores: user-management-service (asignar PROJECT_ADMIN)
     */
    @Async
    public void publishProjectCreated(ProjectEntity project) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "project.created");
        event.put("projectId", project.getId().toString());
        event.put("tenantId", project.getTenantId().toString());
        event.put("ownerId", project.getOwnerId().toString());
        event.put("projectName", project.getName());
        event.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(projectExchange, "project.created", event);
            log.info("Published project.created event for project: {}", project.getId());
        } catch (Exception e) {
            log.error("Failed to publish project.created event: {}", e.getMessage());
        }
    }

    /**
     * Publica: project.deleted
     * Consumidores: scan-orchestrator-service (cancelar scans activos)
     */
    @Async
    public void publishProjectDeleted(UUID projectId, UUID tenantId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "project.deleted");
        event.put("projectId", projectId.toString());
        event.put("tenantId", tenantId.toString());
        event.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(projectExchange, "project.deleted", event);
            log.info("Published project.deleted event for project: {}", projectId);
        } catch (Exception e) {
            log.error("Failed to publish project.deleted event: {}", e.getMessage());
        }
    }

    /**
     * Publica: domain.added
     * Consumidores: domain-verification-service (C#) - iniciar verificación
     */
    @Async
    public void publishDomainAdded(DomainEntity domain) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "domain.added");
        event.put("domainId", domain.getId().toString());
        event.put("projectId", domain.getProjectId().toString());
        event.put("domainUrl", domain.getDomainUrl());
        event.put("verificationMethod", domain.getVerificationMethod().name());
        event.put("verificationToken", domain.getVerificationToken());
        event.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(projectExchange, "domain.added", event);
            log.info("Published domain.added event for domain: {}", domain.getId());
        } catch (Exception e) {
            log.error("Failed to publish domain.added event: {}", e.getMessage());
        }
    }

    /**
     * Publica: repository.added
     * Consumidores: ninguno por ahora (futuro: webhook notifications)
     */
    @Async
    public void publishRepositoryAdded(RepositoryEntity repository) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "repository.added");
        event.put("repositoryId", repository.getId().toString());
        event.put("projectId", repository.getProjectId().toString());
        event.put("repoUrl", repository.getRepoUrl());
        event.put("repoType", repository.getRepoType().name());
        event.put("branch", repository.getBranch());
        event.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(projectExchange, "repository.added", event);
            log.info("Published repository.added event for repo: {}", repository.getId());
        } catch (Exception e) {
            log.error("Failed to publish repository.added event: {}", e.getMessage());
        }
    }
}
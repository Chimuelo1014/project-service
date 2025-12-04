package com.sentinel.project_service.events;

import com.sentinel.project_service.entity.DomainEntity;
import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import com.sentinel.project_service.repository.DomainRepository;
import com.sentinel.project_service.repository.TenantLimitsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Listener de eventos consumidos por project-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectEventListener {

    private final TenantLimitsCacheRepository limitsCache;
    private final DomainRepository domainRepository;

    /**
     * Consume: tenant.plan.upgraded
     * Queue: project.tenant.upgraded.queue
     * 
     * Actualiza cache local de límites del tenant cuando cambia de plan.
     * 
     * Evento desde: tenant-service
     */
    @RabbitListener(queues = "project.tenant.upgraded.queue")
    @Transactional
    public void handleTenantPlanUpgraded(Map<String, Object> event) {
        try {
            log.info("📥 Received event: tenant.plan.upgraded - {}", event);

            // Validar estructura del evento
            String eventType = (String) event.get("eventType");
            if (!"tenant.plan.upgraded".equals(eventType)) {
                log.warn("⚠️ Unexpected event type: {}", eventType);
                return;
            }

            // Extraer datos del evento
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            
            UUID tenantId = UUID.fromString((String) data.get("tenantId"));
            String oldPlan = (String) data.get("oldPlan");
            String newPlan = (String) data.get("newPlan");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> newLimits = (Map<String, Object>) data.get("newLimits");
            
            int maxProjects = (int) newLimits.get("maxProjects");
            int maxDomains = (int) newLimits.get("maxDomains");
            int maxRepos = (int) newLimits.get("maxRepos");

            log.info("🔄 Updating limits cache for tenant {} - {} → {}", 
                tenantId, oldPlan, newPlan);

            // Actualizar o crear cache
            TenantLimitsCacheEntity cache = limitsCache.findById(tenantId)
                    .orElse(TenantLimitsCacheEntity.builder()
                            .tenantId(tenantId)
                            .build());

            cache.setMaxProjects(maxProjects);
            cache.setMaxDomains(maxDomains);
            cache.setMaxRepos(maxRepos);
            cache.setUpdatedAt(LocalDateTime.now());

            limitsCache.save(cache);

            log.info("✅ Tenant limits cache updated for tenant: {} - Projects: {}, Domains: {}, Repos: {}", 
                tenantId, maxProjects, maxDomains, maxRepos);

        } catch (Exception e) {
            log.error("❌ Failed to handle tenant.plan.upgraded event: {}", e.getMessage(), e);
            // Re-lanzar para que RabbitMQ reintente o envíe a DLQ
            throw new RuntimeException("Event processing failed", e);
        }
    }

    /**
     * Consume: domain.verified
     * Queue: project.domain.verified.queue
     * 
     * Actualiza estado de verificación del dominio.
     * 
     * Evento desde: domain-verification-service (C#)
     */
    @RabbitListener(queues = "project.domain.verified.queue")
    @Transactional
    public void handleDomainVerified(Map<String, Object> event) {
        try {
            log.info("📥 Received event: domain.verified - {}", event);

            // Validar estructura del evento
            String eventType = (String) event.get("eventType");
            if (!"domain.verified".equals(eventType)) {
                log.warn("⚠️ Unexpected event type: {}", eventType);
                return;
            }

            // Extraer datos del evento
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            
            UUID domainId = UUID.fromString((String) data.get("domainId"));
            boolean verified = (boolean) data.get("verified");
            String domainUrl = (String) data.get("domainUrl");

            log.info("🔄 Processing domain verification: {} - {} (verified: {})", 
                domainId, domainUrl, verified);

            // Buscar dominio
            DomainEntity domain = domainRepository.findById(domainId)
                    .orElseThrow(() -> new RuntimeException("Domain not found: " + domainId));

            // Actualizar estado
            if (verified) {
                domain.markAsVerified();
                log.info("✅ Domain verified: {} ({})", domainId, domainUrl);
            } else {
                domain.markAsFailed();
                log.warn("⚠️ Domain verification failed: {} ({})", domainId, domainUrl);
            }

            domainRepository.save(domain);

        } catch (Exception e) {
            log.error("❌ Failed to handle domain.verified event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }
}
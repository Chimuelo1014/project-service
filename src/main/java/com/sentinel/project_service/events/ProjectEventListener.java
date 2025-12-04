package com.sentinel.project_service.events;

import com.sentinel.project_service.entity.DomainEntity;
import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import com.sentinel.project_service.enums.VerificationStatus;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectEventListener {

    private final TenantLimitsCacheRepository limitsCache;
    private final DomainRepository domainRepository;

    /**
     * Consume: tenant.plan.upgraded
     * Actualiza cache local de límites del tenant
     */
    @RabbitListener(queues = "project.tenant.upgraded.queue")
    @Transactional
    public void handleTenantPlanUpgraded(Map<String, Object> event) {
        try {
            UUID tenantId = UUID.fromString((String) event.get("tenantId"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> newLimits = (Map<String, Object>) event.get("newLimits");
            
            int maxProjects = (int) newLimits.get("maxProjects");
            int maxDomains = (int) newLimits.get("maxDomains");
            int maxRepos = (int) newLimits.get("maxRepos");

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

            log.info("Tenant limits cache updated for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to handle tenant.plan.upgraded event: {}", e.getMessage());
            throw e; // Reenviar al DLQ
        }
    }

    /**
     * Consume: domain.verified
     * Actualiza estado de verificación del dominio (desde C# domain-verification-service)
     */
    @RabbitListener(queues = "project.domain.verified.queue")
    @Transactional
    public void handleDomainVerified(Map<String, Object> event) {
        try {
            UUID domainId = UUID.fromString((String) event.get("domainId"));
            boolean verified = (boolean) event.get("verified");

            DomainEntity domain = domainRepository.findById(domainId)
                    .orElseThrow(() -> new RuntimeException("Domain not found: " + domainId));

            if (verified) {
                domain.markAsVerified();
                log.info("Domain verified: {}", domainId);
            } else {
                domain.markAsFailed();
                log.warn("Domain verification failed: {}", domainId);
            }

            domainRepository.save(domain);
        } catch (Exception e) {
            log.error("Failed to handle domain.verified event: {}", e.getMessage());
            throw e;
        }
    }
}
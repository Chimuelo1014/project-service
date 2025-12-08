package com.sentinel.project_service.client;

import com.sentinel.project_service.client.dto.TenantSubscriptionLimitsDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client para comunicarse con billing-service.
 */
@FeignClient(name = "billing-service", url = "${services.billing.url}")
public interface BillingServiceClient {

    /**
     * Obtiene los límites del plan de un tenant.
     */
    @CircuitBreaker(name = "billingService", fallbackMethod = "getTenantLimitsFallback")
    @Retry(name = "billingService")
    @GetMapping("/api/internal/subscriptions/tenant/{tenantId}/limits")
    TenantSubscriptionLimitsDTO getTenantLimits(@PathVariable String tenantId);

    /**
     * Fallback: retorna límites muy restrictivos para no bloquear.
     */
    default TenantSubscriptionLimitsDTO getTenantLimitsFallback(String tenantId, Exception ex) {
        TenantSubscriptionLimitsDTO fallback = new TenantSubscriptionLimitsDTO();
        fallback.setTenantId(tenantId);
        fallback.setPlanId("FALLBACK");
        fallback.setSubscriptionStatus("UNKNOWN");
        fallback.setMaxUsers(1);
        fallback.setMaxProjects(1);
        fallback.setMaxDomains(1);
        fallback.setMaxRepos(0);
        fallback.setIncludesBlockchain(false);
        return fallback;
    }
}

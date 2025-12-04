package com.sentinel.project_service.client;

import com.sentinel.project_service.client.dto.TenantDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
    name = "tenant-service",
    url = "${services.tenant.url}"
)
public interface TenantServiceClient {

    /**
     * Obtiene información del tenant incluyendo límites
     * Con Circuit Breaker y Retry
     */
    @CircuitBreaker(name = "tenantService", fallbackMethod = "getTenantFallback")
    @Retry(name = "tenantService")
    @GetMapping("/api/tenants/{id}")
    TenantDTO getTenant(@PathVariable UUID id);

    /**
     * Incrementa contador de recursos (PROJECT, DOMAIN, REPO)
     */
    @CircuitBreaker(name = "tenantService")
    @Retry(name = "tenantService")
    @PostMapping("/api/tenants/{id}/resources/increment")
    void incrementResource(
        @PathVariable UUID id, 
        @RequestParam String resource
    );

    /**
     * Decrementa contador de recursos
     */
    @CircuitBreaker(name = "tenantService")
    @Retry(name = "tenantService")
    @PostMapping("/api/tenants/{id}/resources/decrement")
    void decrementResource(
        @PathVariable UUID id, 
        @RequestParam String resource
    );

    /**
     * Fallback cuando tenant-service no responde
     */
    default TenantDTO getTenantFallback(UUID id, Exception ex) {
        throw new RuntimeException("Tenant service unavailable: " + ex.getMessage());
    }
}
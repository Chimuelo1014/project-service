package com.sentinel.project_service.client;

import com.sentinel.project_service.client.dto.TenantDTO;
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
     */
    @GetMapping("/api/tenants/{id}")
    TenantDTO getTenant(@PathVariable UUID id);

    /**
     * Incrementa contador de recursos (PROJECT, DOMAIN, REPO)
     */
    @PostMapping("/api/tenants/{id}/resources/increment")
    void incrementResource(
        @PathVariable UUID id, 
        @RequestParam String resource
    );

    /**
     * Decrementa contador de recursos
     */
    @PostMapping("/api/tenants/{id}/resources/decrement")
    void decrementResource(
        @PathVariable UUID id, 
        @RequestParam String resource
    );
}
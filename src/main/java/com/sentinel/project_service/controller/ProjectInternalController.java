package com.sentinel.project_service.controller;

import com.sentinel.project_service.dto.response.ProjectDTO;
import com.sentinel.project_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal API Controller para comunicación inter-servicios.
 * Usado por: scan-orchestrator-service, user-management-service
 */
@Slf4j
@RestController
@RequestMapping("/api/projects/internal")
@RequiredArgsConstructor
public class ProjectInternalController {

    private final ProjectService projectService;

    /**
     * Obtener proyecto por ID (sin validar permisos).
     * GET /api/projects/internal/{projectId}
     * 
     * Usado por:
     * - scan-orchestrator-service (validar proyecto antes de scan)
     * - user-management-service (verificar existencia)
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable UUID projectId) {
        log.debug("Internal: Fetching project: {}", projectId);
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    /**
     * Verificar si un proyecto existe y está activo.
     * GET /api/projects/internal/{projectId}/exists
     */
    @GetMapping("/{projectId}/exists")
    public ResponseEntity<Boolean> projectExists(@PathVariable UUID projectId) {
        try {
            ProjectDTO project = projectService.getProjectById(projectId);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Verificar si un proyecto pertenece a un tenant.
     * GET /api/projects/internal/{projectId}/tenant/{tenantId}/verify
     */
    @GetMapping("/{projectId}/tenant/{tenantId}/verify")
    public ResponseEntity<Boolean> verifyProjectTenant(
            @PathVariable UUID projectId,
            @PathVariable UUID tenantId
    ) {
        try {
            ProjectDTO project = projectService.getProjectById(projectId);
            boolean belongs = project.getTenantId().equals(tenantId);
            return ResponseEntity.ok(belongs);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }
}
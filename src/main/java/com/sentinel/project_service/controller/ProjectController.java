package com.sentinel.project_service.controller;

import com.sentinel.project_service.dto.request.AddDomainRequest;
import com.sentinel.project_service.dto.request.AddRepositoryRequest;
import com.sentinel.project_service.dto.request.CreateProjectRequest;
import com.sentinel.project_service.dto.response.DomainDTO;
import com.sentinel.project_service.dto.response.ProjectDTO;
import com.sentinel.project_service.dto.response.RepositoryDTO;
import com.sentinel.project_service.service.DomainService;
import com.sentinel.project_service.service.ProjectService;
import com.sentinel.project_service.service.RepositoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final DomainService domainService;
    private final RepositoryService repositoryService;

    /**
     * Create project
     * POST /api/projects
     */
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Creating project for tenant: {}", tenantId);
        ProjectDTO project = projectService.createProject(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    /**
     * Get projects by tenant
     * GET /api/projects?tenantId={id}
     */
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<List<ProjectDTO>> getProjects(
            @RequestParam UUID tenantId) {
        log.info("Fetching projects for tenant: {}", tenantId);
        return ResponseEntity.ok(projectService.getProjectsByTenant(tenantId));
    }

    /**
     * Get project by ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable UUID id) {
        log.info("Fetching project: {}", id);
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    /**
     * Update project
     * PUT /api/projects/{id}
     */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Updating project: {}", id);
        return ResponseEntity.ok(projectService.updateProject(id, request, userId));
    }

    /**
     * Delete project
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Deleting project: {}", id);
        projectService.deleteProject(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add domain to project
     * POST /api/projects/{id}/domains
     */
    @PostMapping("/{id}/domains")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<DomainDTO> addDomain(
            @PathVariable UUID id,
            @Valid @RequestBody AddDomainRequest request) {
        log.info("Adding domain to project: {}", id);
        DomainDTO domain = domainService.addDomain(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(domain);
    }

    /**
     * Get domains of a project
     * GET /api/projects/{id}/domains
     */
    @GetMapping("/{id}/domains")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<List<DomainDTO>> getDomains(@PathVariable UUID id) {
        log.info("Fetching domains for project: {}", id);
        return ResponseEntity.ok(domainService.getDomainsByProject(id));
    }

    /**
     * Delete domain
     * DELETE /api/projects/{projectId}/domains/{domainId}
     */
    @DeleteMapping("/{projectId}/domains/{domainId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable UUID projectId,
            @PathVariable UUID domainId) {
        log.info("Deleting domain {} from project: {}", domainId, projectId);
        domainService.deleteDomain(domainId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add repository to project
     * POST /api/projects/{id}/repositories
     */
    @PostMapping("/{id}/repositories")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<RepositoryDTO> addRepository(
            @PathVariable UUID id,
            @Valid @RequestBody AddRepositoryRequest request) {
        log.info("Adding repository to project: {}", id);
        RepositoryDTO repo = repositoryService.addRepository(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(repo);
    }

    /**
     * Get repositories of a project
     * GET /api/projects/{id}/repositories
     */
    @GetMapping("/{id}/repositories")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<List<RepositoryDTO>> getRepositories(@PathVariable UUID id) {
        log.info("Fetching repositories for project: {}", id);
        return ResponseEntity.ok(repositoryService.getRepositoriesByProject(id));
    }

    /**
     * Delete repository
     * DELETE /api/projects/{projectId}/repositories/{repositoryId}
     */
    @DeleteMapping("/{projectId}/repositories/{repositoryId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_AUTHENTICATED_USER')")
    public ResponseEntity<Void> deleteRepository(
            @PathVariable UUID projectId,
            @PathVariable UUID repositoryId) {
        log.info("Deleting repository {} from project: {}", repositoryId, projectId);
        repositoryService.deleteRepository(repositoryId);
        return ResponseEntity.noContent().build();
    }
}

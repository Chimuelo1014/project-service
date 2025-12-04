package com.sentinel.project_service.service;

import com.sentinel.project_service.dto.request.CreateProjectRequest;
import com.sentinel.project_service.dto.response.ProjectDTO;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    
    ProjectDTO createProject(CreateProjectRequest request, UUID tenantId, UUID userId);
    
    List<ProjectDTO> getProjectsByTenant(UUID tenantId);
    
    ProjectDTO getProjectById(UUID projectId);
    
    ProjectDTO updateProject(UUID projectId, CreateProjectRequest request, UUID userId);
    
    void deleteProject(UUID projectId, UUID userId);
    
    void incrementDomainCount(UUID projectId);
    
    void incrementRepoCount(UUID projectId);
}
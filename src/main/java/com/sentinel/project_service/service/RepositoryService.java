package com.sentinel.project_service.service;

import com.sentinel.project_service.dto.request.AddRepositoryRequest;
import com.sentinel.project_service.dto.response.RepositoryDTO;

import java.util.List;
import java.util.UUID;

public interface RepositoryService {
    
    RepositoryDTO addRepository(UUID projectId, AddRepositoryRequest request);
    
    List<RepositoryDTO> getRepositoriesByProject(UUID projectId);
    
    void deleteRepository(UUID repositoryId);
    
    void updateLastScan(UUID repositoryId);
}

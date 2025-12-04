package com.sentinel.project_service.service;

import com.sentinel.project_service.dto.request.AddDomainRequest;
import com.sentinel.project_service.dto.response.DomainDTO;

import java.util.List;
import java.util.UUID;

public interface DomainService {
    
    DomainDTO addDomain(UUID projectId, AddDomainRequest request);
    
    List<DomainDTO> getDomainsByProject(UUID projectId);
    
    void deleteDomain(UUID domainId);
    
    void markDomainAsVerified(UUID domainId);
}

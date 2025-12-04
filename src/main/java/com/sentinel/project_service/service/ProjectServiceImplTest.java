package com.sentinel.project_service.service;

import com.sentinel.project_service.client.TenantServiceClient;
import com.sentinel.project_service.dto.request.CreateProjectRequest;
import com.sentinel.project_service.dto.response.ProjectDTO;
import com.sentinel.project_service.entity.ProjectEntity;
import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import com.sentinel.project_service.enums.ProjectStatus;
import com.sentinel.project_service.events.ProjectEventPublisher;
import com.sentinel.project_service.exception.LimitExceededException;
import com.sentinel.project_service.exception.ProjectNotFoundException;
import com.sentinel.project_service.repository.ProjectRepository;
import com.sentinel.project_service.repository.TenantLimitsCacheRepository;
import com.sentinel.project_service.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TenantLimitsCacheRepository limitsCache;

    @Mock
    private TenantServiceClient tenantClient;

    @Mock
    private ProjectEventPublisher eventPublisher;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID tenantId;
    private UUID userId;
    private CreateProjectRequest request;
    private TenantLimitsCacheEntity limits;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        request = CreateProjectRequest.builder()
                .name("Test Project")
                .description("Test Description")
                .build();

        limits = TenantLimitsCacheEntity.builder()
                .tenantId(tenantId)
                .maxProjects(5)
                .maxDomains(10)
                .maxRepos(5)
                .build();
    }

    @Test
    void createProject_Success() {
        // Arrange
        when(limitsCache.findById(tenantId)).thenReturn(Optional.of(limits));
        when(projectRepository.countByTenantIdAndStatus(tenantId, ProjectStatus.ACTIVE)).thenReturn(0L);
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ProjectDTO result = projectService.createProject(request, tenantId, userId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        assertEquals("Test Description", result.getDescription());
        verify(projectRepository).save(any(ProjectEntity.class));
        verify(eventPublisher).publishProjectCreated(any(ProjectEntity.class));
    }

    @Test
    void createProject_ExceedsLimit() {
        // Arrange
        when(limitsCache.findById(tenantId)).thenReturn(Optional.of(limits));
        when(projectRepository.countByTenantIdAndStatus(tenantId, ProjectStatus.ACTIVE)).thenReturn(5L);

        // Act & Assert
        assertThrows(LimitExceededException.class, () -> 
            projectService.createProject(request, tenantId, userId)
        );
        
        verify(projectRepository, never()).save(any());
    }

    @Test
    void getProjectById_Success() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .tenantId(tenantId)
                .name("Test Project")
                .ownerId(userId)
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        ProjectDTO result = projectService.getProjectById(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(projectId, result.getId());
        assertEquals("Test Project", result.getName());
    }

    @Test
    void getProjectById_NotFound() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> 
            projectService.getProjectById(projectId)
        );
    }

    @Test
    void deleteProject_Success() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .tenantId(tenantId)
                .name("Test Project")
                .ownerId(userId)
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        projectService.deleteProject(projectId, userId);

        // Assert
        verify(projectRepository).save(argThat(p -> p.getStatus() == ProjectStatus.DELETED));
        verify(eventPublisher).publishProjectDeleted(projectId, tenantId);
    }

    @Test
    void incrementDomainCount_Success() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .tenantId(tenantId)
                .domainCount(0)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        projectService.incrementDomainCount(projectId);

        // Assert
        verify(projectRepository).save(argThat(p -> p.getDomainCount() == 1));
    }
}
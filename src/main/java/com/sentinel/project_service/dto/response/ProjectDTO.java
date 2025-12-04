package com.sentinel.project_service.dto.response;

import com.sentinel.project_service.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private ProjectStatus status;
    private UUID ownerId;
    private int domainCount;
    private int repoCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
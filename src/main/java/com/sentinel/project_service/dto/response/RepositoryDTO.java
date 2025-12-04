package com.sentinel.project_service.dto.response;

import com.sentinel.project_service.enums.RepoType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDTO {
    private UUID id;
    private UUID projectId;
    private String repoUrl;
    private RepoType repoType;
    private String branch;
    private boolean hasAccessToken;
    private LocalDateTime lastScanAt;
    private LocalDateTime createdAt;
}

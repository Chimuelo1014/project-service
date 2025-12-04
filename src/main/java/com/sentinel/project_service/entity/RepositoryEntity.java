package com.sentinel.project_service.entity;

import com.sentinel.project_service.enums.RepoType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repositories", indexes = {
    @Index(name = "idx_repositories_project_id", columnList = "project_id"),
    @Index(name = "idx_repositories_type", columnList = "repo_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "repo_type", nullable = false, length = 20)
    private RepoType repoType;

    @Column(name = "access_token_encrypted", columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    @Column(length = 100)
    @Builder.Default
    private String branch = "main";

    @Column(name = "last_scan_at")
    private LocalDateTime lastScanAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean hasAccessToken() {
        return accessTokenEncrypted != null && !accessTokenEncrypted.isEmpty();
    }

    public void updateLastScan() {
        this.lastScanAt = LocalDateTime.now();
    }
}

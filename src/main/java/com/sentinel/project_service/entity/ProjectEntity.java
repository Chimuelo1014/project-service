package com.sentinel.project_service.entity;

import com.sentinel.project_service.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_projects_owner_id", columnList = "owner_id"),
    @Index(name = "idx_projects_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Counters denormalizados
    @Column(name = "domain_count")
    @Builder.Default
    private int domainCount = 0;

    @Column(name = "repo_count")
    @Builder.Default
    private int repoCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void incrementDomains() {
        this.domainCount++;
    }

    public void decrementDomains() {
        if (this.domainCount > 0) {
            this.domainCount--;
        }
    }

    public void incrementRepos() {
        this.repoCount++;
    }

    public void decrementRepos() {
        if (this.repoCount > 0) {
            this.repoCount--;
        }
    }

    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }

    public boolean isArchived() {
        return status == ProjectStatus.ARCHIVED;
    }
}

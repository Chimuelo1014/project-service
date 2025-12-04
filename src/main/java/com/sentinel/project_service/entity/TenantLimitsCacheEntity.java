package com.sentinel.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_limits_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantLimitsCacheEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "max_projects", nullable = false)
    private int maxProjects;

    @Column(name = "max_domains", nullable = false)
    private int maxDomains;

    @Column(name = "max_repos", nullable = false)
    private int maxRepos;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean hasUnlimitedProjects() {
        return maxProjects == -1;
    }

    public boolean canCreateProject(int currentCount) {
        return hasUnlimitedProjects() || currentCount < maxProjects;
    }

    public boolean canAddDomain(int currentCount) {
        return currentCount < maxDomains;
    }

    public boolean canAddRepo(int currentCount) {
        return currentCount < maxRepos;
    }
}

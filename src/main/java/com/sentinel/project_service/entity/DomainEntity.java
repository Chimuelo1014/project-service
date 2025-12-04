package com.sentinel.project_service.entity;

import com.sentinel.project_service.enums.VerificationMethod;
import com.sentinel.project_service.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "domains", indexes = {
    @Index(name = "idx_domains_project_id", columnList = "project_id"),
    @Index(name = "idx_domains_status", columnList = "verification_status"),
    @Index(name = "idx_domains_url", columnList = "domain_url")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "domain_url", nullable = false, length = 255)
    private String domainUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", length = 20)
    private VerificationMethod verificationMethod;

    @Column(name = "verification_token", length = 64)
    private String verificationToken;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    public boolean isPending() {
        return verificationStatus == VerificationStatus.PENDING;
    }

    public void markAsVerified() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.verificationStatus = VerificationStatus.FAILED;
    }
}

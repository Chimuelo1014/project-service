package com.sentinel.project_service.dto.response;

import com.sentinel.project_service.enums.VerificationMethod;
import com.sentinel.project_service.enums.VerificationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainDTO {
    private UUID id;
    private UUID projectId;
    private String domainUrl;
    private VerificationStatus verificationStatus;
    private VerificationMethod verificationMethod;
    private String verificationToken;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}

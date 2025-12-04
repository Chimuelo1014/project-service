package com.sentinel.project_service.client.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO {
    private UUID id;
    private String name;
    private String plan;
    private PlanLimitsDTO limits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanLimitsDTO {
        private int maxProjects;
        private int maxDomains;
        private int maxRepos;
        private int maxUsers;
    }
}
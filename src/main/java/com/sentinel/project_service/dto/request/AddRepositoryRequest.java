package com.sentinel.project_service.dto.request;

import com.sentinel.project_service.enums.RepoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRepositoryRequest {
    
    @NotBlank(message = "Repository URL is required")
    private String repoUrl;
    
    @NotNull(message = "Repository type is required")
    private RepoType repoType;
    
    private String accessToken;
    
    @Builder.Default
    private String branch = "main";
}
package com.sentinel.project_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {
    
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255)
    private String name;
    
    @Size(max = 1000)
    private String description;
}

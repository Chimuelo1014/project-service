package com.sentinel.project_service.dto.request;

import com.sentinel.project_service.enums.VerificationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDomainRequest {
    
    @NotBlank(message = "Domain URL is required")
    @Pattern(regexp = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$")
    private String domainUrl;
    
    private VerificationMethod verificationMethod;
}

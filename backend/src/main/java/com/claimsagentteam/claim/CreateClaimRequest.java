package com.claimsagentteam.claim;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClaimRequest(
        @NotBlank(message = "Policy number is required")
        String policyNumber,
        @NotNull(message = "Incident type is required")
        IncidentType incidentType,
        @NotNull(message = "Incident date is required")
        LocalDate incidentDate,
        @NotBlank(message = "Description is required")
        String description) {
}

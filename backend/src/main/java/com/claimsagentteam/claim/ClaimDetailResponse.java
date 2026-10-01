package com.claimsagentteam.claim;

public record ClaimDetailResponse(String claimNumber, String policyNumber, IncidentType incidentType, java.time.LocalDate incidentDate, String description, ClaimStatus status) {
}

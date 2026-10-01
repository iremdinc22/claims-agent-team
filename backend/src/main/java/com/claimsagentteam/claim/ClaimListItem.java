package com.claimsagentteam.claim;

public record ClaimListItem(String claimNumber, String policyNumber, IncidentType incidentType, java.time.LocalDate incidentDate, ClaimStatus status) {
}

package com.claimsagentteam.claim;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "claims")
public class ClaimEntity {

    @Id
    @Column(name = "claim_number", nullable = false, updatable = false)
    private UUID claimNumber;

    @Column(name = "policy_number", nullable = false, updatable = false)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, updatable = false)
    private IncidentType incidentType;

    @Column(name = "incident_date", nullable = false, updatable = false)
    private LocalDate incidentDate;

    @Column(name = "description", nullable = false, updatable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private ClaimStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }

    protected ClaimEntity() {
    }

    ClaimEntity(
            UUID claimNumber,
            String policyNumber,
            IncidentType incidentType,
            LocalDate incidentDate,
            String description,
            ClaimStatus status,
            String createdBy,
            Instant createdAt) {
        this.claimNumber = claimNumber;
        this.policyNumber = policyNumber;
        this.incidentType = incidentType;
        this.incidentDate = incidentDate;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getClaimNumber() {
        return claimNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public String getDescription() {
        return description;
    }

    public ClaimStatus getStatus() {
        return status;
    }
}

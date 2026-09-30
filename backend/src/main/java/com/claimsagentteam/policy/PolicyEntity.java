package com.claimsagentteam.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policies")
public class PolicyEntity {

    @Id
    @Column(name = "policy_number", nullable = false, updatable = false)
    private String policyNumber;

    protected PolicyEntity() {
    }

    public String getPolicyNumber() {
        return policyNumber;
    }
}

package com.claimsagentteam.claim;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {
}

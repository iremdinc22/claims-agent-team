package com.claimsagentteam.claim;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {
    long countByCreatedBy(String createdBy);

    Optional<ClaimEntity> findByClaimNumberAndCreatedBy(UUID claimNumber, String createdBy);

    @Query(value = """
            SELECT * FROM claims WHERE created_by = :owner
            ORDER BY incident_date DESC, created_at DESC, claim_number ASC
            LIMIT 10 OFFSET :offset
            """, nativeQuery = true)
    List<ClaimEntity> findOwnedPage(@Param("owner") String owner, @Param("offset") long offset);
}
